#include <iostream>
#include <sstream>

#include "ThreadPool.h"
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
    printf("map size: %zu\n", map.size());
    auto q = *reinterpret_cast<std::shared_ptr<std::vector<int>> *>(&map[ptr]);
    printf("map[ptr]: %d\n", (*q)[0]);
    assert((*q)[0] == vector[0]);
    return 0;
}

int ThreadVariableTest() {
    ThreadPool pool(2);
    for (int i = 0; i < 10000; ++i) {
        pool.enqueue([i]() {
            ThreadVariable::SetThreadVariable<int>(i, i);
            auto thread_id = std::this_thread::get_id();
            auto p = ThreadVariable::GetThreadVariable<int>(i);
            std::ostringstream os;
            os << thread_id;
            printf("thread id: %s, idx: %d, %d\n", os.str().c_str(), (int) i, *p);
        });
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

    auto find = CreateBatchFindClassUsingStrings(fbb, 0, 0, false,0, fbb.CreateVector(matchers));
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

    auto find = CreateBatchFindMethodUsingStrings(fbb, 0, 0, false, 0, 0, fbb.CreateVector(matchers));
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

int DexKitFindClassUsingStrings(dexkit::DexKit &dexkit) {
    printf("-----------DexKitFindClassUsingStrings Start-----------\n");
    flatbuffers::FlatBufferBuilder fbb;
    auto find = CreateFindClass(
            fbb, 0, 0, false, 0,
            CreateClassMatcher(
                    fbb,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    fbb.CreateVector(std::vector<flatbuffers::Offset<StringMatcher>>{
                            CreateStringMatcher(
                                    fbb,
                                    fbb.CreateString("52b7f2"),
                                    StringMatchType::Equal,
                                    false
                            ),
                            CreateStringMatcher(
                                    fbb,
                                    fbb.CreateString("qimei"),
                                    StringMatchType::Contains,
                                    false
                            ),
                    })
            )
    );
    fbb.Finish(find);

    auto buf = fbb.GetBufferPointer();
    auto query = From<FindClass>(buf);
    printf("build query: %p, size: %d\n", query, fbb.GetSize());
    auto builder = dexkit.FindClass(query);
    auto buffer = builder->GetBufferPointer();
    auto size = builder->GetSize();
    printf("buffer size: %d\n", size);

    auto result = From<ClassMetaArrayHolder>(buffer);
    if (result->classes()) {
        printf("result->classes()->size() = %d\n", result->classes()->size());
        for (int i = 0; i < result->classes()->size(); ++i) {
            auto item = result->classes()->Get(i);
            printf("dex: %02d, idx: %d, class: %s, fields_size: %d\n",
                   item->dex_id(), item->id(), item->dex_descriptor()->string_view().data(), item->fields()->size());
        }
    }
    return 0;
}

int DexKitFindClassTest(dexkit::DexKit &dexkit) {
    printf("-----------DexKitFindClassTest Start-----------\n");
    flatbuffers::FlatBufferBuilder fbb;
    auto find = CreateFindClass(
            fbb, 0, 0, false, 0,
            CreateClassMatcher(
                    fbb,
                    0,
                    0,
                    0,
                    CreateClassMatcher(
                            fbb,
                            0,
                            CreateStringMatcher(
                                    fbb,
                                    fbb.CreateString("JceStruct"),
                                    StringMatchType::Contains,
                                    false
                            )
                    ),
                    0,
                    0,
                    CreateFieldsMatcher(
                            fbb,
                            fbb.CreateVector(std::vector<flatbuffers::Offset<FieldMatcher>>{
                                    CreateFieldMatcher(
                                            fbb,
                                            CreateStringMatcher(
                                                    fbb,
                                                    fbb.CreateString("cAge"),
                                                    StringMatchType::Equal,
                                                    false
                                            )
                                    ),
                                    CreateFieldMatcher(
                                            fbb,
                                            CreateStringMatcher(
                                                    fbb,
                                                    fbb.CreateString("Sex"),
                                                    StringMatchType::Contains,
                                                    false
                                            )
                                    ),
                                    CreateFieldMatcher(
                                            fbb,
                                            CreateStringMatcher(
                                                    fbb,
                                                    fbb.CreateString("Black"),
                                                    StringMatchType::Contains,
                                                    false
                                            )
                                    ),
                                    CreateFieldMatcher(
                                            fbb,
                                            CreateStringMatcher(
                                                    fbb,
                                                    fbb.CreateString("Nick"),
                                                    StringMatchType::Contains,
                                                    false
                                            )
                                    ),
                                    CreateFieldMatcher(
                                            fbb,
                                            CreateStringMatcher(
                                                    fbb,
                                                    fbb.CreateString("Face"),
                                                    StringMatchType::Contains,
                                                    false
                                            )
                                    )
                            }),
                            MatchType::Contains,
                            CreateIntRange(fbb, 3, 5)
                    ),
                    CreateMethodsMatcher(
                            fbb,
                            fbb.CreateVector(std::vector<flatbuffers::Offset<MethodMatcher>>{
                                    CreateMethodMatcher(
                                            fbb,
                                            CreateStringMatcher(
                                                    fbb,
                                                    fbb.CreateString("read"),
                                                    StringMatchType::StartWith,
                                                    false
                                            )
                                    )
                            })
                    )
            )
    );
    fbb.Finish(find);

    auto buf = fbb.GetBufferPointer();
    auto query = From<FindClass>(buf);
    printf("build query: %p, size: %d\n", query, fbb.GetSize());
    auto builder = dexkit.FindClass(query);
    auto buffer = builder->GetBufferPointer();
    auto size = builder->GetSize();
    printf("buffer size: %d\n", size);

    auto result = From<ClassMetaArrayHolder>(buffer);
    if (result->classes()) {
        printf("result->classes()->size() = %d\n", result->classes()->size());
        for (int i = 0; i < result->classes()->size(); ++i) {
            auto item = result->classes()->Get(i);
            printf("dex: %02d, idx: %d, class: %s, fields_size: %d\n",
                   item->dex_id(), item->id(), item->dex_descriptor()->string_view().data(), item->fields()->size());
        }
    }
    return 0;
}


int DexKitFindClassUsingAnnotationTest(dexkit::DexKit &dexkit) {
    printf("-----------DexKitFindClassUsingAnnotationTest Start-----------\n");

    flatbuffers::FlatBufferBuilder fbb;
    auto find = CreateFindClass(
            fbb, 0, 0, false, 0,
            CreateClassMatcher(
                    fbb,
                    0,
                    0,
                    0,
                    0,
                    0,
                    CreateAnnotationsMatcher(
                            fbb,
                            fbb.CreateVector(std::vector<flatbuffers::Offset<AnnotationMatcher>>{
                                    CreateAnnotationMatcher(
                                            fbb,
                                            0,
                                            0,
                                            RetentionPolicyType::Any,
                                            0,
                                            CreateAnnotationElementsMatcher(
                                                    fbb,
                                                    fbb.CreateVector(std::vector<flatbuffers::Offset<AnnotationElementMatcher>>{
                                                            CreateAnnotationElementMatcher(
                                                                    fbb,
                                                                    CreateStringMatcher(
                                                                            fbb,
                                                                            fbb.CreateString("value"),
                                                                            StringMatchType::Equal
                                                                    ),
                                                                    AnnotationEncodeValueMatcher::AnnotationEncodeArrayMatcher,
                                                                    CreateAnnotationEncodeArrayMatcher(
                                                                            fbb,
                                                                            fbb.CreateVector(std::vector<AnnotationEncodeValueMatcher>{
                                                                                    AnnotationEncodeValueMatcher::ClassMatcher
                                                                            }),
                                                                            fbb.CreateVector(std::vector<flatbuffers::Offset<void>>{
                                                                                    CreateClassMatcher(
                                                                                            fbb,
                                                                                            0,
                                                                                            CreateStringMatcher(
                                                                                                    fbb,
                                                                                                    fbb.CreateString("com/tencent/mobileqq/antiphing/a$c"),
                                                                                                    StringMatchType::Equal
                                                                                            )
                                                                                    ).Union()
                                                                            })
                                                                    ).Union()
                                                            )
                                                    }),
                                                    MatchType::Contains,
                                                    CreateIntRange(fbb, 1, 1)
                                            )
                                    ),
                            })
                    )
            )
    );
    fbb.Finish(find);

    auto buf = fbb.GetBufferPointer();
    auto query = From<FindClass>(buf);
    printf("build query: %p, size: %d\n", query, fbb.GetSize());
    auto builder = dexkit.FindClass(query);
    auto buffer = builder->GetBufferPointer();
    auto size = builder->GetSize();
    printf("buffer size: %d\n", size);

    auto result = From<ClassMetaArrayHolder>(buffer);
    if (result->classes()) {
        printf("result->classes()->size() = %d\n", result->classes()->size());
        for (int i = 0; i < result->classes()->size(); ++i) {
            auto item = result->classes()->Get(i);
            printf("dex: %02d, idx: %d, class: %s, fields_size: %d\n",
                   item->dex_id(), item->id(), item->dex_descriptor()->string_view().data(), item->fields()->size());
        }
    }
    return 0;
}

int DexKitFindMethodInvoking(dexkit::DexKit &dexkit) {
    printf("-----------DexKitFindMethodInvoking Start-----------\n");

    flatbuffers::FlatBufferBuilder fbb;
    auto find = CreateFindMethod(
            fbb, 0, 0, false, 0, 0,
            CreateMethodMatcher(
                    fbb,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    CreateMethodsMatcher(
                            fbb,
                            fbb.CreateVector(std::vector<flatbuffers::Offset<MethodMatcher>>{
                                    CreateMethodMatcher(
                                            fbb,
                                            CreateStringMatcher(
                                                    fbb,
                                                    fbb.CreateString("<init>"),
                                                    StringMatchType::Equal,
                                                    false
                                            ),
                                            0,
                                            CreateClassMatcher(
                                                    fbb,
                                                    0,
                                                    CreateStringMatcher(
                                                            fbb,
                                                            fbb.CreateString("com/tencent/mobileqq/app/QQAppInterface"),
                                                            StringMatchType::Equal,
                                                            false
                                                    )
                                            )
                                    )
                            })
                    ),
                    0
            )
    );
    fbb.Finish(find);

    auto buf = fbb.GetBufferPointer();
    auto query = From<FindMethod>(buf);
    printf("build query: %p, size: %d\n", query, fbb.GetSize());
    auto builder = dexkit.FindMethod(query);
    auto buffer = builder->GetBufferPointer();
    auto size = builder->GetSize();
    printf("buffer size: %d\n", size);

    auto result = From<MethodMetaArrayHolder>(buffer);
    if (result->methods()) {
        printf("result->classes()->size() = %d\n", result->methods()->size());
        for (int i = 0; i < result->methods()->size(); ++i) {
            auto item = result->methods()->Get(i);
            printf("dex: %02d, idx: %d, descriptor: %s\n",
                   item->dex_id(), item->id(), item->dex_descriptor()->string_view().data());
        }
    }
    return 0;
}

int DexKitFindMethodCaller(dexkit::DexKit &dexkit) {
    printf("-----------DexKitFindMethodCaller Start-----------\n");

    flatbuffers::FlatBufferBuilder fbb;
    auto find = CreateFindMethod(
            fbb, 0, 0, false, 0, 0,
            CreateMethodMatcher(
                    fbb,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    CreateMethodsMatcher(
                            fbb,
                            fbb.CreateVector(std::vector<flatbuffers::Offset<MethodMatcher>>{
                                    CreateMethodMatcher(
                                            fbb,
                                            0,
                                            0,
                                            CreateClassMatcher(
                                                    fbb,
                                                    0,
                                                    CreateStringMatcher(
                                                            fbb,
                                                            fbb.CreateString("com/tencent/mobileqq/guild/setting/msgnotify/f"),
                                                            StringMatchType::Equal,
                                                            false
                                                    )
                                            )
                                    )
                            })
                    )
            )
    );
    fbb.Finish(find);

    auto buf = fbb.GetBufferPointer();
    auto query = From<FindMethod>(buf);
    printf("build query: %p, size: %d\n", query, fbb.GetSize());
    auto builder = dexkit.FindMethod(query);
    auto buffer = builder->GetBufferPointer();
    auto size = builder->GetSize();
    printf("buffer size: %d\n", size);

    auto result = From<MethodMetaArrayHolder>(buffer);
    if (result->methods()) {
        printf("result->classes()->size() = %d\n", result->methods()->size());
        for (int i = 0; i < result->methods()->size(); ++i) {
            auto item = result->methods()->Get(i);
            printf("dex: %02d, idx: %d, descriptor: %s\n",
                   item->dex_id(), item->id(), item->dex_descriptor()->string_view().data());
        }
    }
    return 0;
}

int DexKitPackageTest(dexkit::DexKit &dexkit) {
    printf("-----------DexKitPackageTest Start-----------\n");


    flatbuffers::FlatBufferBuilder fbb;
    auto find = CreateFindMethod(
            fbb,
            fbb.CreateVectorOfStrings({"Org.luckypray.dexkit.demo"}),
            fbb.CreateVectorOfStrings({"org.luckypray.dexkit.demo.annotations"}),
            true
    );
    fbb.Finish(find);

    auto buf = fbb.GetBufferPointer();
    auto query = From<FindMethod>(buf);
    printf("build query: %p, size: %d\n", query, fbb.GetSize());
    auto builder = dexkit.FindMethod(query);
    auto buffer = builder->GetBufferPointer();
    auto size = builder->GetSize();
    printf("buffer size: %d\n", size);

    auto result = From<MethodMetaArrayHolder>(buffer);
    if (result->methods()) {
        printf("result->methods()->size() = %d\n", result->methods()->size());
        for (int i = 0; i < result->methods()->size(); ++i) {
            auto item = result->methods()->Get(i);
            printf("dex: %02d, idx: %d, methods: %s\n",
                   item->dex_id(), item->id(), item->dex_descriptor()->string_view().data());
        }
    }
    return 0;
}


int main() {
    auto now = std::chrono::system_clock::now();
    auto now_ms = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch());
    auto dexkit = dexkit::DexKit("../apks/demo.apk");
//    dexkit.SetThreadNum(1);
    auto now1 = std::chrono::system_clock::now();
    auto now_ms1 = std::chrono::duration_cast<std::chrono::milliseconds>(now1.time_since_epoch());
    std::cout << "unzip and init full cache used time: " << now_ms1.count() - now_ms.count() << " ms" << std::endl;

    printf("DexCount: %d\n", dexkit.GetDexNum());
//    SharedPtrVoidCast(dexkit);
//    ThreadVariableTest();
//    KmpTest();
//    ACTrieTest();
//    FlatBufferTest();
//    DexKitBatchFindClassTest(dexkit);
//    DexKitBatchFindMethodTest(dexkit);
//    DexKitFindClassUsingStrings(dexkit);
//    DexKitFindClassTest(dexkit);
//    DexKitFindMethodInvoking(dexkit);
//    DexKitFindMethodCaller(dexkit);
//    DexKitFindClassUsingAnnotationTest(dexkit);
    DexKitPackageTest(dexkit);

    auto now2 = std::chrono::system_clock::now();
    auto now_ms2 = std::chrono::duration_cast<std::chrono::milliseconds>(now2.time_since_epoch());
    std::cout << "find used time: " << now_ms2.count() - now_ms1.count() << " ms" << std::endl;
    return 0;
}
