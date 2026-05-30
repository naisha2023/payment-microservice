#pragma once
#include <string>
#include <chrono>

using namespace std;
struct RedisConfig {
    string host     = "localhost";
    int    port     = 6379;
    string password = "";
    int    db       = 0;
    int    pool_size = 10;
    chrono::milliseconds connect_timeout{2000};
    chrono::milliseconds socket_timeout{2000};
};
