#pragma once
#include "redis/RedisPool.h"
#include "rate/RateLimitConfig.h"
#include <memory>
#include <string>

// Avoid clash with drogon::RateLimiter (pulled in via HttpController.h).
namespace gateway {

class RateLimiter {
public:
    explicit RateLimiter(std::shared_ptr<RedisPool> redis,
                         const RateLimitConfig& config = {});

    RateLimitResult check(const std::string& key, const RateLimitRule& rule);

    std::string key_global_ip(const std::string& ip);
    std::string key_global_user(const std::string& user_id);
    std::string key_login_ip(const std::string& ip);
    std::string key_login_email(const std::string& email);
    std::string key_payment(const std::string& user_id);
    std::string key_wallet(const std::string& user_id);
    std::string key_internal(const std::string& service_id);

    const RateLimitConfig& config() const { return config_; }

private:
    std::shared_ptr<RedisPool> redis_;
    RateLimitConfig            config_;
};

}  // namespace gateway