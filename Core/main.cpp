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

int ThreadSharedVariableTest() {
    {
        ThreadPool pool(32);
        for (int i = 0; i < 10000; ++i) {
            pool.enqueue([i]() {
                auto p = ThreadVariable::SetSharedVariable<int>(i, i);
                printf("idx: %d, %d\n", (int) i, *p);
            });
        }
    }
    for (int i = 0; i < 10000; ++i) {
        auto p = ThreadVariable::GetSharedVariable<int>(i);
        assert(p);
        assert(*p == i);
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
            fbb, 0, 0, false, 0, false,
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
            fbb, 0, 0, false, 0, false,
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

int DexKitFindClassFieldsTest(dexkit::DexKit &dexkit) {
    printf("-----------DexKitFindClassFieldsTest Start-----------\n");
    flatbuffers::FlatBufferBuilder fbb;
    auto find = CreateFindClass(
            fbb, 0, 0, false, 0, true,
            CreateClassMatcher(
                    fbb,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    CreateFieldsMatcher(
                            fbb,
                            fbb.CreateVector(std::vector<flatbuffers::Offset<FieldMatcher>>{
                                    CreateFieldMatcher(
                                            fbb,
                                            0,
                                            0,
                                            0,
                                            CreateClassMatcher(
                                                    fbb,
                                                    0,
                                                    CreateStringMatcher(
                                                            fbb,
                                                            fbb.CreateString("java.util.concurrent.ConcurrentHashMap"),
                                                            StringMatchType::Equal,
                                                            false
                                                    )
                                            )
                                    ),
                                    CreateFieldMatcher(
                                            fbb,
                                            0,
                                            0,
                                            0,
                                            CreateClassMatcher(
                                                    fbb,
                                                    0,
                                                    CreateStringMatcher(
                                                            fbb,
                                                            fbb.CreateString("android.content.SharedPreferences"),
                                                            StringMatchType::Equal,
                                                            false
                                                    )
                                            )
                                    ),
                                    CreateFieldMatcher(
                                            fbb,
                                            0,
                                            0,
                                            0,
                                            CreateClassMatcher(
                                                    fbb,
                                                    0,
                                                    CreateStringMatcher(
                                                            fbb,
                                                            fbb.CreateString("long"),
                                                            StringMatchType::Equal,
                                                            false
                                                    )
                                            )
                                    )
                            }),
                            MatchType::Contains
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
            fbb, 0, 0, false, 0, false,
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

int DexKitFindMethodUsingAnnotationTest(dexkit::DexKit &dexkit) {
    printf("-----------DexKitFindMethodUsingAnnotationTest Start-----------\n");

    flatbuffers::FlatBufferBuilder fbb;
    auto find = CreateFindMethod(
            fbb, 0, 0, false, 0, 0, false,
            CreateMethodMatcher(
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
                                            dexkit::schema::RetentionPolicyType::Any,
                                            0,
                                            fbb.CreateVector(std::vector<flatbuffers::Offset<StringMatcher>>{
                                                    CreateStringMatcher(
                                                            fbb,
                                                            fbb.CreateString("Ljava/lang/Boolean;"),
                                                            StringMatchType::Contains
                                                    ),
                                                    CreateStringMatcher(
                                                            fbb,
                                                            fbb.CreateString("("),
                                                            StringMatchType::Equal
                                                    ),
                                                    CreateStringMatcher(
                                                            fbb,
                                                            fbb.CreateString("Lkotlin/jvm/functions/Function0<"),
                                                            StringMatchType::Contains
                                                    ),
                                                    CreateStringMatcher(
                                                            fbb,
                                                            fbb.CreateString(">;)V"),
                                                            StringMatchType::Contains
                                                    ),
                                            })
                                    ),
                            })
                    )
            )
    );
    fbb.Finish(find);

//    auto buf = fbb.GetBufferPointer();
    auto buf = new int8_t [] {24, 0, 0, 0, 0, 0, 18, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 18, 0, 0, 0, 24, 0, 0, 0, 20, 0, 0, 0, 0, 0, 14, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 14, 0, 0, 0, 8, 0, 0, 0, 4, 0, 0, 0, -70, -1, -1, -1, 4, 0, 0, 0, 5, 0, 0, 0, -68, 0, 0, 0, -84, 0, 0, 0, -96, 0, 0, 0, -108, 0, 0, 0, 8, 0, 0, 0, 4, 0, 0, 0, -34, -1, -1, -1, 8, 0, 0, 0, 4, 0, 0, 0, -22, -1, -1, -1, 4, 0, 0, 0, 1, 0, 0, 0, 16, 0, 0, 0, 12, 0, 0, 0, 0, 0, 6, 0, 8, 0, 4, 0, 6, 0, 0, 0, 16, 0, 0, 0, 12, 0, 0, 0, 8, 0, 8, 0, 0, 0, 4, 0, 8, 0, 0, 0, 16, 0, 0, 0, 12, 0, 0, 0, 8, 0, 12, 0, 8, 0, 7, 0, 8, 0, 0, 0, 0, 0, 0, 4, 4, 0, 0, 0, 33, 0, 0, 0, 111, 114, 103, 46, 106, 101, 116, 98, 114, 97, 105, 110, 115, 46, 97, 110, 110, 111, 116, 97, 116, 105, 111, 110, 115, 46, 78, 111, 116, 78, 117, 108, 108, 0, 0, 0, 4, 0, 0, 0, -24, -1, -1, -1, 4, 0, 0, 0, -16, -1, -1, -1, 4, 0, 0, 0, -8, -1, -1, -1, 8, 0, 0, 0, 4, 0, 4, 0, 4, 0, 0, 0};
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
            printf("dex: %02d, idx: %d, method: %s\n",
                   item->dex_id(), item->id(), item->dex_descriptor()->string_view().data());
        }
    }
    return 0;
}

int DexKitFindMethodInvoking(dexkit::DexKit &dexkit) {
    printf("-----------DexKitFindMethodInvoking Start-----------\n");

    flatbuffers::FlatBufferBuilder fbb;
    auto find = CreateFindMethod(
            fbb, 0, 0, false, 0, 0, false,
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
            fbb, 0, 0, false, 0, 0, false,
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

int DexKitFindParameterTypeArray(dexkit::DexKit &dexkit) {
    printf("-----------DexKitFindParameterTypeArray Start-----------\n");

    flatbuffers::FlatBufferBuilder fbb;
    auto find = CreateFindMethod(
            fbb, 0, 0, false, 0, 0, false,
            CreateMethodMatcher(
                    fbb,
                    0,
                    0,
                    CreateClassMatcher(
                            fbb,
                            0,
                            CreateStringMatcher(
                                    fbb,
                                    fbb.CreateString("com/tencent/mobileqq/pb/MessageMicro"),
                                    StringMatchType::Equal,
                                    false
                            )
                    ),
                    0,
                    CreateParametersMatcher(
                            fbb,
                            fbb.CreateVector(std::vector<flatbuffers::Offset<ParameterMatcher>>{
                                    CreateParameterMatcher(fbb),
                                    CreateParameterMatcher(
                                            fbb,
                                            0,
                                            CreateClassMatcher(
                                                    fbb,
                                                    0,
                                                    CreateStringMatcher(
                                                            fbb,
                                                            fbb.CreateString("java.lang.String[]"),
                                                            StringMatchType::Equal
                                                    )
                                            )
                                    ),
                                    CreateParameterMatcher(fbb),
                                    CreateParameterMatcher(fbb)
                            })
                    ),
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
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

int DexKitFindFieldTest(dexkit::DexKit &dexkit) {
    printf("-----------DexKitTest Start-----------\n");

    flatbuffers::FlatBufferBuilder fbb;
    auto find = CreateFindField(
            fbb, 0, 0, false, 0, 0, false,
            CreateFieldMatcher(
                    fbb,
                    0,
                    0,
                    CreateClassMatcher(
                            fbb,
                            0,
                            CreateStringMatcher(
                                    fbb,
                                    fbb.CreateString("at.t2"),
                                    StringMatchType::Equal,
                                    false
                            )
                    ),
                    CreateClassMatcher(
                            fbb,
                            0,
                            CreateStringMatcher(
                                    fbb,
                                    fbb.CreateString("boolean"),
                                    StringMatchType::Equal,
                                    false
                            )
                    ),
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
                                                            fbb.CreateString("at.t2"),
                                                            StringMatchType::Equal
                                                    )
                                            ),
                                            CreateClassMatcher(
                                                    fbb,
                                                    0,
                                                    CreateStringMatcher(
                                                            fbb,
                                                            fbb.CreateString("boolean"),
                                                            StringMatchType::Equal
                                                    )
                                            ),
                                            CreateParametersMatcher(
                                                    fbb,
                                                    0,
                                                    CreateIntRange(fbb, 0, 0)
                                            ),
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
                                                                            CreateStringMatcher(
                                                                                    fbb,
                                                                                    fbb.CreateString(
                                                                                            "AlphaSettingDialogUtils.kt"),
                                                                                    StringMatchType::Equal
                                                                            )
                                                                    ),
                                                                    CreateClassMatcher(
                                                                            fbb,
                                                                            0,
                                                                            CreateStringMatcher(
                                                                                    fbb,
                                                                                    fbb.CreateString(
                                                                                            "android.graphics.drawable.Drawable"),
                                                                                    StringMatchType::Equal
                                                                            )
                                                                    ),
                                                                    CreateParametersMatcher(
                                                                            fbb,
                                                                            fbb.CreateVector(std::vector<flatbuffers::Offset<ParameterMatcher>>{
                                                                                    CreateParameterMatcher(
                                                                                            fbb,
                                                                                            0,
                                                                                            CreateClassMatcher(
                                                                                                    fbb,
                                                                                                    0,
                                                                                                    CreateStringMatcher(
                                                                                                            fbb,
                                                                                                            fbb.CreateString(
                                                                                                                    "android.content.Context"),
                                                                                                            StringMatchType::Equal
                                                                                                    )
                                                                                            )
                                                                                    )
                                                                            })
                                                                    )
                                                            )
                                                    })
                                            )
                                    )
                            })
                    )
            )
    );
    fbb.Finish(find);

    auto buf = fbb.GetBufferPointer();
    auto query = From<FindField>(buf);
    printf("build query: %p, size: %d\n", query, fbb.GetSize());
    auto builder = dexkit.FindField(query);
    auto buffer = builder->GetBufferPointer();
    auto size = builder->GetSize();
    printf("buffer size: %d\n", size);

    auto result = From<FieldMetaArrayHolder>(buffer);
    if (result->fields()) {
        printf("result->classes()->size() = %d\n", result->fields()->size());
        for (int i = 0; i < result->fields()->size(); ++i) {
            auto item = result->fields()->Get(i);
            printf("dex: %02d, idx: %d, descriptor: %s\n",
                   item->dex_id(), item->id(), item->dex_descriptor()->string_view().data());
        }
    }
    return 0;
}

int DexKitFindMethodUsingNumbers(dexkit::DexKit &dexkit) {
    printf("-----------DexKitFindMethodUsingNumbers Start-----------\n");

    flatbuffers::FlatBufferBuilder fbb;
    auto find = CreateFindMethod(
            fbb, 0, 0, false, 0, 0, false,
            CreateMethodMatcher(
                    fbb,
                    0,
                    0,
                    CreateClassMatcher(
                            fbb,
                            0,
                            CreateStringMatcher(
                                    fbb,
                                    fbb.CreateString("org.luckypray.dexkit.demo.PlayActivity"),
                                    StringMatchType::Equal,
                                    false
                            )
                    ),
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    fbb.CreateVector(std::vector<Number>{
                            Number::EncodeValueInt,
                            Number::EncodeValueLong,
                            Number::EncodeValueFloat,
                            Number::EncodeValueDouble,
                            Number::EncodeValueLong
                    }),
                    fbb.CreateVector(std::vector<flatbuffers::Offset<void>>{
                            CreateEncodeValueInt(fbb, 0).Union(),
                            CreateEncodeValueLong(fbb, -1).Union(),
                            CreateEncodeValueFloat(fbb, 0.01).Union(),
                            CreateEncodeValueDouble(fbb, 0.987f).Union(),
                            CreateEncodeValueLong(fbb, 114514).Union()
                    })
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

int DexKitFindDyClassTest(dexkit::DexKit &dexkit) {
    printf("-----------DexKitFindDyClassTest Start-----------\n");
    flatbuffers::FlatBufferBuilder fbb;
    auto find = CreateFindClass(
            fbb, 0, 0, false, 0, false,
            CreateClassMatcher(
                    fbb,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    CreateFieldsMatcher(
                            fbb,
                            fbb.CreateVector(std::vector<flatbuffers::Offset<FieldMatcher>>{
                                    CreateFieldMatcher(
                                            fbb,
                                            0,
                                            0,
                                            0,
                                            CreateClassMatcher(
                                                    fbb,
                                                    0,
                                                    CreateStringMatcher(
                                                            fbb,
                                                            fbb.CreateString("com.ss.android.ugc.aweme.feed.ui.seekbar.CustomizedUISeekBar"),
                                                            StringMatchType::Equal,
                                                            false
                                                    )
                                            )
                                    )
                            })
                    ),
                    CreateMethodsMatcher(
                            fbb,
                            fbb.CreateVector(std::vector<flatbuffers::Offset<MethodMatcher>>{
                                    CreateMethodMatcher(
                                            fbb,
                                            CreateStringMatcher(
                                                    fbb,
                                                    fbb.CreateString("getMOriginView"),
                                                    StringMatchType::Equal,
                                                    false
                                            ),
                                            0,
                                            0,
                                            CreateClassMatcher(
                                                    fbb,
                                                    0,
                                                    CreateStringMatcher(
                                                            fbb,
                                                            fbb.CreateString("android.view.View"),
                                                            StringMatchType::Equal,
                                                            false
                                                    )
                                            )
                                    ),
                                    CreateMethodMatcher(
                                            fbb,
                                            CreateStringMatcher(
                                                    fbb,
                                                    fbb.CreateString("handleMsg"),
                                                    StringMatchType::Equal,
                                                    false
                                            ),
                                            0,
                                            0,
                                            0,
                                            CreateParametersMatcher(
                                                    fbb,
                                                    fbb.CreateVector(std::vector<flatbuffers::Offset<ParameterMatcher>>{
                                                            CreateParameterMatcher(
                                                                    fbb,
                                                                    0,
                                                                    CreateClassMatcher(
                                                                            fbb,
                                                                            0,
                                                                            CreateStringMatcher(
                                                                                    fbb,
                                                                                    fbb.CreateString("android.os.Message"),
                                                                                    StringMatchType::Equal
                                                                            )
                                                                    )
                                                            )
                                                    })
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

int DexKitFindDyClassUsingStrings(dexkit::DexKit &dexkit) {
    printf("-----------DexKitFindDyClassUsingStrings Start-----------\n");
    flatbuffers::FlatBufferBuilder fbb;
    auto find = CreateFindClass(
            fbb, 0, 0, false, 0, false,
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
                                    fbb.CreateString("a1128.b7947"),
                                    StringMatchType::Contains,
                                    false
                            ),
                            CreateStringMatcher(
                                    fbb,
                                    fbb.CreateString("com/ss/android/ugc/aweme/detail/ui/DetailPageFragment"),
                                    StringMatchType::Contains,
                                    false
                            ),
                            CreateStringMatcher(
                                    fbb,
                                    fbb.CreateString("DetailActOtherNitaView"),
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

int main() {
    auto now = std::chrono::system_clock::now();
    auto now_ms = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch());
    auto dexkit = dexkit::DexKit("../apks/QQ_8.9.70_clone.apk");
//    dexkit.SetThreadNum(1);
    auto now1 = std::chrono::system_clock::now();
    auto now_ms1 = std::chrono::duration_cast<std::chrono::milliseconds>(now1.time_since_epoch());
    std::cout << "unzip and init base cache used time: " << now_ms1.count() - now_ms.count() << " ms" << std::endl;

    printf("DexCount: %d\n", dexkit.GetDexNum());
//    SharedPtrVoidCast(dexkit);
//    ThreadVariableTest();
//    ThreadSharedVariableTest();
//    KmpTest();
//    ACTrieTest();
//    FlatBufferTest();
//    DexKitBatchFindClassTest(dexkit);
//    DexKitBatchFindMethodTest(dexkit);
//    DexKitFindClassUsingStrings(dexkit);
//    DexKitFindClassTest(dexkit);
//    DexKitFindClassFieldsTest(dexkit);
//    DexKitFindMethodInvoking(dexkit);
//    DexKitFindMethodCaller(dexkit);
//    DexKitFindClassUsingAnnotationTest(dexkit);
    DexKitFindMethodUsingAnnotationTest(dexkit);
//    DexKitPackageTest(dexkit);
//    DexKitFindParameterTypeArray(dexkit);
//    DexKitFindFieldTest(dexkit);
//    DexKitFindMethodUsingNumbers(dexkit);
//    DexKitFindDyClassTest(dexkit);
//    DexKitFindDyClassUsingStrings(dexkit);
    dexkit.GetParameterAnnotations(17179890467);

    auto now2 = std::chrono::system_clock::now();
    auto now_ms2 = std::chrono::duration_cast<std::chrono::milliseconds>(now2.time_since_epoch());
    std::cout << "find used time: " << now_ms2.count() - now_ms1.count() << " ms" << std::endl;
    return 0;
}
