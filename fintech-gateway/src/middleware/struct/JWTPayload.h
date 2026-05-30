#pragma once
#include <string>
#include <vector>
#include <chrono>
#include <unordered_map>

using namespace std;
using namespace std::chrono;

struct JWTPayload {
    string user_id;
    string user_name;
    string user_email;
    string jti;
    vector<string> permissions;
    vector<string> roles;
    string session_id;
    string device_id;
    string issuer;
    string audience;
    time_point<system_clock> issued_at;
    time_point<system_clock> expires_at;
    time_point<system_clock> not_before;
    unordered_map<string, string> custom_claims;
};