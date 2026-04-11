// DexKit - An high-performance runtime parsing library for dex
// implemented in C++.
// Copyright (C) 2022-2023 LuckyPray
// https://github.com/LuckyPray/DexKit
//
// This program is free software: you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation, either
// version 3 of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see
// <https://www.gnu.org/licenses/>.
// <https://github.com/LuckyPray/DexKit/blob/master/LICENSE>.

#include "dex_item.h"
#include "internal/using_strings_prefilter.h"

#include <bit>

namespace dexkit {

namespace internal {

struct UsingStringsPrefilterAtom {
    std::string value;
    schema::StringMatchType match_type = schema::StringMatchType::Contains;
    bool ignore_case = false;
    uint64_t bit = 0;
};

struct UsingStringsPrefilterNode {
    enum class Kind : uint8_t {
        Group = 1,
        AllOf,
        AnyOf,
    };

    Kind kind = Kind::Group;
    uint64_t group_mask = 0;
    std::vector<int> children;
};

struct UsingStringsPrefilterPlan {
    using AcTrie = acdat::AhoCorasickDoubleArrayTrie<std::string_view>;

    std::vector<UsingStringsPrefilterAtom> atoms;
    std::vector<UsingStringsPrefilterNode> nodes;
    AcTrie case_sensitive_trie;
    AcTrie ignore_case_trie;
    phmap::flat_hash_map<std::string_view, uint64_t> case_sensitive_keyword_masks;
    phmap::flat_hash_map<std::string_view, uint64_t> ignore_case_keyword_masks;
    uint64_t all_atom_mask = 0;
    uint64_t empty_equal_atom_mask = 0;
    int root_node_index = -1;

    [[nodiscard]] bool enabled() const {
        return root_node_index >= 0 && all_atom_mask != 0;
    }
};

namespace {

constexpr uint8_t kMethodUsingStringsPrefilterCacheScope = 241;
constexpr uint8_t kClassUsingStringsPrefilterCacheScope = 242;

template<typename T>
bool HasNonEmptyVector(const T *vector) {
    return vector != nullptr && vector->size() > 0;
}

const flatbuffers::Vector<flatbuffers::Offset<schema::StringMatcher>> *
GetUsingStringsMatchers(const schema::MethodMatcher *matcher) {
    return matcher == nullptr ? nullptr : matcher->using_strings();
}

const flatbuffers::Vector<flatbuffers::Offset<schema::StringMatcher>> *
GetUsingStringsMatchers(const schema::ClassMatcher *matcher) {
    return matcher == nullptr ? nullptr : matcher->using_strings();
}

auto GetAllOfMatchers(const schema::MethodMatcher *matcher) {
    return matcher == nullptr ? nullptr : matcher->all_of();
}

auto GetAllOfMatchers(const schema::ClassMatcher *matcher) {
    return matcher == nullptr ? nullptr : matcher->all_of();
}

auto GetAnyOfMatchers(const schema::MethodMatcher *matcher) {
    return matcher == nullptr ? nullptr : matcher->any_of();
}

auto GetAnyOfMatchers(const schema::ClassMatcher *matcher) {
    return matcher == nullptr ? nullptr : matcher->any_of();
}

void NormalizeStringMatcherValue(std::string_view &value, schema::StringMatchType &match_type) {
    if (match_type != schema::StringMatchType::SimilarRegex) {
        return;
    }
    match_type = schema::StringMatchType::Contains;
    if (value.starts_with('^')) {
        value = value.substr(1);
        match_type = schema::StringMatchType::StartWith;
    }
    if (value.ends_with('$')) {
        value = value.substr(0, value.size() - 1);
        if (match_type == schema::StringMatchType::StartWith) {
            match_type = schema::StringMatchType::Equal;
        } else {
            match_type = schema::StringMatchType::EndWith;
        }
    }
}

uint64_t TryAddPrefilterAtomBit(
        UsingStringsPrefilterPlan &plan,
        const schema::StringMatcher *matcher
) {
    if (matcher == nullptr || matcher->value() == nullptr) {
        return 0;
    }

    auto value = matcher->value()->string_view();
    auto match_type = matcher->match_type();
    NormalizeStringMatcherValue(value, match_type);

    if (value.empty() && match_type != schema::StringMatchType::Equal) {
        return 0;
    }

    for (const auto &atom: plan.atoms) {
        if (atom.match_type == match_type
            && atom.ignore_case == matcher->ignore_case()
            && atom.value == value) {
            return atom.bit;
        }
    }

    if (plan.atoms.size() >= 64) {
        return 0;
    }

    auto bit = uint64_t{1} << plan.atoms.size();
    plan.atoms.push_back(UsingStringsPrefilterAtom{
            .value = std::string(value),
            .match_type = match_type,
            .ignore_case = matcher->ignore_case(),
            .bit = bit,
    });
    plan.all_atom_mask |= bit;
    if (value.empty()) {
        plan.empty_equal_atom_mask |= bit;
    }
    return bit;
}

uint64_t BuildUsingStringsPrefilterMask(
        UsingStringsPrefilterPlan &plan,
        const flatbuffers::Vector<flatbuffers::Offset<schema::StringMatcher>> *matchers
) {
    uint64_t mask = 0;
    if (matchers == nullptr) {
        return mask;
    }
    for (int i = 0; i < matchers->size(); ++i) {
        mask |= TryAddPrefilterAtomBit(plan, matchers->Get(i));
    }
    return mask;
}

int AddUsingStringsPrefilterNode(
        UsingStringsPrefilterPlan &plan,
        UsingStringsPrefilterNode::Kind kind,
        uint64_t group_mask,
        std::vector<int> children = {}
) {
    auto node_index = static_cast<int>(plan.nodes.size());
    plan.nodes.push_back(UsingStringsPrefilterNode{
            .kind = kind,
            .group_mask = group_mask,
            .children = std::move(children),
    });
    return node_index;
}

template<typename Matcher>
int BuildUsingStringsPrefilterNode(
        UsingStringsPrefilterPlan &plan,
        const Matcher *matcher
) {
    if (matcher == nullptr) {
        return -1;
    }

    std::vector<int> conjuncts;
    auto local_mask = BuildUsingStringsPrefilterMask(plan, GetUsingStringsMatchers(matcher));
    if (local_mask != 0) {
        conjuncts.push_back(AddUsingStringsPrefilterNode(
                plan,
                UsingStringsPrefilterNode::Kind::Group,
                local_mask
        ));
    }

    auto all_of = GetAllOfMatchers(matcher);
    if (all_of != nullptr) {
        conjuncts.reserve(conjuncts.size() + all_of->size());
        for (int i = 0; i < all_of->size(); ++i) {
            auto child_index = BuildUsingStringsPrefilterNode(plan, all_of->Get(i));
            if (child_index >= 0) {
                conjuncts.push_back(child_index);
            }
        }
    }

    auto any_of = GetAnyOfMatchers(matcher);
    if (any_of != nullptr && any_of->size() > 0) {
        std::vector<int> disjuncts;
        disjuncts.reserve(any_of->size());
        auto fully_extractable = true;
        for (int i = 0; i < any_of->size(); ++i) {
            auto child_index = BuildUsingStringsPrefilterNode(plan, any_of->Get(i));
            if (child_index < 0) {
                fully_extractable = false;
                break;
            }
            disjuncts.push_back(child_index);
        }
        if (fully_extractable && !disjuncts.empty()) {
            if (disjuncts.size() == 1) {
                conjuncts.push_back(disjuncts[0]);
            } else {
                conjuncts.push_back(AddUsingStringsPrefilterNode(
                        plan,
                        UsingStringsPrefilterNode::Kind::AnyOf,
                        0,
                        std::move(disjuncts)
                ));
            }
        }
    }

    if (conjuncts.empty()) {
        return -1;
    }
    if (conjuncts.size() == 1) {
        return conjuncts[0];
    }
    return AddUsingStringsPrefilterNode(
            plan,
            UsingStringsPrefilterNode::Kind::AllOf,
            0,
            std::move(conjuncts)
    );
}

template<typename Matcher>
UsingStringsPrefilterPlan BuildUsingStringsPrefilterPlan(const Matcher *matcher) {
    UsingStringsPrefilterPlan plan;
    if (matcher == nullptr || !HasComposite(matcher)) {
        return plan;
    }

    plan.root_node_index = BuildUsingStringsPrefilterNode(plan, matcher);
    if (!plan.enabled()) {
        return {};
    }

    std::vector<std::pair<std::string_view, bool>> case_sensitive_keywords;
    std::vector<std::pair<std::string_view, bool>> ignore_case_keywords;
    case_sensitive_keywords.reserve(plan.atoms.size());
    ignore_case_keywords.reserve(plan.atoms.size());
    for (const auto &atom: plan.atoms) {
        if (atom.value.empty()) {
            continue;
        }
        auto keyword = std::string_view(atom.value);
        if (atom.ignore_case) {
            ignore_case_keywords.emplace_back(keyword, true);
            plan.ignore_case_keyword_masks[keyword] |= atom.bit;
        } else {
            case_sensitive_keywords.emplace_back(keyword, false);
            plan.case_sensitive_keyword_masks[keyword] |= atom.bit;
        }
    }
    if (!case_sensitive_keywords.empty()) {
        acdat::Builder<std::string_view>().Build(case_sensitive_keywords, &plan.case_sensitive_trie);
    }
    if (!ignore_case_keywords.empty()) {
        acdat::Builder<std::string_view>().Build(ignore_case_keywords, &plan.ignore_case_trie);
    }
    return plan;
}

template<typename Matcher>
UsingStringsPrefilterPlan *GetUsingStringsPrefilterPlan(
        uint8_t cache_scope,
        const Matcher *matcher,
        QueryContext &query_context
) {
    if (matcher == nullptr || !HasComposite(matcher)) {
        return nullptr;
    }
    auto *plan = query_context.GetOrCreateMatcherCache<UsingStringsPrefilterPlan>(
            cache_scope,
            POINT_CASE(matcher),
            [&]() {
                return BuildUsingStringsPrefilterPlan(matcher);
            }
    );
    return plan->enabled() ? plan : nullptr;
}

bool EvaluateUsingStringsPrefilterNode(
        const UsingStringsPrefilterPlan &plan,
        int node_index,
        uint64_t matched_bits
) {
    auto &node = plan.nodes[node_index];
    switch (node.kind) {
        case UsingStringsPrefilterNode::Kind::Group:
            return (matched_bits & node.group_mask) == node.group_mask;
        case UsingStringsPrefilterNode::Kind::AllOf:
            for (auto child_index: node.children) {
                if (!EvaluateUsingStringsPrefilterNode(plan, child_index, matched_bits)) {
                    return false;
                }
            }
            return true;
        case UsingStringsPrefilterNode::Kind::AnyOf:
            for (auto child_index: node.children) {
                if (EvaluateUsingStringsPrefilterNode(plan, child_index, matched_bits)) {
                    return true;
                }
            }
            return false;
    }
    return false;
}

void AccumulateUsingStringsPrefilterMatches(
        std::string_view str,
        acdat::AhoCorasickDoubleArrayTrie<std::string_view> &trie,
        const phmap::flat_hash_map<std::string_view, uint64_t> &keyword_masks,
        const std::vector<UsingStringsPrefilterAtom> &atoms,
        uint64_t &matched_bits
) {
    if (keyword_masks.empty()) {
        return;
    }
    std::function<void(int, int, std::string_view)> callback =
            [&](int begin, int end, std::string_view keyword) {
                auto it = keyword_masks.find(keyword);
                if (it == keyword_masks.end()) {
                    return;
                }
                auto remaining = it->second & ~matched_bits;
                while (remaining != 0) {
                    auto atom_index = static_cast<size_t>(std::countr_zero(remaining));
                    auto &atom = atoms[atom_index];
                    bool match = false;
                    switch (atom.match_type) {
                        case schema::StringMatchType::Contains: match = true; break;
                        case schema::StringMatchType::StartWith: match = (begin == 0); break;
                        case schema::StringMatchType::EndWith: match = (end == str.size()); break;
                        case schema::StringMatchType::Equal: match = (begin == 0 && end == str.size()); break;
                        case schema::StringMatchType::SimilarRegex: abort();
                    }
                    if (match) {
                        matched_bits |= atom.bit;
                    }
                    remaining &= (remaining - 1);
                }
            };
    trie.ParseText(str, callback);
}

template<typename EnumerateStringsFn>
bool MayMatchUsingStringsPrefilter(
        UsingStringsPrefilterPlan &plan,
        EnumerateStringsFn &&enumerate_strings
) {
    uint64_t matched_bits = 0;
    bool has_empty_string = false;

    auto visit_string = [&](std::string_view str) {
        if (str.empty()) {
            has_empty_string = true;
        }
        AccumulateUsingStringsPrefilterMatches(
                str,
                plan.case_sensitive_trie,
                plan.case_sensitive_keyword_masks,
                plan.atoms,
                matched_bits
        );
        AccumulateUsingStringsPrefilterMatches(
                str,
                plan.ignore_case_trie,
                plan.ignore_case_keyword_masks,
                plan.atoms,
                matched_bits
        );
        return EvaluateUsingStringsPrefilterNode(plan, plan.root_node_index, matched_bits);
    };

    if (enumerate_strings(visit_string)) {
        return true;
    }
    if (has_empty_string) {
        matched_bits |= plan.empty_equal_atom_mask;
    }
    return EvaluateUsingStringsPrefilterNode(plan, plan.root_node_index, matched_bits);
}

} // namespace

UsingStringsPrefilterPlan *GetMethodUsingStringsPrefilterPlan(
        const schema::MethodMatcher *matcher,
        QueryContext &query_context
) {
    return GetUsingStringsPrefilterPlan(kMethodUsingStringsPrefilterCacheScope, matcher, query_context);
}

UsingStringsPrefilterPlan *GetClassUsingStringsPrefilterPlan(
        const schema::ClassMatcher *matcher,
        QueryContext &query_context
) {
    return GetUsingStringsPrefilterPlan(kClassUsingStringsPrefilterCacheScope, matcher, query_context);
}

} // namespace internal

bool DexItem::MayMatchMethodUsingStringsPrefilter(
        uint32_t method_idx,
        internal::UsingStringsPrefilterPlan &plan
) {
    return internal::MayMatchUsingStringsPrefilter(plan, [&](auto &&visit_string) {
        auto &using_string_ids = this->method_using_string_ids[method_idx];
        for (auto string_id: using_string_ids) {
            if (visit_string(this->strings[string_id])) {
                return true;
            }
        }
        return false;
    });
}

bool DexItem::MayMatchClassUsingStringsPrefilter(
        uint32_t type_idx,
        internal::UsingStringsPrefilterPlan &plan
) {
    DexItem *scan_dex = this;
    auto scan_type_idx = type_idx;
    if (!scan_dex->type_def_flag[scan_type_idx]) {
        auto [declared_dex, declared_type_idx] = this->dexkit->GetClassDeclaredPair(this->type_names[type_idx]);
        if (declared_dex == nullptr) {
            return false;
        }
        scan_dex = declared_dex;
        scan_type_idx = declared_type_idx;
    }
    if (!scan_dex->type_def_flag[scan_type_idx]) {
        return false;
    }

    return internal::MayMatchUsingStringsPrefilter(plan, [&](auto &&visit_string) {
        for (auto method_idx: scan_dex->class_method_ids[scan_type_idx]) {
            auto &using_string_ids = scan_dex->method_using_string_ids[method_idx];
            for (auto string_id: using_string_ids) {
                if (visit_string(scan_dex->strings[string_id])) {
                    return true;
                }
            }
        }
        return false;
    });
}

} // namespace dexkit
