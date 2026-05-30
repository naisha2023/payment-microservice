#pragma once
#include "struct/JWTConfig.h"
#include "struct/AuthContext.h"
#include "struct/HTTPRequest.h"
#include "../redis/RedisPool.h"

#include <string>
#include <vector>
#include <optional>
#include <unordered_map>
#include <chrono>
#include <mutex>
#include <memory>

using namespace std;
using namespace std::chrono;

class JWTMiddleware {
public:
    explicit JWTMiddleware(const JWTConfig& config, shared_ptr<RedisPool> redis)
        : config_(config), redis_(redis) {}

    AuthContext validateRequest(const HTTPRequest& request);

private:
    JWTConfig config_;
    shared_ptr<RedisPool> redis_;
    unordered_map<string, system_clock::time_point> tokenBlacklist_;
    mutex blacklistMutex_;

    string extract_token_from_header(const HTTPRequest& request);
    bool is_token_blacklisted(const string& token);
    optional<JWTPayload> validate_jwt_token(const string& token);
    bool is_issuer_allowed(const string& issuer);
    bool is_audience_allowed(const string& audience);
    nanoseconds get_elapsed_time(const time_point<high_resolution_clock>& start);
    string generate_refresh_token(const JWTPayload& payload);
    void log_validation_attempt(const string& user_id, const string& session_id,
                                const string& client_ip, bool success, nanoseconds elapsed);
    int get_user_rate_limit(const string& user_id);
    string join_strings(const vector<string>& vec, const string& delimiter);
};