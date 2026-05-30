#include "CorsFilter.h"
#include "controllers/ProxyController.h"

void CorsFilter::doFilter(const HttpRequestPtr& req,
                           FilterCallback&& fcb,
                           FilterChainCallback&& fccb)
{
    if (req->getMethod() == Options)
    {
        auto resp = HttpResponse::newHttpResponse();
        resp->setStatusCode(k204NoContent);
        string correlation_id = ProxyController::get_or_create_correlation_id(req);
        ProxyController::add_common_response_headers(resp, correlation_id);
        resp->addHeader("Access-Control-Max-Age", "86400");
        fcb(resp);
        return;
    }

    fccb();
}