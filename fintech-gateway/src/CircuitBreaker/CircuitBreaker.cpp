#include "CircuitBreaker.h"
#include <iostream>

using namespace std;
using namespace chrono;

CircuitBreaker::CircuitBreaker(const string& name,
                               const CircuitBreakerConfig& config)
    : name_(name), config_(config) {}

// ── allow_request ─────────────────────────────────────────────

bool CircuitBreaker::allow_request()
{
    lock_guard<mutex> lock(mutex_);

    if (state_ == CircuitState::CLOSED)
        return true;

    if (state_ == CircuitState::OPEN)
    {
        auto elapsed = duration_cast<seconds>(
            steady_clock::now() - opened_at_).count();

        if (elapsed >= config_.timeout_seconds)
        {
            transition_to(CircuitState::HALF_OPEN);
            return true;   // deja pasar un request de prueba
        }
        return false;
    }

    // HALF_OPEN — deja pasar solo un request a la vez
    return true;
}

// ── record_success ────────────────────────────────────────────

void CircuitBreaker::record_success()
{
    lock_guard<mutex> lock(mutex_);

    if (state_ == CircuitState::HALF_OPEN)
    {
        success_count_++;
        if (success_count_ >= config_.success_threshold)
        {
            failure_count_ = 0;
            success_count_ = 0;
            total_count_   = 0;
            transition_to(CircuitState::CLOSED);
        }
        return;
    }

    if (state_ == CircuitState::CLOSED)
    {
        total_count_++;
        // En ventana deslizante, un éxito no resetea fallas
        // pero sí cuenta para el cálculo de tasa
        if (total_count_ > config_.window_size)
        {
            failure_count_ = max(0, failure_count_ - 1);
            total_count_   = config_.window_size;
        }
    }
}

// ── record_failure ────────────────────────────────────────────

void CircuitBreaker::record_failure()
{
    lock_guard<mutex> lock(mutex_);

    if (state_ == CircuitState::HALF_OPEN)
    {
        transition_to(CircuitState::OPEN);
        return;
    }

    if (state_ == CircuitState::CLOSED)
    {
        failure_count_++;
        total_count_++;

        if (total_count_ > config_.window_size)
            total_count_ = config_.window_size;

        if (should_open())
            transition_to(CircuitState::OPEN);
    }
}

// ── should_open ───────────────────────────────────────────────

bool CircuitBreaker::should_open() const
{
    if (failure_count_ >= config_.failure_threshold)
        return true;

    if (total_count_ >= config_.window_size)
    {
        double rate = (double)failure_count_ / total_count_ * 100.0;
        if (rate >= config_.failure_rate_percent)
            return true;
    }

    return false;
}

// ── transition_to ─────────────────────────────────────────────

void CircuitBreaker::transition_to(CircuitState new_state)
{
    state_ = new_state;

    if (new_state == CircuitState::OPEN)
    {
        opened_at_     = steady_clock::now();
        success_count_ = 0;
        cerr << "[CircuitBreaker] " << name_
             << " → OPEN (failures: " << failure_count_ << ")\n";
    }
    else if (new_state == CircuitState::HALF_OPEN)
    {
        success_count_ = 0;
        cerr << "[CircuitBreaker] " << name_ << " → HALF_OPEN\n";
    }
    else
    {
        cerr << "[CircuitBreaker] " << name_ << " → CLOSED\n";
    }
}

// ── helpers ───────────────────────────────────────────────────

CircuitState CircuitBreaker::state() const
{
    lock_guard<mutex> lock(mutex_);
    return state_;
}

string CircuitBreaker::state_string() const
{
    switch (state()) {
        case CircuitState::CLOSED:    return "CLOSED";
        case CircuitState::OPEN:      return "OPEN";
        case CircuitState::HALF_OPEN: return "HALF_OPEN";
    }
    return "UNKNOWN";
}