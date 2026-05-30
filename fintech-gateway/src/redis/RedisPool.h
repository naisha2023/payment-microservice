#pragma once
#include <string>
#include <memory>
#include <chrono>
#include <sw/redis++/redis++.h>
#include "structs/RedisConfig.h"

using namespace std;
using namespace sw::redis;


class RedisPool {
public:
    explicit RedisPool(const RedisConfig& config);

    // Ejecutar comandos
    optional<string> get(const string& key);
    bool set(const string& key, const string& value, chrono::seconds ttl = chrono::seconds(0));
    bool del(const string& key);
    bool exists(const string& key);
    long long incr(const string& key);
    bool expire(const string& key, chrono::seconds ttl);
    long long ttl(const string& key);

    // Ping para verificar conexión
    bool ping();

private:
    unique_ptr<Redis> redis_;
    RedisConfig config_;
};