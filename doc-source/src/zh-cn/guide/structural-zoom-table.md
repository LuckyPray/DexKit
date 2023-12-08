# 结构速查表

> 这里介绍了 `DexKit` 查询的结构组成，匹配器对象可以进行递归嵌套，一切条件都是可以选的。

## 查询相关

### FindClass

| 字段名                | 类型                            | 说明                              |
|:-------------------|:------------------------------|:--------------------------------|
| searchPackages     | Collection&lt;String&gt;      | 搜索的包名列表                         |
| excludePackages    | Collection&lt;String&gt;      | 排除的包名列表                         |
| ignorePackagesCase | Boolean                       | 是否忽略包名大小写                       |
| searchIn           | Collection&lt;ClassData&gt;   | 在指定的类列表中搜索类                     |
| findFirst          | Boolean                       | 查询到第一个结果后立即返回结果，由于多线程执行，不保证结果唯一 |
| matcher            | [ClassMatcher](#classmatcher) | 匹配条件                            |

### FindField

| 字段名                | 类型                            | 说明                              |
|:-------------------|:------------------------------|:--------------------------------|
| searchPackages     | Collection&lt;String&gt;      | 搜索的包名列表                         |
| excludePackages    | Collection&lt;String&gt;      | 排除的包名列表                         |
| ignorePackagesCase | Boolean                       | 是否忽略包名大小写                       |
| searchInClasses    | Collection&lt;ClassData&gt;   | 在指定的类列表中搜索字段                    |
| searchInFields     | Collection&lt;FieldData&gt;   | 在指定的字段列表中搜索字段                   |
| findFirst          | Boolean                       | 查询到第一个结果后立即返回结果，由于多线程执行，不保证结果唯一 |
| matcher            | [FieldMatcher](#fieldmatcher) | 匹配条件                            |

### FindMethod

| 字段名                | 类型                              | 说明                              |
|:-------------------|:--------------------------------|:--------------------------------|
| searchPackages     | Collection&lt;String&gt;        | 搜索的包名列表                         |
| excludePackages    | Collection&lt;String&gt;        | 排除的包名列表                         |
| ignorePackagesCase | Boolean                         | 是否忽略包名大小写                       |
| searchInClasses    | Collection&lt;ClassData&gt;     | 在指定的类列表中搜索方法                    |
| searchInMethods    | Collection&lt;MethodData&gt;    | 在指定的方法列表中搜索方法                   |
| findFirst          | Boolean                         | 查询到第一个结果后立即返回结果，由于多线程执行，不保证结果唯一 |
| matcher            | [MethodMatcher](#methodmatcher) | 匹配条件                            |

### BatchFindClassUsingStrings

| 字段名                | 类型                                                            | 说明          |
|:-------------------|:--------------------------------------------------------------|:------------|
| searchPackages     | Collection&lt;String&gt;                                      | 搜索的包名列表     |
| excludePackages    | Collection&lt;String&gt;                                      | 排除的包名列表     |
| ignorePackagesCase | Boolean                                                       | 是否忽略包名大小写   |
| searchIn           | Collection&lt;ClassData&gt;                                   | 在指定的类列表中搜索类 |
| matchers           | Collection&lt;[StringMatchersGroup](#stringmatchersgroup)&gt; | 查询分组列表      |

### BatchFindMethodUsingStrings

| 字段名                | 类型                                                            | 说明            |
|:-------------------|:--------------------------------------------------------------|:--------------|
| searchPackages     | Collection&lt;String&gt;                                      | 搜索的包名列表       |
| excludePackages    | Collection&lt;String&gt;                                      | 排除的包名列表       |
| ignorePackagesCase | Boolean                                                       | 是否忽略包名大小写     |
| searchInClasses    | Collection&lt;ClassData&gt;                                   | 在指定的类列表中搜索方法  |
| searchInMethods    | Collection&lt;MethodData&gt;                                  | 在指定的方法列表中搜索方法 |
| matchers           | Collection&lt;[StringMatchersGroup](#stringmatchersgroup)&gt; | 查询分组列表        |

## 匹配器相关

### AccessFlagsMatcher

| 字段名       | 类型                      | 说明                |
|:----------|:------------------------|:------------------|
| modifiers | Int                     | 匹配的修饰符的 bit masks |
| matchType | [MatchType](#matchtype) | 匹配模式              |

### AnnotationEncodeValueMatcher

| 字段名             | 类型                                                            | 说明        |
|:----------------|:--------------------------------------------------------------|:----------|
| byteValue       | Byte                                                          | 匹配的字节     |
| shortValue      | Short                                                         | 匹配的短整型    |
| charValue       | Char                                                          | 匹配的字符     |
| intValue        | Int                                                           | 匹配的整型     |
| longValue       | Long                                                          | 匹配的长整型    |
| floatValue      | Float                                                         | 匹配的浮点型    |
| doubleValue     | Double                                                        | 匹配的双精度浮点  |
| stringValue     | [StringMatcher](#stringmatcher)                               | 匹配的字符串    |
| methodValue     | [MethodMatcher](#methodmatcher)                               | 匹配的方法     |
| enumValue       | [FieldMatcher](#fieldmatcher)                                 | 匹配的枚举     |
| arrayValue      | [AnnotationEncodeArrayMatcher](#annotationencodearraymatcher) | 匹配的数组     |
| annotationValue | [AnnotationMatcher](#annotationmatcher)                       | 匹配的注解     |
| nullValue       | 无                                                             | 匹配 `null` |
| boolValue       | Boolean                                                       | 匹配的布尔值    |

### IntRange

| 字段名 | 类型  | 说明                      |
|:----|:----|:------------------------|
| min | Int | 最小值，默认为 `0`             |
| max | Int | 最大值，默认为 `Int.MAX_VALUE` |

### NumberEncodeValueMatcher

| 字段名         | 类型     | 说明       |
|:------------|:-------|:---------|
| byteValue   | Byte   | 匹配的字节    |
| shortValue  | Short  | 匹配的短整型   |
| charValue   | Char   | 匹配的字符    |
| intValue    | Int    | 匹配的整型    |
| longValue   | Long   | 匹配的长整型   |
| floatValue  | Float  | 匹配的浮点型   |
| doubleValue | Double | 匹配的双精度浮点 |

### OpcodesMatcher

| 字段名       | 类型                                  | 说明                         |
|:----------|:------------------------------------|:---------------------------|
| opCodes   | Collection&lt;Int&gt;               | 匹配的操作码对应的 Int 值            |
| opNames   | Collection&lt;String&gt;            | 匹配的操作码对应的名称，即 smali 语法中的名称 |
| matchType | [OpCodeMatchType](#opcodematchtype) | 匹配模式                       |
| size      | [IntRange](#intrange)               | 该操作码总长度范围                  |

### StringMatcher

| 字段名        | 类型                                  | 说明      |
|:-----------|:------------------------------------|:--------|
| value      | String                              | 匹配的字符串  |
| matchType  | [StringMatchType](#stringmatchtype) | 匹配模式    |
| ignoreCase | Boolean                             | 是否忽略大小写 |

### TargetElementTypesMatcher

| 字段名       | 类型                                                        | 说明             |
|:----------|:----------------------------------------------------------|:---------------|
| types     | Collection&lt;[TargetElementType](#targetelementtype)&gt; | 匹配的注解声明的元素类型列表 |
| matchType | [MatchType](#matchtype)                                   | 匹配模式           |

### AnnotationElementMatcher

| 字段名   | 类型                                                            | 说明    |
|:------|:--------------------------------------------------------------|:------|
| name  | [StringMatcher](#stringmatcher)                               | 匹配的名称 |
| value | [AnnotationEncodeValueMatcher](#annotationencodevaluematcher) | 匹配的值  |

### AnnotationElementsMatcher

| 字段名       | 类型                                                                      | 说明           |
|:----------|:------------------------------------------------------------------------|:-------------|
| elements  | Collection&lt;[AnnotationElementMatcher](#annotationelementmatcher)&gt; | 匹配的注解声明的元素列表 |
| matchType | [MatchType](#matchtype)                                                 | 匹配模式         |
| count     | [IntRange](#intrange)                                                   | 匹配的注解声明的元素数量 |

### AnnotationEncodeArrayMatcher

| 字段名       | 类型                                                                              | 说明           |
|:----------|:--------------------------------------------------------------------------------|:-------------|
| values    | Collection&lt;[AnnotationEncodeValueMatcher](#annotationencodevaluematcher)&gt; | 匹配的注解声明的元素列表 |
| matchType | [MatchType](#matchtype)                                                         | 匹配模式         |
| count     | [IntRange](#intrange)                                                           | 匹配的注解声明的元素数量 |

### AnnotationMatcher

| 字段名                | 类型                                                      | 说明             |
|:-------------------|:--------------------------------------------------------|:---------------|
| type               | [ClassMatcher](#classmatcher)                           | 匹配的注解类型        |
| targetElementTypes | [TargetElementTypesMatcher](#targetelementtypesmatcher) | 匹配的注解声明的元素类型列表 |
| policy             | [RetentionPolicyType](#retentionpolicytype)             | 匹配的注解声明的保留策略   |
| elements           | [AnnotationElementsMatcher](#annotationelementsmatcher) | 匹配的注解声明的元素列表   |
| usingStrings       | Collection&lt;[StringMatcher](#stringmatcher)&gt;       | 注解中使用的字符串列表    |

### AnnotationsMatcher

| 字段名         | 类型                                                        | 说明      |
|:------------|:----------------------------------------------------------|:--------|
| annotations | Collection&lt;[AnnotationMatcher](#annotationmatcher)&gt; | 匹配的注解列表 |
| matchType   | [MatchType](#matchtype)                                   | 匹配模式    |
| count       | [IntRange](#intrange)                                     | 匹配的注解数量 |

### ClassMatcher

| 字段名          | 类型                                                | 说明                              |
|:-------------|:--------------------------------------------------|:--------------------------------|
| source       | [StringMatcher](#stringmatcher)                   | 类的源码文件名，即 smali 中的 `.source` 字段 |
| className    | [StringMatcher](#stringmatcher)                   | 类的名称                            |
| modifiers    | [AccessFlagsMatcher](#accessflagsmatcher)         | 类的修饰符                           |
| superClass   | [ClassMatcher](#classmatcher)                     | 类的父类                            |
| interfaces   | [InterfacesMatcher](#interfacesmatcher)           | 类的接口列表                          |
| annotations  | [AnnotationsMatcher](#annotationsmatcher)         | 类的注解列表                          |
| fields       | [FieldsMatcher](#fieldsmatcher)                   | 类的字段列表                          |
| methods      | [MethodsMatcher](#methodsmatcher)                 | 类的方法列表                          |
| usingStrings | Collection&lt;[StringMatcher](#stringmatcher)&gt; | 类中使用的字符串列表                      |

### InterfacesMatcher

| 字段名        | 类型                                              | 说明      |
|:-----------|:------------------------------------------------|:--------|
| interfaces | Collection&lt;[ClassMatcher](#classmatcher)&gt; | 匹配的接口列表 |
| matchType  | [MatchType](#matchtype)                         | 匹配模式    |
| count      | [IntRange](#intrange)                           | 匹配的接口数量 |

### FieldMatcher

| 字段名           | 类型                                        | 说明         |
|:--------------|:------------------------------------------|:-----------|
| name          | [StringMatcher](#stringmatcher)           | 字段的名称      |
| modifiers     | [AccessFlagsMatcher](#accessflagsmatcher) | 字段的修饰符     |
| declaredClass | [ClassMatcher](#classmatcher)             | 字段的声明类     |
| type          | [ClassMatcher](#classmatcher)             | 字段的类型      |
| annotations   | [AnnotationsMatcher](#annotationsmatcher) | 字段的注解      |
| getMethods    | [MethodsMatcher](#methodsmatcher)         | 读取该字段的方法列表 |
| putMethods    | [MethodsMatcher](#methodsmatcher)         | 设置该字段的方法列表 |

### FieldsMatcher

| 字段名       | 类型                                              | 说明      |
|:----------|:------------------------------------------------|:--------|
| fields    | Collection&lt;[FieldMatcher](#fieldmatcher)&gt; | 匹配的字段列表 |
| matchType | [MatchType](#matchtype)                         | 匹配模式    |
| count     | [IntRange](#intrange)                           | 匹配的字段数量 |

### MethodMatcher

| 字段名           | 类型                                                        | 说明          |
|:--------------|:----------------------------------------------------------|:------------|
| name          | [StringMatcher](#stringmatcher)                           | 方法的名称       |
| modifiers     | [AccessFlagsMatcher](#accessflagsmatcher)                 | 方法的修饰符      |
| declaredClass | [ClassMatcher](#classmatcher)                             | 方法的声明类      |
| returnType    | [ClassMatcher](#classmatcher)                             | 方法的返回值类型    |
| params        | [ParametersMatcher](#parametersmatcher)                   | 方法的参数列表     |
| annotations   | [AnnotationsMatcher](#annotationsmatcher)                 | 方法的注解       |
| opCodes       | [OpcodesMatcher](#opcodesmatcher)                         | 方法的操作码列表    |
| usingStrings  | Collection&lt;[StringMatcher](#stringmatcher)&gt;         | 方法中使用的字符串列表 |
| usingFields   | Collection&lt;[UsingFieldMatcher](#usingfieldmatcher)&gt; | 方法中使用的字段列表  |
| usingNumbers  | Collection&lt;Number&gt;                                  | 方法中使用的数字列表  |
| invokeMethods | [MethodsMatcher](#methodsmatcher)                         | 方法中调用的方法列表  |
| callMethods   | [MethodsMatcher](#methodsmatcher)                         | 调用了该方法的方法列表 |

### MethodsMatcher

| 字段名       | 类型                                                | 说明      |
|:----------|:--------------------------------------------------|:--------|
| methods   | Collection&lt;[MethodMatcher](#methodmatcher)&gt; | 匹配的方法列表 |
| matchType | [MatchType](#matchtype)                           | 匹配模式    |
| count     | [IntRange](#intrange)                             | 匹配的方法数量 |

### ParameterMatcher

| 字段名         | 类型                                        | 说明    |
|:------------|:------------------------------------------|:------|
| type        | [ClassMatcher](#classmatcher)             | 参数的类型 |
| annotations | [AnnotationsMatcher](#annotationsmatcher) | 参数的注解 |

### ParametersMatcher

| 字段名    | 类型                                                      | 说明      |
|:-------|:--------------------------------------------------------|:--------|
| params | Collection&lt;[ParameterMatcher](#parametermatcher)&gt; | 匹配的参数列表 |
| count  | [IntRange](#intrange)                                   | 匹配的参数数量 |

### StringMatchersGroup

| 字段名       | 类型                                                | 说明                 |
|:----------|:--------------------------------------------------|:-------------------|
| groupName | String                                            | 分组名称               |
| matchers  | Collection&lt;[StringMatcher](#stringmatcher)&gt; | 该查询分组匹配对象所使用的字符串列表 |

### UsingFieldMatcher

| 字段名       | 类型                            | 说明    |
|:----------|:------------------------------|:------|
| field     | [FieldMatcher](#fieldmatcher) | 匹配的字段 |
| usingType | [UsingType](#usingtype)       | 使用类型  |

### EncodeValueByte

| 字段名   | 类型   | 说明 |
|:------|:-----|:---|
| value | Byte | 字节 |

### EncodeValueShort

| 字段名   | 类型    | 说明  |
|:------|:------|:----|
| value | Short | 短整型 |

### EncodeValueChar

| 字段名   | 类型   | 说明 |
|:------|:-----|:---|
| value | Char | 字符 |

### EncodeValueInt

| 字段名   | 类型  | 说明 |
|:------|:----|:---|
| value | Int | 整型 |

### EncodeValueLong

| 字段名   | 类型   | 说明  |
|:------|:-----|:----|
| value | Long | 长整型 |

### EncodeValueFloat

| 字段名   | 类型    | 说明  |
|:------|:------|:----|
| value | Float | 浮点型 |

### EncodeValueDouble

| 字段名   | 类型     | 说明    |
|:------|:-------|:------|
| value | Double | 双精度浮点 |

### EncodeValueString

| 字段名   | 类型     | 说明  |
|:------|:-------|:----|
| value | String | 字符串 |

### EncodeValueNull

该对象无字段。

### EncodeValueBoolean

| 字段名   | 类型      | 说明  |
|:------|:--------|:----|
| value | Boolean | 布尔值 |

## 枚举相关

### AnnotationEncodeValueType

| 字段名             | 类型   | 说明      |
|:----------------|:-----|:--------|
| ByteValue       | Enum | 字节类型    |
| ShortValue      | Enum | 短整型类型   |
| CharValue       | Enum | 字符类型    |
| IntValue        | Enum | 整型类型    |
| LongValue       | Enum | 长整型类型   |
| FloatValue      | Enum | 浮点类型    |
| DoubleValue     | Enum | 双精度浮点类型 |
| StringValue     | Enum | 字符串类型   |
| TypeValue       | Enum | 类型类型    |
| MethodValue     | Enum | 方法类型    |
| EnumValue       | Enum | 枚举类型    |
| ArrayValue      | Enum | 数组类型    |
| AnnotationValue | Enum | 注解类型    |
| NullValue       | Enum | 空类型     |
| BoolValue       | Enum | 布尔类型    |

### AnnotationVisibilityType

详情参照 [Visibility values](https://source.android.com/docs/core/runtime/dex-format#visibility)

| 字段名     | 类型   | 说明    |
|:--------|:-----|:------|
| Build   | Enum | 构建时可见 |
| Runtime | Enum | 运行时可见 |
| System  | Enum | 系统可见  |

### MatchType

| 字段名      | 类型   | 说明 |
|:---------|:-----|:---|
| Contains | Enum | 包含 |
| Equals   | Enum | 等于 |

### NumberEncodeValueType

| 字段名         | 类型   | 说明      |
|:------------|:-----|:--------|
| ByteValue   | Enum | 字节类型    |
| ShortValue  | Enum | 短整型类型   |
| CharValue   | Enum | 字符类型    |
| IntValue    | Enum | 整型类型    |
| LongValue   | Enum | 长整型类型   |
| FloatValue  | Enum | 浮点类型    |
| DoubleValue | Enum | 双精度浮点类型 |

### OpCodeMatchType

| 字段名        | 类型   | 说明     |
|:-----------|:-----|:-------|
| Contains   | Enum | 包含     |
| StartsWith | Enum | 以...开头 |
| EndsWith   | Enum | 以...结尾 |
| Equals     | Enum | 等于     |

### RetentionPolicyType

该 Enum 与 `java.lang.annotation.RetentionPolicy` 保持对应。

| 字段名     | 类型   | 说明    |
|:--------|:-----|:------|
| Source  | Enum | 源码级别  |
| Class   | Enum | 类级别   |
| Runtime | Enum | 运行时级别 |

### StringMatchType

| 字段名          | 类型   | 说明                  |
|:-------------|:-----|:--------------------|
| Contains     | Enum | 包含                  |
| StartsWith   | Enum | 以...开头              |
| EndsWith     | Enum | 以...结尾              |
| SimilarRegex | Enum | 类正则匹配，只支持 `^` 与 `$` |
| Equals       | Enum | 等于                  |

### TargetElementType

该 Enum 与 `java.lang.annotation.ElementType` 保持对应。

| 字段名            | 类型   | 说明   |
|:---------------|:-----|:-----|
| Type           | Enum | 类型   |
| Field          | Enum | 字段   |
| Method         | Enum | 方法   |
| Parameter      | Enum | 参数   |
| Constructor    | Enum | 构造方法 |
| LocalVariable  | Enum | 局部变量 |
| AnnotationType | Enum | 注解类型 |
| Package        | Enum | 包    |
| TypeParameter  | Enum | 类型参数 |
| TypeUse        | Enum | 类型使用 |

### UsingType

| 字段名 | 类型   | 说明 |
|:----|:-----|:---|
| Any | Enum | 任意 |
| Get | Enum | 获取 |
| Set | Enum | 设置 |
