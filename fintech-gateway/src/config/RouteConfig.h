#pragma once

#include <string>
#include <vector>

using namespace std;

struct ServiceRoute {
    string prefix;
    string service_name;

    bool requires_auth = true;
    bool strip_prefix  = false;
    bool use_ssl       = true;
};

struct GatewayConfig {
    vector<ServiceRoute> routes = {
        { "/auth",          "auth-service",          true, false, true },
        { "/wallets",       "wallet-service",        true,  false, true },
        { "/payments",      "payment-service",       true,  false, true },
        { "/notifications", "notification-service",  false,  false, true },
        { "/loans",         "loans-service",         true,  false, true },
        { "/realtime",      "realtime-service",      true,  false, true }
    };

    vector<string> public_paths = {
        "/auth/login",
        "/auth/register",
        "/auth/refresh",
        "/health",
        "/metrics"
    };

    string consul_host = "consul";
    int consul_port = 8500;

    int gateway_port = 8080;
    string gateway_host = "0.0.0.0";
    int request_timeout = 30;
};