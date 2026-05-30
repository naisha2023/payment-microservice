#pragma once

#include <atomic>
#include <sstream>
#include <string>

using namespace std;

class GatewayMetrics {
private:
    atomic<long long> total_requests{0};
    atomic<long long> upstream_success{0};
    atomic<long long> upstream_errors{0};
    atomic<long long> rate_limited{0};
    atomic<long long> discovery_failures{0};
    atomic<long long> circuit_open{0};

public:
    void inc_total() { total_requests++; }
    void inc_success() { upstream_success++; }
    void inc_error() { upstream_errors++; }
    void inc_rate_limited() { rate_limited++; }
    void inc_discovery_failure() { discovery_failures++; }
    void inc_circuit_open() { circuit_open++; }

    string prometheus() const {
        stringstream ss;

        ss << "# HELP gateway_requests_total Total requests received by gateway\n";
        ss << "# TYPE gateway_requests_total counter\n";
        ss << "gateway_requests_total " << total_requests.load() << "\n";

        ss << "# HELP gateway_upstream_success_total Successful upstream responses\n";
        ss << "# TYPE gateway_upstream_success_total counter\n";
        ss << "gateway_upstream_success_total " << upstream_success.load() << "\n";

        ss << "# HELP gateway_upstream_errors_total Failed upstream requests\n";
        ss << "# TYPE gateway_upstream_errors_total counter\n";
        ss << "gateway_upstream_errors_total " << upstream_errors.load() << "\n";

        ss << "# HELP gateway_rate_limited_total Rate limited requests\n";
        ss << "# TYPE gateway_rate_limited_total counter\n";
        ss << "gateway_rate_limited_total " << rate_limited.load() << "\n";

        ss << "# HELP gateway_discovery_failures_total Service discovery failures\n";
        ss << "# TYPE gateway_discovery_failures_total counter\n";
        ss << "gateway_discovery_failures_total " << discovery_failures.load() << "\n";

        ss << "# HELP gateway_circuit_open_total Requests rejected by open circuit breaker\n";
        ss << "# TYPE gateway_circuit_open_total counter\n";
        ss << "gateway_circuit_open_total " << circuit_open.load() << "\n";

        return ss.str();
    }
};