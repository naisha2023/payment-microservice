FROM debian:bookworm AS build

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/* && apt-get install -y --no-install-recommends \
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

RUN git clone --recurse-submodules --depth 1 --branch v1.9.13 \
    https://github.com/drogonframework/drogon.git /app/drogon

RUN cmake -S /app/drogon -B /app/drogon/build \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_EXAMPLES=OFF \
        -DBUILD_CTL=OFF \
        -GNinja && \
    cmake --build /app/drogon/build -j"$(nproc)" && \
    cmake --install /app/drogon/build

WORKDIR /app

COPY CMakeLists.txt .
COPY src ./src
COPY certs ./certs

RUN cmake -S . -B build -DCMAKE_BUILD_TYPE=Release -GNinja

RUN cmake --build build -j"$(nproc)" 2>&1 || \
    (cat build/CMakeFiles/CMakeError.log 2>/dev/null; exit 1)


FROM debian:bookworm-slim

ENV DEBIAN_FRONTEND=noninteractive

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/fintech_gateway /app/fintech_gateway
COPY --from=build /app/certs /app/certs

COPY --from=build /usr/lib/x86_64-linux-gnu/libjsoncpp.so.25 /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libcares.so.2 /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libbrotlidec.so.1 /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libbrotlienc.so.1 /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libbrotlicommon.so.1 /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libuuid.so.1 /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libz.so.1 /usr/lib/x86_64-linux-gnu/

RUN cp /app/certs/ca.crt /usr/local/share/ca-certificates/fintech-ca.crt && \
    update-ca-certificates

EXPOSE 8080

ENTRYPOINT ["/app/fintech_gateway"]