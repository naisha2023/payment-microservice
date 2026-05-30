#include "RateLimiter.h"
#include <chrono>
#include <iostream>

using namespace gateway;

RateLimiter::RateLimiter(std::shared_ptr<RedisPool> redis, const RateLimitConfig& config)
    : redis_(redis), config_(config)
{
}

RateLimitResult RateLimiter::check(const std::string& key, const RateLimitRule& rule)
{
    long long count = redis_->incr(key);
    if (count == 1)
        redis_->expire(key, chrono::seconds(rule.window_seconds));
    if (count > rule.max_requests){
        long long remaining_ttl = redis_->ttl(key);
        int retry = remaining_ttl > 0 ? static_cast<int>(remaining_ttl) : rule.retry_after;
        return {false, 0, retry, rule.max_requests};
    }
    int remaining = rule.max_requests - static_cast<int>(count);
    return {true, remaining, 0, rule.max_requests};
}

std::string RateLimiter::key_global_ip(const std::string& ip) {
    return "rl:global:ip:" + ip;
}
std::string RateLimiter::key_global_user(const std::string& user_id) {
    return "rl:global:user:" + user_id;
}
std::string RateLimiter::key_login_ip(const std::string& ip) {
    return "rl:login:ip:" + ip;
}
std::string RateLimiter::key_login_email(const std::string& email) {
    return "rl:login:email:" + email;
}
std::string RateLimiter::key_payment(const std::string& user_id) {
    return "rl:payment:user:" + user_id;
}
std::string RateLimiter::key_wallet(const std::string& user_id) {
    return "rl:wallet:user:" + user_id;
}
std::string RateLimiter::key_internal(const std::string& service_id) {
    return "rl:internal:service:" + service_id;
}