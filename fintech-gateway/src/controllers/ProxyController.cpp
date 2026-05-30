#include "ProxyController.h"

#include <json/json.h>
#include <algorithm>
#include <iostream>
#include <memory>
#include <random>
#include <sstream>

#include "../metrics/B3Propagator.h"
#include "logging/StructuredLogger.h"

using namespace drogon;
using namespace std;

shared_ptr<JWTMiddleware> ProxyController::jwt_;
shared_ptr<RedisPool> ProxyController::redis_;
GatewayConfig ProxyController::config_;
shared_ptr<ServiceRegistry> ProxyController::registry_;
shared_ptr<CircuitBreakerRegistry> ProxyController::cb_registry_;
shared_ptr<gateway::RateLimiter> ProxyController::rate_limiter_;
shared_ptr<GatewayMetrics> ProxyController::metrics_;

void ProxyController::init(
    shared_ptr<JWTMiddleware> jwt,
    shared_ptr<RedisPool> redis,
    const GatewayConfig& config,
    shared_ptr<ServiceRegistry> registry,
    shared_ptr<GatewayMetrics> metrics)
{
    jwt_ = jwt;
    redis_ = redis;
    config_ = config;
    registry_ = registry;
    metrics_ = metrics;

    rate_limiter_ = make_shared<gateway::RateLimiter>(redis_);
    cb_registry_ = make_shared<CircuitBreakerRegistry>();

    cb_registry_->configure("auth-service", {5, 2, 30, 50.0, 10});
    cb_registry_->configure("wallet-service", {3, 2, 60, 40.0, 10});
    cb_registry_->configure("payment-service", {3, 2, 60, 40.0, 10});
}

optional<ServiceRoute> ProxyController::find_route(const string& path)
{
    string stripped = path;

    if (stripped.rfind("/api", 0) == 0)
        stripped = stripped.substr(4);

    for (const auto& route : config_.routes) {
        if (stripped.rfind(route.prefix, 0) == 0)
            return route;
    }

    return nullopt;
}

void ProxyController::add_common_response_headers(
    const HttpResponsePtr& resp,
    const string& correlation_id)
{
    if (!correlation_id.empty()) {
        resp->addHeader("X-Correlation-ID", correlation_id);
        resp->addHeader("X-Request-ID", correlation_id);
    }

    resp->addHeader("Access-Control-Allow-Origin", "*");
    resp->addHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,PATCH,OPTIONS");
    resp->addHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,X-Service-ID,X-Correlation-ID,X-Request-ID");
    resp->addHeader("Access-Control-Expose-Headers", "X-RateLimit-Limit,X-RateLimit-Remaining,Retry-After,X-Correlation-ID,X-Request-ID");

    resp->addHeader("X-Content-Type-Options", "nosniff");
    resp->addHeader("X-Frame-Options", "DENY");
    resp->addHeader("Referrer-Policy", "no-referrer");
    resp->addHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
    resp->addHeader("Vary", "Origin");
}

HttpResponsePtr ProxyController::error_response(int status, const string& message)
{
    Json::Value body;
    body["success"] = false;
    body["message"] = message;

    auto resp = HttpResponse::newHttpJsonResponse(body);
    resp->setStatusCode(static_cast<HttpStatusCode>(status));
    add_common_response_headers(resp);

    return resp;
}

HttpResponsePtr ProxyController::circuit_open_response(const string& service)
{
    Json::Value body;
    body["success"] = false;
    body["message"] = "Service temporarily unavailable";
    body["service"] = service;

    auto resp = HttpResponse::newHttpJsonResponse(body);
    resp->setStatusCode(k503ServiceUnavailable);
    resp->addHeader("Retry-After", "30");
    add_common_response_headers(resp);

    return resp;
}

HttpResponsePtr ProxyController::rate_limit_response(const RateLimitResult& result)
{
    Json::Value body;
    body["success"] = false;
    body["message"] = "Rate limit exceeded";

    auto resp = HttpResponse::newHttpJsonResponse(body);
    resp->setStatusCode(k429TooManyRequests);
    resp->addHeader("Retry-After", to_string(result.retry_after));
    resp->addHeader("X-RateLimit-Limit", to_string(result.limit));
    resp->addHeader("X-RateLimit-Remaining", to_string(result.remaining));
    add_common_response_headers(resp);

    return resp;
}

string ProxyController::get_or_create_correlation_id(const HttpRequestPtr& req)
{
    string id = req->getHeader("X-Correlation-ID");

    if (!id.empty())
        return id;

    id = req->getHeader("X-Request-ID");

    if (!id.empty())
        return id;

    static random_device rd;
    static mt19937_64 gen(rd());
    static uniform_int_distribution<unsigned long long> dist;

    stringstream ss;
    ss << hex << dist(gen) << dist(gen);

    return ss.str();
}

HttpRequestPtr ProxyController::build_upstream_request(
    const HttpRequestPtr& req,
    const ServiceRoute& route,
    const AuthContext& auth,
    const string& correlation_id)
{
    auto upstream = HttpRequest::newHttpRequest();

    upstream->setMethod(req->getMethod());

    string fullPath = req->path();

    if (fullPath.rfind("/api", 0) == 0)
        fullPath = fullPath.substr(4);

    if (route.strip_prefix && fullPath.rfind(route.prefix, 0) == 0) {
        fullPath = fullPath.substr(route.prefix.size());

        if (fullPath.empty())
            fullPath = "/";
    }

    if (!req->query().empty())
        fullPath += "?" + req->query();

    upstream->setPath(fullPath);

    if (!req->getBody().empty()) {
        upstream->setBody(string(req->getBody()));

        string ct = req->getHeader("Content-Type");

        if (ct.empty())
            ct = req->getHeader("content-type");

        if (ct.empty())
            upstream->setContentTypeCode(CT_APPLICATION_JSON);
        else
            upstream->setContentTypeString(ct);
    }

    for (const auto& [key, value] : req->getHeaders()) {
        string lower = key;

        transform(lower.begin(), lower.end(), lower.begin(), ::tolower);

        if (lower == "host")
            continue;

        if (lower == "content-length")
            continue;

        if (lower == "content-type")
            continue;

        upstream->addHeader(key, value);
    }

    for (const auto& [key, value] : auth.headers_to_forward) {
        upstream->addHeader(key, value);
    }

    upstream->addHeader("X-Correlation-ID", correlation_id);
    upstream->addHeader("X-Request-ID", correlation_id);

    return upstream;
}

void ProxyController::options_handler(
    const HttpRequestPtr& req,
    function<void(const HttpResponsePtr&)>&& callback,
    string path)
{
    string correlation_id = get_or_create_correlation_id(req);
    auto resp = HttpResponse::newHttpResponse();
    resp->setStatusCode(k204NoContent);
    add_common_response_headers(resp, correlation_id);
    callback(resp);
}

void ProxyController::proxy(
    const HttpRequestPtr& req,
    function<void(const HttpResponsePtr&)>&& callback,
    string path)
{
    if (metrics_)
        metrics_->inc_total();

    string full_path = req->path();
    string client_ip = req->peerAddr().toIp();
    string correlation_id = get_or_create_correlation_id(req);

    if (req->getMethod() == Options) {
        auto resp = HttpResponse::newHttpResponse();
        resp->setStatusCode(k200OK);
        add_common_response_headers(resp, correlation_id);
        callback(resp);
        return;
    }

    Json::Value request_log;
    request_log["event"] = "proxy_request";
    request_log["correlation_id"] = correlation_id;
    request_log["method"] = req->getMethodString();
    request_log["path"] = full_path;
    request_log["client_ip"] = client_ip;
    StructuredLogger::info(request_log);

    {
        auto result = rate_limiter_->check(
            rate_limiter_->key_global_ip(client_ip),
            rate_limiter_->config().global_ip);

        if (!result.allowed) {
            if (metrics_)
                metrics_->inc_rate_limited();

            Json::Value log;
            log["event"] = "rate_limited";
            log["correlation_id"] = correlation_id;
            log["scope"] = "global_ip";
            log["client_ip"] = client_ip;
            StructuredLogger::warn(log);

            auto resp = rate_limit_response(result);
            add_common_response_headers(resp, correlation_id);
            callback(resp);
            return;
        }
    }

    bool is_login = full_path == "/auth/login" || full_path == "/api/auth/login";

    if (is_login) {
        auto result_ip = rate_limiter_->check(
            rate_limiter_->key_login_ip(client_ip),
            rate_limiter_->config().login_ip);

        if (!result_ip.allowed) {
            if (metrics_)
                metrics_->inc_rate_limited();

            Json::Value log;
            log["event"] = "rate_limited";
            log["correlation_id"] = correlation_id;
            log["scope"] = "login_ip";
            log["client_ip"] = client_ip;
            StructuredLogger::warn(log);

            auto resp = rate_limit_response(result_ip);
            add_common_response_headers(resp, correlation_id);
            callback(resp);
            return;
        }

        try {
            Json::Value body_json;
            Json::Reader reader;

            if (reader.parse(string(req->getBody()), body_json)) {
                string email = body_json.get("email", "").asString();

                if (!email.empty()) {
                    auto result_email = rate_limiter_->check(
                        rate_limiter_->key_login_email(email),
                        rate_limiter_->config().login_email);

                    if (!result_email.allowed) {
                        if (metrics_)
                            metrics_->inc_rate_limited();

                        Json::Value log;
                        log["event"] = "rate_limited";
                        log["correlation_id"] = correlation_id;
                        log["scope"] = "login_email";
                        StructuredLogger::warn(log);

                        auto resp = rate_limit_response(result_email);
                        add_common_response_headers(resp, correlation_id);
                        callback(resp);
                        return;
                    }
                }
            }
        } catch (...) {
        }
    }

    Json::Value debug_log;
debug_log["event"] = "debug_path";
debug_log["raw_path"] = full_path;
debug_log["routes_count"] = (int)config_.routes.size();
StructuredLogger::info(debug_log);

    auto route = find_route(full_path);

    if (!route.has_value()) {
        Json::Value log;
        log["event"] = "route_not_found";
        log["correlation_id"] = correlation_id;
        log["path"] = full_path;
        StructuredLogger::warn(log);

        auto resp = error_response(404, "Route not found");
        add_common_response_headers(resp, correlation_id);
        callback(resp);
        return;
    }

    AuthContext auth;

    string stripped_path = full_path;

    if (stripped_path.rfind("/api", 0) == 0)
        stripped_path = stripped_path.substr(4);

    bool is_public =
        find(
        config_.public_paths.begin(),
        config_.public_paths.end(),
        stripped_path
    ) != config_.public_paths.end();

    if (route->requires_auth && !is_public) {
        auth = jwt_->validateRequest(*req);

        if (!auth.is_authenticated) {
            Json::Value log;
            log["event"] = "auth_failed";
            log["correlation_id"] = correlation_id;
            log["path"] = full_path;
            StructuredLogger::warn(log);

            auto resp = error_response(401, auth.error_message);
            add_common_response_headers(resp, correlation_id);
            callback(resp);
            return;
        }
    }

    if (auth.is_authenticated) {
        const string& user_id = auth.payload.user_id;

        {
            auto result = rate_limiter_->check(
                rate_limiter_->key_global_user(user_id),
                rate_limiter_->config().global_user);

            if (!result.allowed) {
                if (metrics_)
                    metrics_->inc_rate_limited();

                auto resp = rate_limit_response(result);
                add_common_response_headers(resp, correlation_id);
                callback(resp);
                return;
            }
        }

        if (full_path.find("/payment") != string::npos && req->getMethod() == Post) {
            auto result = rate_limiter_->check(
                rate_limiter_->key_payment(user_id),
                rate_limiter_->config().payment_create);

            if (!result.allowed) {
                if (metrics_)
                    metrics_->inc_rate_limited();

                auto resp = rate_limit_response(result);
                add_common_response_headers(resp, correlation_id);
                callback(resp);
                return;
            }
        }

        if (full_path.find("/wallet") != string::npos) {
            auto result = rate_limiter_->check(
                rate_limiter_->key_wallet(user_id),
                rate_limiter_->config().wallet_operation);

            if (!result.allowed) {
                if (metrics_)
                    metrics_->inc_rate_limited();

                auto resp = rate_limit_response(result);
                add_common_response_headers(resp, correlation_id);
                callback(resp);
                return;
            }
        }

        string service_id = req->getHeader("X-Service-ID");

        if (!service_id.empty()) {
            auto result = rate_limiter_->check(
                rate_limiter_->key_internal(service_id),
                rate_limiter_->config().internal_service);

            if (!result.allowed) {
                if (metrics_)
                    metrics_->inc_rate_limited();

                auto resp = rate_limit_response(result);
                add_common_response_headers(resp, correlation_id);
                callback(resp);
                return;
            }
        }
    }

    ServiceInstance instance;

    try {
        if (!registry_) {
            if (metrics_)
                metrics_->inc_discovery_failure();

            Json::Value log;
            log["event"] = "service_registry_not_initialized";
            log["correlation_id"] = correlation_id;
            StructuredLogger::error(log);

            auto resp = error_response(500, "Service registry not initialized");
            add_common_response_headers(resp, correlation_id);
            callback(resp);
            return;
        }

        instance = registry_->resolve(route->service_name);

    } catch (const exception& ex) {
        if (metrics_)
            metrics_->inc_discovery_failure();

        Json::Value log;
        log["event"] = "discovery_failed";
        log["correlation_id"] = correlation_id;
        log["service"] = route->service_name;
        log["error"] = ex.what();
        StructuredLogger::error(log);

        auto resp = error_response(503, "No healthy instances available");
        add_common_response_headers(resp, correlation_id);
        callback(resp);
        return;
    }

    Json::Value upstream_log;
    upstream_log["event"] = "upstream_selected";
    upstream_log["correlation_id"] = correlation_id;
    upstream_log["upstream_service"] = route->service_name;
    upstream_log["upstream_host"] = instance.host;
    upstream_log["upstream_port"] = instance.port;
    upstream_log["use_ssl"] = instance.use_ssl;
    StructuredLogger::info(upstream_log);

    string url =
        string(instance.use_ssl ? "https://" : "http://") +
        instance.host + ":" +
        to_string(instance.port);

    auto client = HttpClient::newHttpClient(url);

    const double upstream_timeout_seconds =
        static_cast<double>(config_.request_timeout);

    if (instance.use_ssl)
    {
        const char* cert = getenv("GATEWAY_CLIENT_CERT_PATH");
        const char* key  = getenv("GATEWAY_CLIENT_KEY_PATH");

        if (!cert || !key || !cert[0] || !key[0]) {
            cert = getenv("GATEWAY_CERT_PATH");
            key  = getenv("GATEWAY_KEY_PATH");
        }

        const char* ca = getenv("GATEWAY_CA_PATH");

        // setCertPath(certFile, keyFile, caFile)
        client->setCertPath(
            (cert && cert[0]) ? cert : "",
            (key  && key[0])  ? key  : "",
            (ca   && ca[0])   ? ca   : ""
        );
    }

    B3Context incoming = B3Propagator::extract(*req);
    B3Context outgoing = B3Propagator::create_child(incoming);

    auto upstream = build_upstream_request(req, *route, auth, correlation_id);
    B3Propagator::inject(upstream, outgoing);

    Json::Value forwarding_log;
    forwarding_log["event"] = "proxy_forwarding";
    forwarding_log["correlation_id"] = correlation_id;
    forwarding_log["method"] = req->getMethodString();
    forwarding_log["path"] = upstream->path();
    forwarding_log["upstream_service"] = route->service_name;
    StructuredLogger::info(forwarding_log);

    auto cb = cb_registry_->get(route->service_name);

    if (!cb->allow_request()) {
        if (metrics_)
            metrics_->inc_circuit_open();

        Json::Value log;
        log["event"] = "circuit_open";
        log["correlation_id"] = correlation_id;
        log["service"] = route->service_name;
        StructuredLogger::warn(log);

        auto resp = circuit_open_response(route->service_name);
        add_common_response_headers(resp, correlation_id);
        callback(resp);
        return;
    }

    auto start = chrono::steady_clock::now();

    client->sendRequest(
        upstream,
        [callback = std::move(callback),
         cb,
         correlation_id,
         service_name = route->service_name,
         start](ReqResult result, const HttpResponsePtr& resp) mutable {
            auto end = chrono::steady_clock::now();

            auto latency_ms =
                chrono::duration_cast<chrono::milliseconds>(end - start).count();

            if (result != ReqResult::Ok || !resp) {
                cb->record_failure();

                if (metrics_)
                    metrics_->inc_error();

                Json::Value log;
                log["event"] = "proxy_error";
                log["correlation_id"] = correlation_id;
                log["upstream_service"] = service_name;
                log["result"] = static_cast<int>(result);
                log["latency_ms"] = static_cast<Json::Int64>(latency_ms);
                StructuredLogger::error(log);

                auto error = error_response(502, "Bad Gateway");
                add_common_response_headers(error, correlation_id);
                callback(error);
                return;
            }

            int status = static_cast<int>(resp->getStatusCode());

            if (status >= 500) {
                cb->record_failure();

                if (metrics_)
                    metrics_->inc_error();
            } else {
                cb->record_success();

                if (metrics_)
                    metrics_->inc_success();
            }

            Json::Value log;
            log["event"] = "proxy_response";
            log["correlation_id"] = correlation_id;
            log["upstream_service"] = service_name;
            log["status"] = status;
            log["latency_ms"] = static_cast<Json::Int64>(latency_ms);

            if (status >= 500)
                StructuredLogger::error(log);
            else
                StructuredLogger::info(log);

            add_common_response_headers(resp, correlation_id);
            callback(resp);
        },
        upstream_timeout_seconds);
}