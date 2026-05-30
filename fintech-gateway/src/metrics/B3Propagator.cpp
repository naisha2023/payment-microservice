#include "B3Propagator.h"
#include <random>
#include <sstream>
#include <iomanip>

using namespace std;

// ── Generadores de IDs aleatorios ────────────────────────────

static string random_hex(int bytes)
{
    random_device              rd;
    mt19937_64                 gen(rd());
    uniform_int_distribution<> dist(0, 255);

    ostringstream oss;
    for (int i = 0; i < bytes; ++i)
        oss << hex << setw(2) << setfill('0') << dist(gen);
    return oss.str();
}

string B3Propagator::generate_trace_id() { return random_hex(16); } // 128-bit
string B3Propagator::generate_span_id()  { return random_hex(8);  } //  64-bit

// ── Extract ───────────────────────────────────────────────────

B3Context B3Propagator::extract(const drogon::HttpRequest& req)
{
    B3Context ctx;

    // Intenta multi-header primero
    ctx.trace_id      = req.getHeader("X-B3-TraceId");
    ctx.span_id       = req.getHeader("X-B3-SpanId");
    ctx.parent_span_id = req.getHeader("X-B3-ParentSpanId");
    ctx.sampled       = req.getHeader("X-B3-Sampled");

    // Si no hay multi-header, intenta single header "b3"
    if (ctx.trace_id.empty()) {
        string b3 = req.getHeader("b3");
        if (!b3.empty()) {
            // Formato: {traceId}-{spanId}-{sampled}-{parentSpanId}
            istringstream ss(b3);
            string token;
            vector<string> parts;
            while (getline(ss, token, '-'))
                parts.push_back(token);

            if (parts.size() >= 1) ctx.trace_id       = parts[0];
            if (parts.size() >= 2) ctx.span_id         = parts[1];
            if (parts.size() >= 3) ctx.sampled         = parts[2];
            if (parts.size() >= 4) ctx.parent_span_id  = parts[3];
        }
    }

    // Si no viene ningún header, genera un trace nuevo
    if (ctx.trace_id.empty())
        ctx.trace_id = generate_trace_id();

    if (ctx.span_id.empty())
        ctx.span_id = generate_span_id();

    if (ctx.sampled.empty())
        ctx.sampled = "1";  // samplea por defecto

    return ctx;
}

// ── Create child ──────────────────────────────────────────────

B3Context B3Propagator::create_child(const B3Context& parent)
{
    B3Context child;
    child.trace_id       = parent.trace_id;       // mismo trace
    child.parent_span_id = parent.span_id;        // span del gateway es el padre
    child.span_id        = generate_span_id();    // nuevo span para el upstream
    child.sampled        = parent.sampled;
    return child;
}

// ── Inject ────────────────────────────────────────────────────

void B3Propagator::inject(const drogon::HttpRequestPtr& req,
                           const B3Context&              ctx)
{
    // Multi-header B3 (compatible con Spring Sleuth / Micrometer)
    req->addHeader("X-B3-TraceId",  ctx.trace_id);
    req->addHeader("X-B3-SpanId",   ctx.span_id);
    req->addHeader("X-B3-Sampled",  ctx.sampled);

    if (!ctx.parent_span_id.empty())
        req->addHeader("X-B3-ParentSpanId", ctx.parent_span_id);

    // Single header también, por compatibilidad
    string b3_single = ctx.trace_id + "-" + ctx.span_id +
                       "-" + ctx.sampled;
    if (!ctx.parent_span_id.empty())
        b3_single += "-" + ctx.parent_span_id;

    req->addHeader("b3", b3_single);
}