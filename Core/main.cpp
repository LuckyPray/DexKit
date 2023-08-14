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

int SharedPtrVoidCast(dexkit::DexKit &dexkit) {
    std::map<void *, std::shared_ptr<void>> map;
    auto ptr = &dexkit;
    auto vector = std::vector<int>{114, 514};
    map[ptr] = std::make_shared<std::vector<int>>(vector);
    printf("map size: %lld\n", map.size());
    auto q = *reinterpret_cast<std::shared_ptr<std::vector<int>>*>(&map[ptr]);
    printf("map[ptr]: %d\n", (*q)[0]);
    assert((*q)[0] == vector[0]);
    return 0;
}

int ThreadVariableTest() {
    std::vector<std::future<void>> results;
    ThreadPool pool(2);
    for (int i = 0; i < 10000; ++i) {
        results.push_back(pool.enqueue([i]() {
            ThreadVariable::SetThreadVariable<int>(i, i);
            auto thread_id = std::this_thread::get_id();
            auto p = ThreadVariable::GetThreadVariable<int>(i);
            printf("thread id: %p, idx: %d, %d\n", thread_id, i, *p);
        }));
    }
    return 0;
}

int KmpTest() {
    assert(kmp::FindIndex("abc", "abc") == 0);
    assert(kmp::FindIndex("abc", "bc") == 1);
    assert(kmp::FindIndex("abc", "c") == 2);
    assert(kmp::FindIndex("abc", "d") == -1);
    assert(kmp::FindIndex("ABc", "abc", true) == 0);
    assert(kmp::FindIndex("aBc", "AbC", true) == 0);
    assert(kmp::FindIndex("aBc", "Bc", true) == 1);
    assert(kmp::FindIndex("ABc", "d", true) == -1);
    printf("%d", kmp::FindIndex("abc", ""));
    return 0;
}

int ACTrieTest() {
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
    return 0;
}

int FlatBufferTest() {
    flatbuffers::FlatBufferBuilder builder;
    auto using_strings = builder.CreateVector(std::vector<flatbuffers::Offset<StringMatcher>>{
            CreateStringMatcher(builder, builder.CreateString("abc"), StringMatchType::EndWith, false)
    });
    auto union_key = builder.CreateString("abc");
    BatchUsingStringsMatcherBuilder builder_(builder);
    builder_.add_using_strings(using_strings);
    builder_.add_union_key(union_key);
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
        for (auto value: *matcher2->using_strings()) {
            std::cout << "value->type() = " << EnumNameStringMatchType(value->match_type()) << std::endl;
            std::cout << "value->value()->c_str() = " << value->value()->c_str() << std::endl;
        }
    }
    return 0;
}


int DexKitBatchFindClassTest(dexkit::DexKit &dexkit) {
    printf("-----------DexKitBatchFindClassTest Start-----------\n");
    flatbuffers::FlatBufferBuilder fbb;
    std::vector<flatbuffers::Offset<BatchUsingStringsMatcher>> matchers{
            CreateBatchUsingStringsMatcher(
                    fbb,
                    fbb.CreateString("Lcom/tencent/mobileqq/activity/ChatActivityFacade;"),
                    fbb.CreateVector(std::vector<flatbuffers::Offset<StringMatcher>>{
                            CreateStringMatcher(fbb, fbb.CreateString("reSendEmo"), StringMatchType::StartWith, false),
                    })
            ),
            CreateBatchUsingStringsMatcher(
                    fbb,
                    fbb.CreateString("Lcooperation/qzone/PlatformInfor;"),
                    fbb.CreateVector(std::vector<flatbuffers::Offset<StringMatcher>>{
                            CreateStringMatcher(fbb, fbb.CreateString("52b7f2"), StringMatchType::Contains, false),
                            CreateStringMatcher(fbb, fbb.CreateString("qimei"), StringMatchType::Contains, false)
                    })
            ),
            CreateBatchUsingStringsMatcher(
                    fbb,
                    fbb.CreateString("Lcom/tencent/mobileqq/troop/clockin/handler/TroopClockInHandler;"),
                    fbb.CreateVector(std::vector<flatbuffers::Offset<StringMatcher>>{
                            CreateStringMatcher(fbb, fbb.CreateString("TroopClockInHandler"), StringMatchType::Equal, false),
                    })
            ),
            CreateBatchUsingStringsMatcher(
                    fbb,
                    fbb.CreateString("com.tencent.widget.CustomWidgetUtil"),
                    fbb.CreateVector(std::vector<flatbuffers::Offset<StringMatcher>>{
                            CreateStringMatcher(fbb, fbb.CreateString("^NEW$"), StringMatchType::SimilarRegex, false),
                    })
            ),
    };

    auto find = CreateBatchFindClassUsingStrings(fbb, fbb.CreateString("com.tencent"), 0, fbb.CreateVector(matchers));
    fbb.Finish(find);

    auto buf = fbb.GetBufferPointer();
    auto query = From<BatchFindClassUsingStrings>(buf);

    auto builder = dexkit.BatchFindClassUsingStrings(query);
    auto buffer = builder->GetBufferPointer();
    auto size = builder->GetSize();
    printf("buffer size: %d\n", size);

    auto result = From<BatchClassMetaArrayHolder>(buffer);
    printf("result->items()->size() = %d\n", result->items()->size());
    for (int i = 0; i < result->items()->size(); ++i) {
        auto item = result->items()->Get(i);
        auto key = item->union_key();
        auto values = item->classes();
        printf("union key: %s\n", key->string_view().data());
        for (int j = 0; j < values->size(); ++j) {
            auto cls = values->Get(j);
            printf("\tdex: %02d, class: %s\n", cls->dex_id(), cls->dex_descriptor()->string_view().data());
        }
    }
    return 0;
}


int DexKitBatchFindMethodTest(dexkit::DexKit &dexkit) {
    printf("-----------DexKitBatchFindMethodTest Start-----------\n");
    flatbuffers::FlatBufferBuilder fbb;
    std::vector<flatbuffers::Offset<BatchUsingStringsMatcher>> matchers{
            CreateBatchUsingStringsMatcher(
                    fbb,
                    fbb.CreateString("Lcom/tencent/mobileqq/activity/ChatActivityFacade;"),
                    fbb.CreateVector(std::vector<flatbuffers::Offset<StringMatcher>>{
                            CreateStringMatcher(fbb, fbb.CreateString("reSendEmo"), StringMatchType::StartWith, false),
                    })
            ),
            CreateBatchUsingStringsMatcher(
                    fbb,
                    fbb.CreateString("Lcooperation/qzone/PlatformInfor;"),
                    fbb.CreateVector(std::vector<flatbuffers::Offset<StringMatcher>>{
                            CreateStringMatcher(fbb, fbb.CreateString("qimei="), StringMatchType::Equal, false)
                    })
            ),
            CreateBatchUsingStringsMatcher(
                    fbb,
                    fbb.CreateString("Lcom/tencent/mobileqq/troop/clockin/handler/TroopClockInHandler;"),
                    fbb.CreateVector(std::vector<flatbuffers::Offset<StringMatcher>>{
                            CreateStringMatcher(fbb, fbb.CreateString("TroopClockInHandler"), StringMatchType::Equal, false),
                    })
            ),
            CreateBatchUsingStringsMatcher(
                    fbb,
                    fbb.CreateString("com.tencent.widget.CustomWidgetUtil"),
                    fbb.CreateVector(std::vector<flatbuffers::Offset<StringMatcher>>{
                            CreateStringMatcher(fbb, fbb.CreateString("^NEW$"), StringMatchType::SimilarRegex, false),
                    })
            ),
    };

    auto find = CreateBatchFindMethodUsingStrings(fbb, fbb.CreateString("com.tencent"), 0, 0, fbb.CreateVector(matchers));
    fbb.Finish(find);

    auto buf = fbb.GetBufferPointer();
    auto query = From<BatchFindMethodUsingStrings>(buf);

    auto builder = dexkit.BatchFindMethodUsingStrings(query);
    auto buffer = builder->GetBufferPointer();
    auto size = builder->GetSize();
    printf("buffer size: %d\n", size);

    auto result = From<BatchMethodMetaArrayHolder>(buffer);
    printf("result->items()->size() = %d\n", result->items()->size());
    for (int i = 0; i < result->items()->size(); ++i) {
        auto item = result->items()->Get(i);
        auto key = item->union_key();
        auto values = item->methods();
        printf("union key: %s\n", key->string_view().data());
        for (int j = 0; j < values->size(); ++j) {
            auto cls = values->Get(j);
            printf("\tdex: %02d, class: %s\n", cls->dex_id(), cls->dex_descriptor()->string_view().data());
        }
    }
    return 0;
}

int main() {
    auto dexkit = dexkit::DexKit("../apks/qq-8.9.2.apk");
    printf("DexCount: %d\n", dexkit.GetDexNum());
    SharedPtrVoidCast(dexkit);
//    KmpTest();
//    ACTrieTest();
//    FlatBufferTest();
//    DexKitBatchFindClassTest(dexkit);
//    DexKitBatchFindMethodTest(dexkit);

    return 0;
}
