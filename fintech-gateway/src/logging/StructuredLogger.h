#pragma once

#include <json/json.h>
#include <chrono>
#include <iostream>
#include <string>

using namespace std;

class StructuredLogger {
public:
    static void log(Json::Value event)
    {
        event["timestamp"] = now_iso8601();
        event["service"] = "api-gateway";

        Json::StreamWriterBuilder builder;
        builder["indentation"] = "";

        cerr << Json::writeString(builder, event) << endl;
    }

    static void info(Json::Value event)
    {
        event["level"] = "INFO";
        log(event);
    }

    static void warn(Json::Value event)
    {
        event["level"] = "WARN";
        log(event);
    }

    static void error(Json::Value event)
    {
        event["level"] = "ERROR";
        log(event);
    }

private:
    static string now_iso8601()
    {
        using namespace chrono;

        auto now = system_clock::now();
        time_t t = system_clock::to_time_t(now);

        char buffer[32];
        strftime(buffer, sizeof(buffer), "%Y-%m-%dT%H:%M:%SZ", gmtime(&t));

        return string(buffer);
    }
};