#pragma once

#include <string_view>
#include <vector>
#include <thread>
#include <list>
#include <set>
#include <unordered_map>
#include <optional>
#include "slicer/reader.h"
#include "file_helper.h"

namespace dexkit {

// null_param is used to match any param
const static std::optional<std::vector<std::string>> null_param = std::nullopt;
const static std::vector<std::string> empty_param;

// init cached flag
constexpr dex::u4 fHeader = 0x0001;
constexpr dex::u4 fString = 0x0002;
constexpr dex::u4 fType = 0x0004;
constexpr dex::u4 fProto = 0x0008;
constexpr dex::u4 fField = 0x0010;
constexpr dex::u4 fMethod = 0x0020;
constexpr dex::u4 fAnnotation = 0x0040;
constexpr dex::u4 fOpCodeSeq = 0x1000;
constexpr dex::u4 fDefault = fHeader | fString | fType | fProto | fMethod;

// used field flag
constexpr dex::u4 fGetting = 0x1;
constexpr dex::u4 fSetting = 0x2;
constexpr dex::u4 fUsing = fGetting | fSetting;

// match type
// mFull           : full string match, eg.
//     full_match(search = "abc", target = "abc") = true
//     full_match(search = "abc", target = "abcd") = false
constexpr dex::u4 mFull = 0;
// mContains       : contains string match, eg.
//     contains_match(search = "abc", target = "abcd") = true
//     contains_match(search = "abc", target = "abc") = true
//     contains_match(search = "abc", target = "ab") = false
constexpr dex::u4 mContains = 1;
// mSimilarRegex   : similar regex matches, only support: '^', '$' eg.
//     similar_regex_match(search = "abc", target = "abc") == true
//     similar_regex_match(search = "^abc", target = "abc") == true
//     similar_regex_match(search = "abc$", target = "bc") == true
//     similar_regex_match(search = "^abc$", target = "abc") == true
//     similar_regex_match(search = "^abc$", target = "abcd") == false
constexpr dex::u4 mSimilarRegex = 2;

class DexKit {
public:

    explicit DexKit() = default;

    explicit DexKit(std::string_view apk_path, int unzip_thread_num = -1);

    void AddImages(std::vector<std::unique_ptr<MemMap>> dex_images);

    void AddPath(std::string_view apk_path, int unzip_thread_num = -1);

    ~DexKit() = default;

    void SetThreadNum(int num) {
        thread_num_ = num;
    }

    void ExportDexFile(std::string &out_dir);

    /**
     * @brief find used all matched keywords in class (all methods of this class)
     * @param location_map deobfuscation info map <br/>
     * key: unique identifier, eg: class name <br/>
     * value: used keywords <br/>
     * @param match_type mFull: full string match, mContains: contains string match, similar regex matches, only support: '^', '$' <br/>
     * <code>
     * similar_regex_match("^abc$", "abc") == true
     * similar_regex_match("^abc$", "abcd") == false
     * </code>
     * @param dex_priority if not empty, only search included dex ids. dex numbering starts from 0.
     * @return search result map <br/>
     * like: {"Lcom/tencent/mobileqq/troop/clockin/handler/TroopClockInHandler;" -> {"Lxadt;"}}
     * @note <a color="#E3170D">Try to avoid keyword duplication as this will invalidate your search. </a> <br/>
     * eg: keywords = {"word", "key_word"}, when matching the string "key_word", it matches "key_word" first,
     * then again "word", so "key_word" only marks "word". <br/>
     * While there are ways to deal with it, in the worst case there can be a performance gap of tens or even hundreds of times.
     * So I finally decided not to handle this situation. <br/>
     * but for the previous example, if keywords = {"^word$", "^key_word$"}, "key_word" matches "^key_word$" but not "^word$".
     * This avoids the problem to some extent
     */
    std::map<std::string, std::vector<std::string>>
    BatchFindClassesUsingStrings(std::map<std::string, std::set<std::string>> &location_map,
                                 int match_type,
                                 const std::string &find_package = "",
                                 const std::vector<size_t> &dex_priority = {});

    /**
     * @brief find used all matched keywords in method.
     * @param location_map deobfuscation info map <br/>
     * key: unique identifier, eg: class name <br/>
     * value: used keywords <br/>
     * @param match_type mFull: full string match, mContains: contains string match, similar regex matches, only support: '^', '$' <br/>
     * <code>
     * similar_regex_match("^abc$", "abc") == true
     * similar_regex_match("^abc$", "abcd") == false
     * </code>
     * @param dex_priority if not empty, only search included dex ids. dex numbering starts from 0.
     * @return like: {"Lcom/tencent/mobileqq/troop/clockin/handler/TroopClockInHandler;" -> {"Lxadt;->a()V"}}
     * @see BatchFindClassesUsingStrings()
     */
    std::map<std::string, std::vector<std::string>>
    BatchFindMethodsUsingStrings(std::map<std::string, std::set<std::string>> &location_map,
                                 int match_type,
                                 const std::string &find_package = "",
                                 const std::vector<size_t> &dex_priority = {});

    /**
     * @brief find caller for specified method.
     * @param method_descriptor if not empty, filled in [method_declare_class], [method_declare_name], [method_return_type], [method_param_types]
     * @param method_declare_class if empty, match any class;
     * @param method_declare_name if empty, match any method name;
     * @param method_return_type if empty, match any return type;
     * @param method_param_types if match any param size and type, used 'dexkit::null_param;' or '{}', <br/>
     * if match empty param, use 'dexkit::empty_param' or 'std::vector<std::string>()', <br/>
     * if it contains unknown types, please keep the empty string eg: {"I", "", "Ljava/lang/String;"}
     * @param caller_method_descriptor refer to [method_descriptor]
     * @param caller_method_declare_class refer to [method_declare_class]
     * @param caller_method_declare_name refer to [method_declare_name]
     * @param caller_method_return_type refer to [method_return_type]
     * @param caller_method_param_types refer to [method_param_types]
     * @param dex_priority if not empty, only search included dex ids. dex numbering starts from 0.
     * @return method descriptor
     */
    std::map<std::string, std::vector<std::string>>
    FindMethodCaller(const std::string &method_descriptor,
                     const std::string &method_declare_class,
                     const std::string &method_declare_name,
                     const std::string &method_return_type,
                     const std::optional<std::vector<std::string>> &method_param_types,
                     const std::string &caller_method_descriptor,
                     const std::string &caller_method_declare_class,
                     const std::string &caller_method_declare_name,
                     const std::string &caller_method_return_type,
                     const std::optional<std::vector<std::string>> &caller_method_param_types,
                     bool unique_result = true,
                     const std::string &source_file = "",
                     const std::string &find_package = "",
                     const std::vector<size_t> &dex_priority = {});

    /**
     * @brief find the called method
     * @param method_descriptor be called method descriptor <br/>
     * eg: "Lxadt;->a()V" <br>
     * after specifying method_descriptor, the subsequent parameters are automatically filled in [method_declare_class], [method_declare_name], [method_return_type], [method_param_types]
     * @param method_declare_class if empty, match any class; <br/>
     * eg: "Lme/teble/MainActivity$InnerClass;" or "me.teble.MainActivity$InnerClass"
     * @param method_declare_name if empty, match any method name; <br/>
     * eg: "onCreate"
     * @param method_return_type if empty, match any return type; <br/>
     * eg: "V" or "void"
     * @param method_param_types if match any param size and type, used 'dexkit::null_param;' or '{}', <br/>
     * if match empty param, use 'dexkit::empty_param' or 'std::vector<std::string>()', <br/>
     * if it contains unknown types, please keep the empty string eg: {"I", "", "Ljava/lang/String;"}
     * @param called_method_descriptor refer to [method_descriptor]
     * @param be_called_method_declare_class refer to [method_declare_class]
     * @param be_called_method_declare_name refer to [method_declare_name]
     * @param be_called_method_return_type refer to [method_return_type]
     * @param be_called_method_param_types refer to [method_param_types]
     * @param dex_priority if not empty, only search included dex ids. dex numbering starts from 0.
     * @return {"method descriptor" -> {"be caller method descriptor"}}
     */
    std::map<std::string, std::vector<std::string>>
    FindMethodInvoking(const std::string &method_descriptor,
                       const std::string &method_declare_class,
                       const std::string &method_declare_name,
                       const std::string &method_return_type,
                       const std::optional<std::vector<std::string>> &method_param_types,
                       const std::string &be_called_method_descriptor,
                       const std::string &be_called_method_declare_class,
                       const std::string &be_called_method_declare_name,
                       const std::string &be_called_method_return_type,
                       const std::optional<std::vector<std::string>> &be_called_method_param_types,
                       bool unique_result = true,
                       const std::string &source_file = "",
                       const std::string &find_package = "",
                       const std::vector<size_t> &dex_priority = {});

    /**
     * @brief find method getting specified field. for opcode: iget, iget-*, sget, sget-*, iput, iput-*, sput, sput-*
     * @param field_descriptor field descriptor, after specifying field_descriptor, the subsequent parameters are automatically filled in [field_declare_class], [field_declare_name], [field_type]
     * @param field_declare_class if empty, match any class;
     * @param field_declare_name if empty, match any field name;
     * @param field_type if empty, match any field type;
     * @param used_flags used flags, eg: 'fGetting' or 'fSetting' or 'fGetting | fSetting'
     * @param method_descriptor if not empty, filled in [method_declare_class], [method_declare_name], [method_return_type], [method_param_types]
     * @param caller_method_declare_class if empty, match any class;
     * @param caller_method_declare_name if empty, match any method name;
     * @param caller_method_return_type if empty, match any return type;
     * @param caller_method_param_types if match any param size and type, used 'dexkit::null_param;' or '{}', <br/>
     * if match empty param, use 'dexkit::empty_param' or 'std::vector<std::string>()', <br/>
     * if it contains unknown types, please keep the empty string eg: {"I", "", "Ljava/lang/String;"}
     * @param dex_priority if not empty, only search included dex ids. dex numbering starts from 0.
     * @return {"method descriptor" -> {"be used field descriptor"}}
     */
    std::map<std::string, std::vector<std::string>>
    FindMethodUsingField(const std::string &field_descriptor,
                         const std::string &field_declare_class,
                         const std::string &field_declare_name,
                         const std::string &field_type,
                         uint32_t used_flags,
                         const std::string &caller_method_descriptor,
                         const std::string &caller_method_declare_class,
                         const std::string &caller_method_declare_name,
                         const std::string &caller_method_return_type,
                         const std::optional<std::vector<std::string>> &caller_method_param_types,
                         bool unique_result = true,
                         const std::string &source_file = "",
                         const std::string &find_package = "",
                         const std::vector<size_t> &dex_priority = {});

    /**
     * @brief find method used utf8 string
     * @param using_utf8_string used utf8 string
     * @param match_type mFull: full string match, mContains: contains string match, similar regex matches, only support: '^', '$' <br/>
     * <code>
     * similar_regex_match("^abc$", "abc") == true
     * similar_regex_match("^abc$", "abcd") == false
     * </code>
     * @param method_declare_class if empty, match any class;
     * @param method_declare_name if empty, match any method name;
     * @param method_return_type if empty, match any return type;
     * @param method_param_types if match any param size and type, used 'dexkit::null_param;' or '{}', <br/>
     * if match empty param, use 'dexkit::empty_param' or 'std::vector<std::string>()', <br/>
     * if it contains unknown types, please keep the empty string eg: {"I", "", "Ljava/lang/String;"}
     * @param dex_priority if not empty, only search included dex ids. dex numbering starts from 0.
     * @return method descriptor
     */
    std::vector<std::string>
    FindMethodUsingString(const std::string &using_utf8_string,
                          int match_type,
                          const std::string &method_declare_class,
                          const std::string &method_declare_name,
                          const std::string &method_return_type,
                          const std::optional<std::vector<std::string>> &method_param_types,
                          bool unique_result = true,
                          const std::string &source_file = "",
                          const std::string &find_package = "",
                          const std::vector<size_t> &dex_priority = {});

    /**
     * @brief find using annotation's class
     * @param annotation_class annotation class
     * @param annotation_using_string if not empty, only search annotation using string
     * @param match_type mFull: full string match, mContains: contains string match, similar regex matches, only support: '^', '$' <br/>
     * <code>
     * similar_regex_match("^abc$", "abc") == true
     * similar_regex_match("^abc$", "abcd") == false
     * </code>
     * @param dex_priority if not empty, only search included dex ids. dex numbering starts from 0.
     * @return class descriptor
     */
    std::vector<std::string>
    FindClassUsingAnnotation(const std::string &annotation_class,
                             const std::string &annotation_using_string,
                             int match_type,
                             const std::string &source_file = "",
                             const std::string &find_package = "",
                             const std::vector<size_t> &dex_priority = {});

    /**
     * @brief find using annotation's field
     * @param annotation_class annotation class
     * @param annotation_using_string if not empty, only search annotation using string
     * @param match_type mFull: full string match, mContains: contains string match, similar regex matches, only support: '^', '$' <br/>
     * <code>
     * similar_regex_match("^abc$", "abc") == true
     * similar_regex_match("^abc$", "abcd") == false
     * </code>
     * @param field_declare_class if empty, match any class
     * @param field_declare_name if empty, match any field name
     * @param field_type if empty, match any field type
     * @param dex_priority if not empty, only search included dex ids. dex numbering starts from 0.
     * @return field descriptor
     */
    std::vector<std::string>
    FindFieldUsingAnnotation(const std::string &annotation_class,
                             const std::string &annotation_using_string,
                             int match_type,
                             const std::string &field_declare_class,
                             const std::string &field_declare_name,
                             const std::string &field_type,
                             const std::string &source_file = "",
                             const std::string &find_package = "",
                             const std::vector<size_t> &dex_priority = {});

    /**
     * @brief find using annotation's method
     * @param annotation_class annotation class
     * @param annotation_using_string if not empty, only search annotation using string
     * @param match_type mFull: full string match, mContains: contains string match, similar regex matches, only support: '^', '$' <br/>
     * <code>
     * similar_regex_match("^abc$", "abc") == true
     * similar_regex_match("^abc$", "abcd") == false
     * </code>
     * @param method_declare_class if empty, match any class
     * @param method_declare_name if empty, match any method name
     * @param method_return_type if empty, match any return type
     * @param method_param_types if match any param size and type, used 'dexkit::null_param;' or '{}', <br/>
     * @param dex_priority if not empty, only search included dex ids. dex numbering starts from 0.
     * @return method descriptor
     */
    std::vector<std::string>
    FindMethodUsingAnnotation(const std::string &annotation_class,
                              const std::string &annotation_using_string,
                              int match_type,
                              const std::string &method_declare_class,
                              const std::string &method_declare_name,
                              const std::string &method_return_type,
                              const std::optional<std::vector<std::string>> &method_param_types,
                              const std::string &source_file = "",
                              const std::string &find_package = "",
                              const std::vector<size_t> &dex_priority = {});

    /**
     * @brief find method by multiple conditions
     * @param method_descriptor method descriptor
     * @param method_declare_class if empty, match any class;
     * @param method_declare_name if empty, match any method name;
     * @param method_return_type if empty, match any return type;
     * @param method_param_types if match any param size and type, used 'dexkit::null_param;' or '{}', <br/>
     * if match empty param, use 'dexkit::empty_param' or 'std::vector<std::string>()', <br/>
     * if it contains unknown types, please keep the empty string eg: {"I", "", "Ljava/lang/String;"}
     * @param dex_priority if not empty, only search included dex ids. dex numbering starts from 0.
     * @return method descriptor
     */
    std::vector<std::string>
    FindMethod(const std::string &method_descriptor,
               const std::string &method_declare_class,
               const std::string &method_declare_name,
               const std::string &method_return_type,
               const std::optional<std::vector<std::string>> &method_param_types,
               const std::string &source_file = "",
               const std::string &find_package = "",
               const std::vector<size_t> &dex_priority = {});

    std::vector<std::string>
    FindClass(const std::string &source_file,
              const std::string &find_package = "",
              const std::vector<size_t> &dex_priority = {});

    /**
     * @brief find all direct subclasses of the specified class
     * @param parent_class direct parent class
     * @param dex_priority if not empty, only search included dex ids. dex numbering starts from 0.
     * @return
     */
    std::vector<std::string>
    FindSubClasses(const std::string &parent_class,
                   const std::vector<size_t> &dex_priority = {});

    /**
     * @brief find all method used opcode prefix sequence
     * @param op_prefix_seq opcode prefix sequence. eg. [0x70, 0x22, 0x70, 0x5b] -> ["invoke-direct", "new-instance", "invoke-direct", "iput-object"]
     * @param method_declare_class if empty, match any class;
     * @param method_declare_name if empty, match any method name;
     * @param method_return_type if empty, match any return type;
     * @param method_param_types if match any param size and type, used 'dexkit::null_param;' or '{}', <br/>
     * if match empty param, use 'dexkit::empty_param' or 'std::vector<std::string>()', <br/>
     * if it contains unknown types, please keep the empty string eg: {"I", "", "Ljava/lang/String;"}
     * @param dex_priority if not empty, only search included dex ids. dex numbering starts from 0.
     * @return method descriptor
     */
    std::vector<std::string>
    FindMethodUsingOpPrefixSeq(const std::vector<uint8_t> &op_prefix_seq,
                               const std::string &method_declare_class,
                               const std::string &method_declare_name,
                               const std::string &method_return_type,
                               const std::optional<std::vector<std::string>> &method_param_types,
                               const std::string &source_file = "",
                               const std::string &find_package = "",
                               const std::vector<size_t> &dex_priority = {});

    /**
     *
     * @param op_seq opcodes sequence. eg. [0x70, 0x22, 0x70, 0x5b] -> ["invoke-direct", "new-instance", "invoke-direct", "iput-object"]
     * @param method_declare_class if empty, match any class;
     * @param method_declare_name if empty, match any method name;
     * @param method_return_type if empty, match any return type;
     * @param method_param_types if match any param size and type, used 'dexkit::null_param;' or '{}', <br/>
     * if match empty param, use 'dexkit::empty_param' or 'std::vector<std::string>()', <br/>
     * if it contains unknown types, please keep the empty string eg: {"I", "", "Ljava/lang/String;"}
     * @param dex_priority if not empty, only search included dex ids. dex numbering starts from 0.
     * @return method descriptor
     */
    std::vector<std::string>
    FindMethodUsingOpCodeSeq(const std::vector<uint8_t> &op_seq,
                             const std::string &method_declare_class,
                             const std::string &method_declare_name,
                             const std::string &method_return_type,
                             const std::optional<std::vector<std::string>> &method_param_types,
                             const std::string &source_file = "",
                             const std::string &find_package = "",
                             const std::vector<size_t> &dex_priority = {});

    /**
     * @brief get method opcodes sequence
     * @param method_descriptor method descriptor
     * @param method_declare_class if empty, match any class;
     * @param method_declare_name if empty, match any method name;
     * @param method_return_type if empty, match any return type;
     * @param method_param_types if match any param size and type, used 'dexkit::null_param;' or '{}', <br/>
     * if match empty param, use 'dexkit::empty_param' or 'std::vector<std::string>()', <br/>
     * if it contains unknown types, please keep the empty string eg: {"I", "", "Ljava/lang/String;"}
     * @param dex_priority if not empty, only search included dex ids. dex numbering starts from 0.
     * @return {method_descriptor -> {opcodes sequence}}
     */
    std::map<std::string, std::vector<uint8_t>>
    GetMethodOpCodeSeq(const std::string &method_descriptor,
                       const std::string &method_declare_class,
                       const std::string &method_declare_name,
                       const std::string &method_return_type,
                       const std::optional<std::vector<std::string>> &method_param_types,
                       const std::string &source_file = "",
                       const std::string &find_package = "",
                       const std::vector<size_t> &dex_priority = {});

    uint32_t GetClassAccessFlags(const std::string &class_descriptor);

    uint32_t GetMethodAccessFlags(const std::string &method_descriptor);

    uint32_t GetFieldAccessFlags(const std::string &field_descriptor);

    size_t GetDexNum() {
        return dex_images_.size();
    }

private:
    std::vector<dex::u4> init_flags_;
    std::vector<std::unique_ptr<MemMap>> maps_;
    std::vector<std::pair<const void *, size_t>> dex_images_;

    uint32_t thread_num_ = std::thread::hardware_concurrency();
    std::vector<dex::Reader> readers_;

    std::vector<std::vector<std::string_view>> strings_;
    std::vector<std::vector<std::string_view>> type_names_;
    std::vector<std::unordered_map<std::string_view, dex::u4>> type_ids_map_;
    std::vector<std::vector<bool>> type_declared_flag_;
    std::vector<std::vector<std::string_view>> class_source_files_;
    std::vector<std::vector<uint32_t>> class_access_flags_;
    std::vector<std::vector<std::vector<uint32_t>>> class_method_ids_;
    std::vector<std::vector<uint32_t>> method_access_flags_;
    std::vector<std::vector<std::vector<uint32_t>>> class_field_ids_;
    std::vector<std::vector<uint32_t>> field_access_flags_;
    std::vector<std::vector<const dex::Code *>> method_codes_;
    std::vector<std::vector<const dex::TypeList *>> proto_type_list_;
    std::vector<std::vector<std::vector<uint8_t>>> method_opcode_seq_;
    std::vector<std::vector<bool>> method_opcode_seq_init_flag_;
    std::vector<std::vector<const ir::AnnotationsDirectory *>> class_annotations_;

    std::vector<std::string> cache_;

    void InitImages(int begin, int end);

    void InitCached(size_t dex_idx, dex::u4 flag);

    uint32_t FindTypeIdx(size_t dex_idx, std::string &type_desc);

    bool IsMethodMatch(size_t dex_idx, uint32_t method_idx, uint32_t decl_class,
                       const std::string &shorty_match, const std::string &method_declare_name,
                       uint32_t return_type, const std::vector<uint32_t> &param_types,
                       bool match_any_param);

    bool IsFieldMatch(size_t dex_idx, uint32_t field_idx, uint32_t decl_class,
                      const std::string &field_declare_name, uint32_t field_type);

    static inline bool NeedMethodMatch(const std::string &method_descriptor,
                                       const std::string &caller_method_declare_class,
                                       const std::string &caller_method_declare_name,
                                       const std::string &caller_method_return_type,
                                       const std::optional<std::vector<std::string>> &caller_method_param_types);

    void InitMethodOpCodeSeq(size_t dex_idx, uint32_t method_idx);

    bool IsInitMethodOpCodeSeq(size_t dex_idx, uint32_t method_idx);

    std::string GetMethodDescriptor(size_t dex_idx, uint32_t method_idx);

    std::string GetFieldDescriptor(size_t dex_idx, uint32_t field_idx);

    std::vector<size_t> GetDexPriority(const std::vector<size_t> &dex_priority);
};

}
