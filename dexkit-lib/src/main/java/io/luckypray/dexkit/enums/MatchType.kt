package io.luckypray.dexkit.enums

enum class MatchType {

    /**
     * full match
     *
     *     full_match(search = "abc", target = "abc") = true
     *     full_match(search = "abc", target = "abcd") = false
     */
    FULL,

    /**
     * contains match
     *
     *     contains_match(search = "abc", target = "abcd") = true
     *     contains_match(search = "abc", target = "abc") = true
     *     contains_match(search = "abc", target = "ab") = false
     */
    CONTAINS,

    /**
     * similar regex matches, only support: '^', '$'
     *
     *     similar_regex_match(search = "abc", target = "abc") == true
     *     similar_regex_match(search = "^abc", target = "abc") == true
     *     similar_regex_match(search = "abc$", target = "bc") == false
     *     similar_regex_match(search = "^abc$", target = "abc") == true
     *     similar_regex_match(search = "^abc$", target = "abcd") == false
     */
    SIMILAR_REGEX,
}