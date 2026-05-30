#pragma once

#include <drogon/HttpRequest.h>
#include <string>
#include <unordered_map>

using namespace std;

struct HTTPRequest {
    string method;
    string path;
    unordered_map<string, string> headers;
    string body;
    string client_ip;
    string user_agent;
};

// Enable `HTTPRequest r = *req;` and passing `*req` to functions expecting
// `HTTPRequest` (e.g. JWT middleware).
namespace drogon
{
template <>
inline ::HTTPRequest fromRequest(const HttpRequest &req)
{
    ::HTTPRequest r;
    r.method = req.getMethodString();
    r.path = req.getPath();
    for (const auto &kv : req.getHeaders())
    {
        r.headers.emplace(kv.first, kv.second);
    }
    r.body = std::string(req.getBody());
    r.client_ip = req.getPeerAddr().toIp();
    r.user_agent = req.getHeader("User-Agent");
    if (r.user_agent.empty())
        r.user_agent = req.getHeader("user-agent");
    return r;
}
}  // namespace drogon