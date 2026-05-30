#pragma once

#include <drogon/HttpController.h>
#include <drogon/HttpClient.h>

#include "middleware/JWTMiddleware.h"
#include "config/RouteConfig.h"
#include "redis/RedisPool.h"
#include "rate/RateLimiter.h"
#include <optional>
#include <memory>
#include "CircuitBreaker/CircuitBreakerRegistry.h"
#include "routes/ServiceRegistry.h"
#include "metrics/GatewayMetrics.h"

using namespace drogon;
using namespace std;

class ProxyController : public HttpController<ProxyController>
{
public:
    METHOD_LIST_BEGIN

    ADD_METHOD_TO(
        ProxyController::proxy,
        "/api/auth/{1:.*}",
        Get,
        Post,
        Put,
        Delete,
        Patch
    );
    ADD_METHOD_TO(
        ProxyController::proxy,
        "/api/wallets/{1:.*}",
        Get,
        Post,
        Put,
        Delete,
        Patch
    );
    ADD_METHOD_TO(
        ProxyController::proxy,
        "/api/wallets/{1}/credit",
        Post
    );

    METHOD_LIST_END

    static void init(
        shared_ptr<JWTMiddleware> jwt,
        shared_ptr<RedisPool> redis,
        const GatewayConfig& config,
        shared_ptr<ServiceRegistry> registry,
        shared_ptr<GatewayMetrics> metrics
    );

    static HttpResponsePtr rate_limit_response(const RateLimitResult& result);
    static void add_common_response_headers(const HttpResponsePtr& resp, const string& correlation_id = "");
    static string get_or_create_correlation_id(const HttpRequestPtr& req);
    static shared_ptr<GatewayMetrics> metrics_;

    void proxy(
        const HttpRequestPtr& req,
        function<void(const HttpResponsePtr&)>&& callback,
        string path
    );

    void options_handler(
        const HttpRequestPtr& req,
        function<void(const HttpResponsePtr&)>&& callback,
        string path
    );

private:
    static shared_ptr<gateway::RateLimiter> rate_limiter_;
    static shared_ptr<JWTMiddleware> jwt_;
    static shared_ptr<RedisPool> redis_;
    static GatewayConfig config_;
    static shared_ptr<CircuitBreakerRegistry> cb_registry_;
    static HttpResponsePtr circuit_open_response(const std::string& service);
    static shared_ptr<ServiceRegistry> registry_;

    static optional<ServiceRoute> find_route(
        const string& path
    );

    static HttpResponsePtr error_response(
        int status,
        const string& message
    );

    static HttpRequestPtr build_upstream_request(
        const HttpRequestPtr& req,
        const ServiceRoute& route,
        const AuthContext& auth,
        const string& correlation_id
    );
};