#pragma once

#include <drogon/drogon.h>
#include <nlohmann/json.hpp>

#include <string>
#include <vector>
#include <stdexcept>

using namespace std;
using json = nlohmann::json;

struct ServiceInstance {
    string host;
    int port;
    bool use_ssl = true;
};

class ConsulClient {
private:
    string consul_base_url;

public:
    ConsulClient(string host = "consul", int port = 8500) {
        consul_base_url = "http://" + host + ":" + to_string(port);
    }

    vector<ServiceInstance> getHealthyInstances(
        const string& service_name,
        bool use_ssl = true
    ) {
        auto client = drogon::HttpClient::newHttpClient(consul_base_url);

        string path = "/v1/health/service/" + service_name + "?passing=true";

        auto request = drogon::HttpRequest::newHttpRequest();
        request->setMethod(drogon::Get);
        request->setPath(path);

        auto pair = client->sendRequest(request);
        auto result = pair.first;
        auto response = pair.second;

        if (result != drogon::ReqResult::Ok || !response) {
            throw runtime_error("Consul not reachable for service: " + service_name);
        }

        if (response->statusCode() != drogon::k200OK) {
            throw runtime_error(
                "Consul returned HTTP " +
                to_string(response->statusCode()) +
                " for service: " +
                service_name
            );
        }

        json body = json::parse(string(response->body()));

        vector<ServiceInstance> instances;

        for (const auto& item : body) {
            const auto& service = item["Service"];

            int port = service.value("Port", 0);

            // En Docker Compose, usa el DNS estable del servicio.
            // Evita usar Service.Address si Consul devuelve container-id,
            // porque puede romper TLS/SNI.
            string address = service_name;

            if (port > 0) {
                instances.push_back({
                    address,
                    port,
                    use_ssl
                });
            }
        }

        return instances;
    }
};