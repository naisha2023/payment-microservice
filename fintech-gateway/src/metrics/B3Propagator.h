#pragma once
#include <string>
#include <drogon/HttpRequest.h>

struct B3Context {
    std::string trace_id;     // 128-bit hex (32 chars)
    std::string span_id;      // 64-bit hex  (16 chars)
    std::string parent_span_id; // vacío si es el primer span
    std::string sampled;      // "1" o "0"
};

class B3Propagator {
public:
    // Extrae B3 headers del request entrante o genera uno nuevo
    static B3Context extract(const drogon::HttpRequest& req);

    // Genera un nuevo spanId para el gateway (hijo del span entrante)
    static B3Context create_child(const B3Context& parent);

    // Inyecta los headers B3 en el request upstream
    static void inject(const drogon::HttpRequestPtr& req,
                       const B3Context&              ctx);

private:
    static std::string generate_trace_id(); // 128-bit random hex
    static std::string generate_span_id();  //  64-bit random hex
};