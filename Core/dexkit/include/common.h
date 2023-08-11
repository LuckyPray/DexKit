#pragma once

namespace dexkit {

void _checkFailed(const char *expr, int line, const char *file);

#ifndef DEXKIT_CHECK
#ifdef NDEBUG
#define DEXKIT_CHECK(expr)
#else
#define DEXKIT_CHECK(expr) do { if(!(expr)) dexkit::_checkFailed(#expr, __LINE__, __FILE__); } while(false)
#endif
#endif

}