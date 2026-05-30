#include <drogon/drogon.h>

#include "controllers/ProxyController.h"
#include "controllers/HealthController.h"

#include "middleware/JWTMiddleware.h"

#include "redis/RedisPool.h"

#include "config/RouteConfig.h"

#include "routes/ConsulClient.h"
#include "routes/ServiceRegistry.h"

#include "metrics/GatewayMetrics.h"

#include "logging/StructuredLogger.h"

using namespace drogon;
using namespace std;

int main()
{
    const char* gateway_cert = getenv("GATEWAY_SERVER_CERT_PATH");
    const char* gateway_key  = getenv("GATEWAY_SERVER_KEY_PATH");
    const char* client_ca    = getenv("GATEWAY_CLIENT_CA_PATH");
    try
    {
        // ─────────────────────────────────────────────────────
        // CONFIG
        // ─────────────────────────────────────────────────────

        RedisConfig redis_config;

        redis_config.host =
            getenv("REDIS_HOST")
                ? getenv("REDIS_HOST")
                : "redis";

        redis_config.port = getenv("REDIS_PORT") ? stoi(getenv("REDIS_PORT")): 6379;

        redis_config.password =
            getenv("REDIS_PASSWORD")
                ? getenv("REDIS_PASSWORD")
                : "";

        redis_config.pool_size = 10;

        const char* jwt_secret = getenv("JWT_SECRET");

        if (!jwt_secret || string(jwt_secret).empty())
        {
            Json::Value log;
            log["event"] = "startup_failed";
            log["reason"] = "JWT_SECRET missing";

            StructuredLogger::error(log);

            return 1;
        }

        JWTConfig jwt_config;

        jwt_config.secret_key = jwt_secret;
        jwt_config.expiration_minutes = 60;
        jwt_config.enable_blacklist = true;
        jwt_config.enable_caching = true;

        GatewayConfig gateway_config;

        // ─────────────────────────────────────────────────────
        // METRICS
        // ─────────────────────────────────────────────────────

        auto metrics = make_shared<GatewayMetrics>();

        // ─────────────────────────────────────────────────────
        // CONSUL
        // ─────────────────────────────────────────────────────

        ConsulClient consul(
            gateway_config.consul_host,
            gateway_config.consul_port
        );

        auto registry =
            make_shared<ServiceRegistry>(consul);

        registry->startAutoRefresh(
            gateway_config.routes,
            5
        );

        // ─────────────────────────────────────────────────────
        // REDIS
        // ─────────────────────────────────────────────────────

        auto redis =
            make_shared<RedisPool>(redis_config);

        if (!redis->ping())
        {
            Json::Value log;
            log["event"] = "redis_connection_failed";
            log["host"] = redis_config.host;
            log["port"] = redis_config.port;

            StructuredLogger::error(log);

            return 1;
        }

        Json::Value redis_log;
        redis_log["event"] = "redis_connected";
        redis_log["host"] = redis_config.host;
        redis_log["port"] = redis_config.port;

        StructuredLogger::info(redis_log);

        // ─────────────────────────────────────────────────────
        // JWT
        // ─────────────────────────────────────────────────────

        auto jwt =
            make_shared<JWTMiddleware>(
                jwt_config,
                redis
            );

        // ─────────────────────────────────────────────────────
        // CONTROLLERS
        // ─────────────────────────────────────────────────────

        HealthController::init(metrics);

        ProxyController::init(
            jwt,
            redis,
            gateway_config,
            registry,
            metrics
        );

        // ─────────────────────────────────────────────────────
        // STARTUP LOG
        // ─────────────────────────────────────────────────────

        Json::Value startup_log;

        startup_log["event"] = "gateway_started";
        startup_log["host"] = gateway_config.gateway_host;
        startup_log["port"] = gateway_config.gateway_port;
        startup_log["threads"] = 4;
        startup_log["consul_host"] = gateway_config.consul_host;
        startup_log["consul_port"] = gateway_config.consul_port;

        StructuredLogger::info(startup_log);

        // ─────────────────────────────────────────────────────
        // DROGON
        // ─────────────────────────────────────────────────────        

        if (!gateway_cert || !gateway_key) {
            Json::Value log;
            log["event"] = "startup_failed";
            log["reason"] = "gateway TLS cert/key missing";
            StructuredLogger::error(log);
            return 1;
        }


        app().registerSyncAdvice([](const HttpRequestPtr& req) -> HttpResponsePtr {
            if (req->getMethod() == Options) {
                auto resp = HttpResponse::newHttpResponse();
                resp->setStatusCode(k204NoContent);
                string correlation_id = ProxyController::get_or_create_correlation_id(req);
                ProxyController::add_common_response_headers(resp, correlation_id);
                resp->addHeader("Access-Control-Max-Age", "86400");
                return resp;  // short-circuit, nunca llega al router
            }
            return nullptr;  // continúa normalmente
        });
        app()
        .addListener(
            gateway_config.gateway_host,
            gateway_config.gateway_port,
            true,
            gateway_cert,
            gateway_key
        )
        .setThreadNum(4)
        .run();
    }
    catch (const exception& ex)
    {
        Json::Value log;

        log["event"] = "fatal_exception";
        log["error"] = ex.what();

        StructuredLogger::error(log);

        return 1;
    }
    catch (...)
    {
        Json::Value log;

        log["event"] = "fatal_unknown_exception";

        StructuredLogger::error(log);

        return 1;
    }

    return 0;
}