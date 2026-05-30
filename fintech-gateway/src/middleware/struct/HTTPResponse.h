#pragma once
#include <string>
#include <unordered_map>

using namespace std;

struct HTTPResponse {
    int status_code = 200;
    unordered_map<string, string> headers;
    string body;
    string content_type = "application/json";
};