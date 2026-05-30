#pragma once
#include "JWTPayload.h"
#include <string>
#include <chrono>
#include <unordered_map>

using namespace std;
using namespace std::chrono;

struct AuthContext {
    JWTPayload payload;
    bool is_authenticated   = false;
    bool requires_2fa       = false;
    bool should_refresh     = false;
    bool needs_refresh      = false;
    string error_message;
    string new_token;
    nanoseconds validation_time_ns{0};
    unordered_map<string, string> headers_to_forward;
};