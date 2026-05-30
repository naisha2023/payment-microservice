#pragma once

#include <drogon/HttpController.h>
#include <memory>

#include "metrics/GatewayMetrics.h"

using namespace drogon;
using namespace std;

class HealthController : public HttpController<HealthController> {
public:
    METHOD_LIST_BEGIN
        ADD_METHOD_TO(HealthController::health,  "/health",  Get);
        ADD_METHOD_TO(HealthController::ready,   "/ready",   Get);
        ADD_METHOD_TO(HealthController::metrics, "/metrics", Get);
    METHOD_LIST_END

    static void init(shared_ptr<GatewayMetrics> metrics);

    void health(const HttpRequestPtr& req,
                function<void(const HttpResponsePtr&)>&& callback);

    void ready(const HttpRequestPtr& req,
               function<void(const HttpResponsePtr&)>&& callback);

    void metrics(const HttpRequestPtr& req,
                 function<void(const HttpResponsePtr&)>&& callback);

private:
    static shared_ptr<GatewayMetrics> metrics_;
};