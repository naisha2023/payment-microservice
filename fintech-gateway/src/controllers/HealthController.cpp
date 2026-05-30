#include "HealthController.h"
#include <drogon/HttpResponse.h>
#include <json/json.h>

using namespace drogon;
using namespace std::chrono;
using namespace std;

shared_ptr<GatewayMetrics> HealthController::metrics_;

void HealthController::init(shared_ptr<GatewayMetrics> metrics)
{
    metrics_ = metrics;
}

void HealthController::metrics(
    const HttpRequestPtr& req,
    function<void(const HttpResponsePtr&)>&& callback
) {
    auto resp = HttpResponse::newHttpResponse();
    resp->setStatusCode(k200OK);
    resp->setContentTypeCode(CT_TEXT_PLAIN);
    resp->setBody(metrics_ ? metrics_->prometheus() : "");
    callback(resp);
}

void HealthController::health(
    const HttpRequestPtr& req,
    function<void(const HttpResponsePtr&)>&& callback)
{
    Json::Value body;
    body["status"]  = "UP";
    body["service"] = "fintech-gateway";

    auto resp = HttpResponse::newHttpJsonResponse(body);
    resp->setStatusCode(k200OK);
    callback(resp);
}

void HealthController::ready(
    const HttpRequestPtr& req,
    function<void(const HttpResponsePtr&)>&& callback)
{
    Json::Value body;
    body["status"] = "READY";

    auto resp = HttpResponse::newHttpJsonResponse(body);
    resp->setStatusCode(k200OK);
    callback(resp);
}