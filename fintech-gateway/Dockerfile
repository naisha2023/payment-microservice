FROM debian:bookworm AS build

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    cmake \
    git \
    ninja-build \
    pkg-config \
    libssl-dev \
    zlib1g-dev \
    uuid-dev \
    libjsoncpp-dev \
    libc-ares-dev \
    libbrotli-dev \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY drogon /app/drogon
COPY CMakeLists.txt .
COPY src ./src

RUN cd /app/drogon && git submodule update --init --recursive

RUN cmake -S . -B build -DCMAKE_BUILD_TYPE=Release -GNinja && \
    cmake --build build -j"$(nproc)"

FROM gcr.io/distroless/cc-debian12

WORKDIR /app

COPY --from=build /app/build/fintech_gateway /app/fintech_gateway

COPY --from=build /usr/lib/x86_64-linux-gnu/libjsoncpp.so.25 /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libcares.so.2 /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libbrotlidec.so.1 /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libbrotlienc.so.1 /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libbrotlicommon.so.1 /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libuuid.so.1 /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libz.so.1 /usr/lib/x86_64-linux-gnu/

EXPOSE 8080

ENTRYPOINT ["/app/fintech_gateway"]