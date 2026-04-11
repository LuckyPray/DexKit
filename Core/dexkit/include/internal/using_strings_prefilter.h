#pragma once

namespace dexkit {

class QueryContext;

namespace schema {
struct MethodMatcher;
struct ClassMatcher;
}

namespace internal {

struct UsingStringsPrefilterPlan;

UsingStringsPrefilterPlan *GetMethodUsingStringsPrefilterPlan(
        const schema::MethodMatcher *matcher,
        QueryContext &query_context
);

UsingStringsPrefilterPlan *GetClassUsingStringsPrefilterPlan(
        const schema::ClassMatcher *matcher,
        QueryContext &query_context
);

} // namespace internal

} // namespace dexkit
