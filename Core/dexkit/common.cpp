#include "common.h"

#include <cstdio>

namespace dexkit {

void _checkFailed(const char *expr, int line, const char *file) {
    printf("\nDEXKIT_CHECK failed [%s] at %s:%d\n\n", expr, file, line);
    abort();
}

}