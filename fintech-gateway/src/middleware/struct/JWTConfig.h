#pragma once
#include <string>
#include <vector>

struct JWTConfig {
    std::string secret_key;
    int expiration_minutes;
    int refresh_expiration_days;
    std::vector<std::string> allowed_issuers;
    std::vector<std::string> allowed_audiences;
    bool enable_blacklist         = false;
    bool enable_caching           = false;
    bool enable_session_validation = false;
    bool enable_device_validation  = false;
    bool enable_user_rate_limit    = false;
    int max_token_size_bytes      = 8192;
};