#include "CircuitBreakerRegistry.h"

using namespace std;

void CircuitBreakerRegistry::configure(const string&             name,
                                        const CircuitBreakerConfig& config)
{
    lock_guard<mutex> lock(mutex_);
    configs_[name] = config;
}

shared_ptr<CircuitBreaker> CircuitBreakerRegistry::get(const string& name)
{
    lock_guard<mutex> lock(mutex_);

    auto it = registry_.find(name);
    if (it != registry_.end())
        return it->second;

    // Crea con config personalizada si existe, si no usa defaults
    CircuitBreakerConfig config;
    auto cfg_it = configs_.find(name);
    if (cfg_it != configs_.end())
        config = cfg_it->second;

    auto cb = make_shared<CircuitBreaker>(name, config);
    registry_[name] = cb;
    return cb;
}