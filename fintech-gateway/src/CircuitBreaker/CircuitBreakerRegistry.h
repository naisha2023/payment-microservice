#pragma once
#include "CircuitBreaker.h"
#include <string>
#include <unordered_map>
#include <memory>
#include <mutex>

class CircuitBreakerRegistry {
public:
    // Obtiene o crea el circuito para un servicio
    std::shared_ptr<CircuitBreaker> get(const std::string& service_name);

    // Configuración por servicio — llama antes de get()
    void configure(const std::string&          service_name,
                   const CircuitBreakerConfig& config);

private:
    std::mutex mutex_;
    std::unordered_map<std::string,
        std::shared_ptr<CircuitBreaker>> registry_;
    std::unordered_map<std::string,
        CircuitBreakerConfig>            configs_;
};