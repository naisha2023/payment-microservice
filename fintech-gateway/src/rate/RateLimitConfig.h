#pragma once

struct RateLimitRule{
    int max_requests;
    int window_seconds;
    int retry_after;
};

struct RateLimitConfig{
    RateLimitRule global_ip         = {300, 60, 60};
    RateLimitRule global_user       = {100, 60, 60};
    RateLimitRule login_ip          = {10, 60, 60};
    RateLimitRule login_email       = {5, 60, 60};
    RateLimitRule payment_create    = {20, 60, 60};
    RateLimitRule wallet_operation  ={30, 60, 60};
    RateLimitRule internal_service  = {1000, 60, 60};
};

struct RateLimitResult {
    bool  allowed;
    int   remaining;
    int   retry_after;    
    int   limit;
};