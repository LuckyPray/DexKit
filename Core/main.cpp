#include <iostream>
#include "slicer/reader.h"
#include "querys_generated.h"

using namespace schema;

template<typename T>
const T *From(const void *buf) {
    return ::flatbuffers::GetRoot<T>(buf);
}

int main() {
    flatbuffers::FlatBufferBuilder builder;
    auto using_strings = builder.CreateVector(std::vector<flatbuffers::Offset<StringMatcher>>{
        CreateStringMatcher(builder, StringMatchType::EndWith,
                            builder.CreateString("abc"))
    });
    auto union_id = builder.CreateString("abc");
    BatchUsingStringsMatcherBuilder builder_(builder);
    builder_.add_using_strings(using_strings);
    builder_.add_union_id(union_id);
    auto matcher = builder_.Finish();
    builder.Finish(matcher);

    // serialize to buffer
    auto buffer = builder.GetBufferPointer();
    auto size = builder.GetSize();
    std::cout << "buffer size: " << size << std::endl;

    // deserialize from buffer
    auto matcher2 = From<BatchUsingStringsMatcher>(buffer);
    std::cout << "using_strings * = " << matcher2->using_strings() << std::endl;
    if (matcher2->using_strings()) {
        for (auto value : *matcher2->using_strings()) {
            std::cout << "value->type() = " << EnumNameStringMatchType(value->type()) << std::endl;
            std::cout << "value->value()->c_str() = " << value->value()->c_str() << std::endl;
        }
    }
    return 0;
}
