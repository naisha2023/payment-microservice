#include "struct/JWTConfig.h"
#include "struct/AuthContext.h"
#include "struct/HTTPRequest.h"
#include "JWTMiddleware.h"

#include <jwt-cpp/jwt.h>
#include <nlohmann/json.hpp>

#include <iostream>
#include <sstream>
#include <algorithm>

using namespace std;
using namespace std::chrono;

// ─── Token extraction ────────────────────────────────────────────────────────

string JWTMiddleware::extract_token_from_header(const HTTPRequest& request) {
    // Headers in Drogon are case-insensitive and may be stored normalized
    // (often lowercase). Accept both.
    auto it = request.headers.find("Authorization");
    if (it == request.headers.end())
        it = request.headers.find("authorization");
    if (it == request.headers.end()) return "";

    const string& auth = it->second;
    if (auth.substr(0, 7) != "Bearer ") return "";

    return auth.substr(7);
}

// ─── Blacklist ────────────────────────────────────────────────────────────────

bool JWTMiddleware::is_token_blacklisted(const string& token) {
    {
        lock_guard<mutex> lock(blacklistMutex_);
        auto it = tokenBlacklist_.find(token);
        if (it != tokenBlacklist_.end()) {
            if (system_clock::now() > it->second) {
                tokenBlacklist_.erase(it);
            } else {
                return true;
            }
        }
    }

    if (config_.enable_blacklist && redis_) {
        string key = "blacklist:token:" + token;
        return redis_->exists(key);
    }

    return false;
}

// ─── Issuer / Audience ───────────────────────────────────────────────────────

bool JWTMiddleware::is_issuer_allowed(const string& issuer) {
    const auto& allowed = config_.allowed_issuers;
    return allowed.empty() ||
           find(allowed.begin(), allowed.end(), issuer) != allowed.end();
}

bool JWTMiddleware::is_audience_allowed(const string& audience) {
    const auto& allowed = config_.allowed_audiences;
    return allowed.empty() ||
           find(allowed.begin(), allowed.end(), audience) != allowed.end();
}

// ─── Time helpers ─────────────────────────────────────────────────────────────

nanoseconds JWTMiddleware::get_elapsed_time(
    const time_point<high_resolution_clock>& start)
{
    return duration_cast<nanoseconds>(high_resolution_clock::now() - start);
}

// ─── Utils ────────────────────────────────────────────────────────────────────

string JWTMiddleware::join_strings(const vector<string>& vec, const string& delimiter) {
    ostringstream oss;
    for (size_t i = 0; i < vec.size(); ++i) {
        if (i > 0) oss << delimiter;
        oss << vec[i];
    }
    return oss.str();
}

// ─── Rate limit ───────────────────────────────────────────────────────────────

int JWTMiddleware::get_user_rate_limit(const string& user_id) {
    return 100;
}

// ─── Logging ──────────────────────────────────────────────────────────────────

void JWTMiddleware::log_validation_attempt(
    const string& user_id,
    const string& session_id,
    const string& client_ip,
    bool success,
    nanoseconds elapsed)
{
    cout << "[JWT] user=" << user_id
         << " session=" << session_id
         << " ip=" << client_ip
         << " success=" << success
         << " elapsed=" << elapsed.count() << "ns\n";
}

// ─── Token generation ─────────────────────────────────────────────────────────

string JWTMiddleware::generate_refresh_token(const JWTPayload& payload) {
    return "refresh_stub_" + payload.user_id;
}

// ─── JWT validation ───────────────────────────────────────────────────────────

optional<JWTPayload> JWTMiddleware::validate_jwt_token(const string& token) {
    try {
        auto verifier = jwt::verify()
            .allow_algorithm(jwt::algorithm::hs256{config_.secret_key})
            .with_type("JWT");

        auto decoded = jwt::decode(token);
        verifier.verify(decoded);

        JWTPayload payload;

        if (decoded.has_subject())
            payload.user_id = decoded.get_subject();

        if (decoded.has_issuer())
            payload.issuer = decoded.get_issuer();

        if (decoded.has_audience()) {
            auto aud = decoded.get_audience();
            if (!aud.empty())
                payload.audience = *aud.begin();
        }

        if (decoded.has_expires_at())
            payload.expires_at = decoded.get_expires_at();

        if (decoded.has_issued_at())
            payload.issued_at = decoded.get_issued_at();

        if (decoded.has_not_before())
            payload.not_before = decoded.get_not_before();

        if (decoded.has_id())
            payload.jti = decoded.get_id();

        if (decoded.has_payload_claim("email"))
            payload.user_email = decoded.get_payload_claim("email").as_string();

        if (decoded.has_payload_claim("name"))
            payload.user_name = decoded.get_payload_claim("name").as_string();

        if (decoded.has_payload_claim("session_id"))
            payload.session_id = decoded.get_payload_claim("session_id").as_string();

        if (decoded.has_payload_claim("device_id"))
            payload.device_id = decoded.get_payload_claim("device_id").as_string();

        if (decoded.has_payload_claim("roles")) {
            auto roles = decoded.get_payload_claim("roles").as_array();
            for (const auto& r : roles)
                payload.roles.push_back(r.get<string>());
        }

        if (decoded.has_payload_claim("permissions")) {
            auto perms = decoded.get_payload_claim("permissions").as_array();
            for (const auto& p : perms)
                payload.permissions.push_back(p.get<string>());
        }

        return payload;

    } catch (const jwt::error::token_verification_exception& e) {
        cerr << "[JWT] Verification failed: " << e.what() << "\n";
        return nullopt;
    } catch (const exception& e) {
        cerr << "[JWT] Decode error: " << e.what() << "\n";
        return nullopt;
    }
}

// ─── Main validation ─────────────────────────────────────────────────────────

AuthContext JWTMiddleware::validateRequest(const HTTPRequest& request) {
    auto start_time = high_resolution_clock::now();
    AuthContext result;

    string token = extract_token_from_header(request);
    if (token.empty()) {
        result.error_message = "Missing Authorization header";
        result.validation_time_ns = get_elapsed_time(start_time);
        return result;
    }

    if ((int)token.length() > config_.max_token_size_bytes) {
        result.error_message = "Token exceeds maximum allowed size";
        result.validation_time_ns = get_elapsed_time(start_time);
        return result;
    }

    if (is_token_blacklisted(token)) {
        result.error_message = "Token is blacklisted";
        result.validation_time_ns = get_elapsed_time(start_time);
        return result;
    }

    auto validation_result = validate_jwt_token(token);
    if (!validation_result.has_value()) {
        result.error_message = "Invalid JWT token";
        result.validation_time_ns = get_elapsed_time(start_time);
        return result;
    }

    JWTPayload payload = validation_result.value();
    auto now = system_clock::now();
    auto clock_skew = seconds(30);

    if (payload.expires_at < now) {
        result.error_message = "Token has expired";
        result.validation_time_ns = get_elapsed_time(start_time);
        return result;
    }

    if (payload.issued_at > now + clock_skew) {
        result.error_message = "Token issued in the future";
        result.validation_time_ns = get_elapsed_time(start_time);
        return result;
    }

    if (payload.not_before.time_since_epoch().count() > 0) {
        if (now < payload.not_before - clock_skew) {
            result.error_message = "Token not yet valid";
            result.validation_time_ns = get_elapsed_time(start_time);
            return result;
        }
    }

    if (!is_issuer_allowed(payload.issuer)) {
        result.error_message = "Invalid token issuer";
        result.validation_time_ns = get_elapsed_time(start_time);
        return result;
    }

    if (!is_audience_allowed(payload.audience)) {
        result.error_message = "Invalid token audience";
        result.validation_time_ns = get_elapsed_time(start_time);
        return result;
    }

    if (config_.enable_blacklist && redis_) {
        string blacklist_key = "blacklist:token:" + token;
        if (redis_->exists(blacklist_key)) {
            result.error_message = "Token has been revoked";
            result.validation_time_ns = get_elapsed_time(start_time);
            return result;
        }

        if (!payload.jti.empty()) {
            string jti_key = "blacklist:jti:" + payload.jti;
            if (redis_->exists(jti_key)) {
                result.error_message = "Token ID has been revoked";
                result.validation_time_ns = get_elapsed_time(start_time);
                return result;
            }
        }
    }

    if (config_.enable_session_validation && redis_) {
        string session_key = "session:" + payload.session_id;
        auto session_data = redis_->get(session_key);

        if (!session_data.has_value()) {
            result.error_message = "Session not found or expired";
            result.validation_time_ns = get_elapsed_time(start_time);
            return result;
        }

        auto session_json = nlohmann::json::parse(*session_data);

        if (session_json["user_id"] != payload.user_id) {
            result.error_message = "Session user mismatch";
            result.validation_time_ns = get_elapsed_time(start_time);
            return result;
        }

        if (session_json["active"] == false) {
            result.error_message = "Session is inactive";
            result.validation_time_ns = get_elapsed_time(start_time);
            return result;
        }

        redis_->expire(session_key, chrono::hours(1));
    }

    if (config_.enable_device_validation && !payload.device_id.empty() && redis_) {
        string device_key = "device:" + payload.user_id + ":" + payload.device_id;
        auto trusted = redis_->get(device_key);

        if (!trusted.has_value()) {
            result.error_message = "Untrusted device";
            result.requires_2fa = true;
            result.validation_time_ns = get_elapsed_time(start_time);
            return result;
        }
    }

    if (config_.enable_user_rate_limit && redis_) {
        string rate_key = "ratelimit:user:" + payload.user_id;
        long long count = redis_->incr(rate_key);

        if (count == 1)
            redis_->expire(rate_key, chrono::seconds(60));

        int max_requests = get_user_rate_limit(payload.user_id);
        if (count > max_requests) {
            result.error_message = "User rate limit exceeded";
            result.validation_time_ns = get_elapsed_time(start_time);
            return result;
        }
    }

    auto time_until_expiry = payload.expires_at - now;
    auto refresh_threshold = minutes(5);
    bool needs_refresh = false;

    if (time_until_expiry < refresh_threshold && time_until_expiry > seconds(0)) {
        needs_refresh = true;
        result.should_refresh = true;
        result.new_token = generate_refresh_token(payload);
    }

    log_validation_attempt(
        payload.user_id,
        payload.session_id,
        request.client_ip,
        true,
        get_elapsed_time(start_time)
    );

    if (config_.enable_caching && redis_) {
        if (time_until_expiry > minutes(1)) {
            // cache por 5 minutos
            redis_->set(
                "token_cache:" + token,
                payload.user_id,
                chrono::minutes(5)
            );
        }
    }

    result.is_authenticated = true;
    result.payload = payload;
    result.needs_refresh = needs_refresh;
    result.validation_time_ns = get_elapsed_time(start_time);

    result.headers_to_forward["X-Authenticated-User"] = payload.user_id;
    result.headers_to_forward["X-Session-Id"]         = payload.session_id;
    result.headers_to_forward["X-User-Roles"]         = join_strings(payload.roles, ",");

    return result;
}