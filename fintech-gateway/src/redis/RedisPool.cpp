#include "RedisPool.h"
#include <iostream>

using namespace std;
using namespace sw::redis;

RedisPool::RedisPool(const RedisConfig& config) : config_(config) {
    ConnectionOptions opts;
    opts.host    = config.host;
    opts.port    = config.port;
    opts.db      = config.db;
    opts.connect_timeout = config.connect_timeout;
    opts.socket_timeout  = config.socket_timeout;

    if (!config.password.empty())
        opts.password = config.password;

    ConnectionPoolOptions pool_opts;
    pool_opts.size = config.pool_size;

    redis_ = make_unique<Redis>(opts, pool_opts);
}

optional<string> RedisPool::get(const string& key) {
    try {
        return redis_->get(key);
    } catch (const Error& e) {
        cerr << "[Redis] GET error: " << e.what() << "\n";
        return nullopt;
    }
}

bool RedisPool::set(const string& key, const string& value, chrono::seconds ttl) {
    try {
        if (ttl.count() > 0)
            redis_->setex(key, ttl, value);
        else
            redis_->set(key, value);
        return true;
    } catch (const Error& e) {
        cerr << "[Redis] SET error: " << e.what() << "\n";
        return false;
    }
}

bool RedisPool::del(const string& key) {
    try {
        return redis_->del(key) > 0;
    } catch (const Error& e) {
        cerr << "[Redis] DEL error: " << e.what() << "\n";
        return false;
    }
}

bool RedisPool::exists(const string& key) {
    try {
        return redis_->exists(key) > 0;
    } catch (const Error& e) {
        cerr << "[Redis] EXISTS error: " << e.what() << "\n";
        return false;
    }
}

long long RedisPool::incr(const string& key) {
    try {
        return redis_->incr(key);
    } catch (const Error& e) {
        cerr << "[Redis] INCR error: " << e.what() << "\n";
        return -1;
    }
}

bool RedisPool::expire(const string& key, chrono::seconds ttl) {
    try {
        return redis_->expire(key, ttl);
    } catch (const Error& e) {
        cerr << "[Redis] EXPIRE error: " << e.what() << "\n";
        return false;
    }
}

long long RedisPool::ttl(const string& key) {
    try {
        return redis_->ttl(key);
    } catch (const Error& e) {
        cerr << "[Redis] TTL error: " << e.what() << "\n";
        return -2;
    }
}

bool RedisPool::ping() {
    try {
        redis_->ping();
        return true;
    } catch (const Error& e) {
        cerr << "[Redis] PING error: " << e.what() << "\n";
        return false;
    }
}