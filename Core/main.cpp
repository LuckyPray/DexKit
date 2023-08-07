#include <iostream>
#include "slicer/reader.h"
#include "schema/querys_generated.h"
#include "dexkit.h"
#include "beans.h"
#include "acdat/Builder.h"

using namespace dexkit::schema;

template<typename T>
const T *From(const void *buf) {
    return ::flatbuffers::GetRoot<T>(buf);
}

int main() {
    auto acTrie = acdat::AhoCorasickDoubleArrayTrie<std::string_view>();
    auto keywords = std::vector<std::pair<std::string_view, bool>>();
    keywords.emplace_back("HELLO", true);
    keywords.emplace_back("Hello", false);
    keywords.emplace_back("hello", false);
    std::string_view text = "hello World! Hello~";
    std::function<void(int, int, std::string_view)> callback = [&text](int start, int end, std::string_view value) {
        std::cout << text << "[" << start << ":" << end << "]=" << value << std::endl;
    };
    acdat::Builder<std::string_view>().Build(keywords, &acTrie);
    acTrie.ParseText(text, callback);
//    flatbuffers::FlatBufferBuilder builder;
//    auto using_strings = builder.CreateVector(std::vector<flatbuffers::Offset<StringMatcher>>{
//        CreateStringMatcher(builder, builder.CreateString("abc"), StringMatchType::EndWith, false)
//    });
//    auto union_key = builder.CreateString("abc");
//    BatchUsingStringsMatcherBuilder builder_(builder);
//    builder_.add_using_strings(using_strings);
//    builder_.add_union_key(union_key);
//    auto matcher = builder_.Finish();
//    builder.Finish(matcher);
//
//    // serialize to buffer
//    auto buffer = builder.GetBufferPointer();
//    auto size = builder.GetSize();
//    std::cout << "buffer size: " << size << std::endl;
//
//    // deserialize from buffer
//    auto matcher2 = From<BatchUsingStringsMatcher>(buffer);
//    std::cout << "using_strings * = " << matcher2->using_strings() << std::endl;
//    if (matcher2->using_strings()) {
//        for (auto value : *matcher2->using_strings()) {
//            std::cout << "value->type() = " << EnumNameStringMatchType(value->type()) << std::endl;
//            std::cout << "value->value()->c_str() = " << value->value()->c_str() << std::endl;
//        }
//    }
    return 0;
}
