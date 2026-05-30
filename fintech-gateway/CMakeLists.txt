cmake_minimum_required(VERSION 3.20)
project(fintech_gateway)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

# ─── Dependencies ─────────────────────────────────────────────────────────────

# Vendored Drogon (includes HttpClient::setCaPath for custom CA trust).
set(BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(BUILD_CTL OFF CACHE BOOL "" FORCE)
set(BUILD_DOC OFF CACHE BOOL "" FORCE)

set(BUILD_ORM OFF CACHE BOOL "" FORCE)
set(BUILD_POSTGRESQL OFF CACHE BOOL "" FORCE)
set(BUILD_MYSQL OFF CACHE BOOL "" FORCE)

add_subdirectory(drogon)

find_library(JSONCPP_LIB jsoncpp REQUIRED)
find_path(JSONCPP_INCLUDE_DIR json/json.h
    REQUIRED
    PATHS /usr/include/jsoncpp
)

include(FetchContent)

# jwt-cpp
FetchContent_Declare(jwt-cpp
    GIT_REPOSITORY https://github.com/Thalhammer/jwt-cpp.git
    GIT_TAG        v0.7.0
)
FetchContent_MakeAvailable(jwt-cpp)

# hiredis
FetchContent_Declare(hiredis
    GIT_REPOSITORY https://github.com/redis/hiredis.git
    GIT_TAG        v1.2.0
)
FetchContent_MakeAvailable(hiredis)

# Apuntar redis++ a hiredis de FetchContent
set(HIREDIS_HEADER ${CMAKE_BINARY_DIR}/hiredis_include CACHE PATH "" FORCE)
file(COPY ${hiredis_SOURCE_DIR}/hiredis.h   DESTINATION ${CMAKE_BINARY_DIR}/hiredis_include/hiredis)
file(COPY ${hiredis_SOURCE_DIR}/read.h      DESTINATION ${CMAKE_BINARY_DIR}/hiredis_include/hiredis)
file(COPY ${hiredis_SOURCE_DIR}/sds.h       DESTINATION ${CMAKE_BINARY_DIR}/hiredis_include/hiredis)
file(COPY ${hiredis_SOURCE_DIR}/alloc.h     DESTINATION ${CMAKE_BINARY_DIR}/hiredis_include/hiredis)
file(COPY ${hiredis_SOURCE_DIR}/async.h     DESTINATION ${CMAKE_BINARY_DIR}/hiredis_include/hiredis)
file(COPY ${hiredis_SOURCE_DIR}/net.h       DESTINATION ${CMAKE_BINARY_DIR}/hiredis_include/hiredis)
set(HIREDIS_LIB hiredis CACHE STRING "" FORCE)

# redis++ (static only — shared lib + static hiredis fails PIC link in Docker)
set(REDIS_PLUS_PLUS_BUILD_TEST     OFF CACHE BOOL "" FORCE)
set(REDIS_PLUS_PLUS_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(REDIS_PLUS_PLUS_BUILD_SHARED   OFF CACHE BOOL "" FORCE)
set(REDIS_PLUS_PLUS_BUILD_STATIC   ON CACHE BOOL "" FORCE)
FetchContent_Declare(redis++
    GIT_REPOSITORY https://github.com/sewenew/redis-plus-plus.git
    GIT_TAG        1.3.10
)
FetchContent_MakeAvailable(redis++)

# ─── Sources ──────────────────────────────────────────────────────────────────

add_executable(fintech_gateway
    src/main.cpp
    src/middleware/JWTMiddleware.cpp
    src/redis/RedisPool.cpp
    src/controllers/HealthController.cpp
    src/controllers/ProxyController.cpp
    src/rate/RateLimiter.cpp
    src/CircuitBreaker/CircuitBreaker.cpp
    src/CircuitBreaker/CircuitBreakerRegistry.cpp
    src/metrics/B3Propagator.cpp
    src/config/CorsFilter.cpp
)

# ─── Includes ─────────────────────────────────────────────────────────────────

target_include_directories(fintech_gateway
    PRIVATE
        ${CMAKE_SOURCE_DIR}/src
        ${JSONCPP_INCLUDE_DIR}
        ${CMAKE_BINARY_DIR}/_deps/jwt-cpp-src/include
        ${CMAKE_BINARY_DIR}/_deps/nlohmann_json-src/include
        ${CMAKE_BINARY_DIR}/_deps/hiredis-src
        ${CMAKE_BINARY_DIR}/_deps/redis++-src/src
        ${CMAKE_BINARY_DIR}/hiredis_include
)

# ─── Links ────────────────────────────────────────────────────────────────────

target_link_libraries(fintech_gateway
    PRIVATE
        drogon
        ${JSONCPP_LIB}
        jwt-cpp::jwt-cpp
        redis++::redis++_static
        hiredis::hiredis
)