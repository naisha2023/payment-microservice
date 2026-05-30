#pragma once
#include <string>
#include <mutex>
#include <chrono>

enum class CircuitState {
    CLOSED,
    OPEN,
    HALF_OPEN
};

struct CircuitBreakerConfig {
    int    failure_threshold    = 5;     // fallas para abrir
    int    success_threshold    = 2;     // éxitos en HALF_OPEN para cerrar
    int    timeout_seconds      = 30;    // tiempo en OPEN antes de HALF_OPEN
    double failure_rate_percent = 50.0;  // % de fallas en ventana
    int    window_size          = 10;    // requests en ventana deslizante
};

class CircuitBreaker {
public:
    explicit CircuitBreaker(const std::string&          name,
                            const CircuitBreakerConfig& config = {});

    // Llama antes de hacer el request — false significa circuito abierto
    bool allow_request();

    // Llama después del request
    void record_success();
    void record_failure();

    CircuitState state() const;
    std::string  name()  const { return name_; }
    std::string  state_string() const;

private:
    void     transition_to(CircuitState state);
    bool     should_open() const;

    std::string            name_;
    CircuitBreakerConfig   config_;
    mutable std::mutex     mutex_;

    CircuitState           state_         = CircuitState::CLOSED;
    int                    failure_count_ = 0;
    int                    success_count_ = 0;
    int                    total_count_   = 0;

    std::chrono::steady_clock::time_point opened_at_;
};