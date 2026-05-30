#pragma once

#include "ConsulClient.h"
#include "config/RouteConfig.h"

#include <unordered_map>
#include <vector>
#include <mutex>
#include <atomic>
#include <thread>
#include <chrono>
#include <iostream>

using namespace std;

class ServiceRegistry {
private:
    ConsulClient consul_client;

    unordered_map<string, vector<ServiceInstance>> cache;
    unordered_map<string, size_t> counters;

    mutex cache_mutex;

public:
    explicit ServiceRegistry(ConsulClient client)
        : consul_client(std::move(client)) {}

    void refreshService(const string& service_name, bool use_ssl = true) {
        auto instances = consul_client.getHealthyInstances(service_name, use_ssl);

        lock_guard<mutex> lock(cache_mutex);

        cache[service_name] = instances;

        if (counters.find(service_name) == counters.end()) {
            counters[service_name] = 0;
        }

        cout << "[Discovery] refreshed "
             << service_name
             << " instances="
             << instances.size()
             << endl;
    }

    void refreshAll(const vector<ServiceRoute>& routes) {
        for (const auto& route : routes) {
            try {
                refreshService(route.service_name, route.use_ssl);
            } catch (const exception& ex) {
                cerr << "[Discovery] failed to refresh "
                     << route.service_name
                     << ": "
                     << ex.what()
                     << endl;
            }
        }
    }

    ServiceInstance resolve(const string& service_name) {
        lock_guard<mutex> lock(cache_mutex);

        auto it = cache.find(service_name);

        if (it == cache.end() || it->second.empty()) {
            throw runtime_error(
                "No healthy instances available for service: " + service_name
            );
        }

        auto& instances = it->second;

        size_t index = counters[service_name] % instances.size();
        counters[service_name]++;

        return instances[index];
    }

    void startAutoRefresh(
        const vector<ServiceRoute>& routes,
        int interval_seconds = 5
    ) {
        thread([this, routes, interval_seconds]() {
            while (true) {
                refreshAll(routes);
                this_thread::sleep_for(chrono::seconds(interval_seconds));
            }
        }).detach();
    }
};