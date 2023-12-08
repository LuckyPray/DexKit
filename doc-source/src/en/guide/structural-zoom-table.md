# Quick Reference Guide

> This section introduces the structure components of `DexKit` queries. Matcher objects can be
> recursively nested, and all conditions are optional.

## Query Related

### FindClass

| Field Name         | Type                          | Description                                                                                               |
|:-------------------|:------------------------------|:----------------------------------------------------------------------------------------------------------|
| searchPackages     | Collection&lt;String&gt;      | List of package names to search                                                                           |
| excludePackages    | Collection&lt;String&gt;      | List of excluded package names                                                                            |
| ignorePackagesCase | Boolean                       | Whether to ignore package name case                                                                       |
| searchIn           | Collection&lt;ClassData&gt;   | Search for classes in the specified list of classes                                                       |
| findFirst          | Boolean                       | Return the first result found immediately; results are not guaranteed to be unique due to multi-threading |
| matcher            | [ClassMatcher](#classmatcher) | Matching conditions                                                                                       |

### FindField

| Field Name         | Type                          | Description                                                                                               |
|:-------------------|:------------------------------|:----------------------------------------------------------------------------------------------------------|
| searchPackages     | Collection&lt;String&gt;      | List of package names to search                                                                           |
| excludePackages    | Collection&lt;String&gt;      | List of excluded package names                                                                            |
| ignorePackagesCase | Boolean                       | Whether to ignore package name case                                                                       |
| searchInClasses    | Collection&lt;ClassData&gt;   | Search for fields in the specified list of classes                                                        |
| searchInFields     | Collection&lt;FieldData&gt;   | Search for fields in the specified list of fields                                                         |
| findFirst          | Boolean                       | Return the first result found immediately; results are not guaranteed to be unique due to multi-threading |
| matcher            | [FieldMatcher](#fieldmatcher) | Matching conditions                                                                                       |

### FindMethod

| Field Name         | Type                            | Description                                                                                               |
|:-------------------|:--------------------------------|:----------------------------------------------------------------------------------------------------------|
| searchPackages     | Collection&lt;String&gt;        | List of package names to search                                                                           |
| excludePackages    | Collection&lt;String&gt;        | List of excluded package names                                                                            |
| ignorePackagesCase | Boolean                         | Whether to ignore package name case                                                                       |
| searchInClasses    | Collection&lt;ClassData&gt;     | Search for methods in the specified list of classes                                                       |
| searchInMethods    | Collection&lt;MethodData&gt;    | Search for methods in the specified list of methods                                                       |
| findFirst          | Boolean                         | Return the first result found immediately; results are not guaranteed to be unique due to multi-threading |
| matcher            | [MethodMatcher](#methodmatcher) | Matching conditions                                                                                       |

### BatchFindClassUsingStrings

| Field Name         | Type                                                          | Description                                         |
|:-------------------|:--------------------------------------------------------------|:----------------------------------------------------|
| searchPackages     | Collection&lt;String&gt;                                      | List of package names to search                     |
| excludePackages    | Collection&lt;String&gt;                                      | List of excluded package names                      |
| ignorePackagesCase | Boolean                                                       | Whether to ignore package name case                 |
| searchIn           | Collection&lt;ClassData&gt;                                   | Search for classes in the specified list of classes |
| matchers           | Collection&lt;[StringMatchersGroup](#stringmatchersgroup)&gt; | List of query groups using strings                  |

### BatchFindMethodUsingStrings

| Field Name         | Type                                                          | Description                                         |
|:-------------------|:--------------------------------------------------------------|:----------------------------------------------------|
| searchPackages     | Collection&lt;String&gt;                                      | List of package names to search                     |
| excludePackages    | Collection&lt;String&gt;                                      | List of excluded package names                      |
| ignorePackagesCase | Boolean                                                       | Whether to ignore package name case                 |
| searchInClasses    | Collection&lt;ClassData&gt;                                   | Search for methods in the specified list of classes |
| searchInMethods    | Collection&lt;MethodData&gt;                                  | Search for methods in the specified list of methods |
| matchers           | Collection&lt;[StringMatchersGroup](#stringmatchersgroup)&gt; | List of query groups using strings                  |

## Matcher Related

### AccessFlagsMatcher

| Field Name | Type                    | Description                      |
|:-----------|:------------------------|:---------------------------------|
| modifiers  | Int                     | Bit masks for matching modifiers |
| matchType  | [MatchType](#matchtype) | Matching mode                    |

### AnnotationEncodeValueMatcher

| Field Name      | Type                                                          | Description                    |
|:----------------|:--------------------------------------------------------------|:-------------------------------|
| byteValue       | Byte                                                          | Matched byte                   |
| shortValue      | Short                                                         | Matched short integer          |
| charValue       | Char                                                          | Matched character              |
| intValue        | Int                                                           | Matched integer                |
| longValue       | Long                                                          | Matched long integer           |
| floatValue      | Float                                                         | Matched float                  |
| doubleValue     | Double                                                        | Matched double precision float |
| stringValue     | [StringMatcher](#stringmatcher)                               | Matched string                 |
| methodValue     | [MethodMatcher](#methodmatcher)                               | Matched method                 |
| enumValue       | [FieldMatcher](#fieldmatcher)                                 | Matched enumeration            |
| arrayValue      | [AnnotationEncodeArrayMatcher](#annotationencodearraymatcher) | Matched array                  |
| annotationValue | [AnnotationMatcher](#annotationmatcher)                       | Matched annotation             |
| nullValue       | None                                                          | Match `null`                   |
| boolValue       | Boolean                                                       | Matched boolean                |

### IntRange

| Field Name | Type | Description                               |
|:-----------|:-----|:------------------------------------------|
| min        | Int  | Minimum value, default is `0`             |
| max        | Int  | Maximum value, default is `Int.MAX_VALUE` |

### NumberEncodeValueMatcher

| Field Name  | Type   | Description                    |
|:------------|:-------|:-------------------------------|
| byteValue   | Byte   | Matched byte                   |
| shortValue  | Short  | Matched short integer          |
| charValue   | Char   | Matched character              |
| intValue    | Int    | Matched integer                |
| longValue   | Long   | Matched long integer           |
| floatValue  | Float  | Matched float                  |
| doubleValue | Double | Matched double precision float |

### OpcodesMatcher

| Field Name | Type                                | Description                                    |
|:-----------|:------------------------------------|:-----------------------------------------------|
| opCodes    | Collection&lt;Int&gt;               | Matched Int values of opcodes                  |
| opNames    | Collection&lt;String&gt;            | Matched names of opcodes (as per smali syntax) |
| matchType  | [OpCodeMatchType](#opcodematchtype) | Matching mode                                  |
| size       | [IntRange](#intrange)               | Total length range of the opcode               |

### StringMatcher

| Field Name | Type                                | Description             |
|:-----------|:------------------------------------|:------------------------|
| value      | String                              | Matched string          |
| matchType  | [StringMatchType](#stringmatchtype) | Matching mode           |
| ignoreCase | Boolean                             | Ignore case sensitivity |

### TargetElementTypesMatcher

| Field Name | Type                                                      | Description                              |
|:-----------|:----------------------------------------------------------|:-----------------------------------------|
| types      | Collection&lt;[TargetElementType](#targetelementtype)&gt; | List of matched annotation element types |
| matchType  | [MatchType](#matchtype)                                   | Matching mode                            |

### AnnotationElementMatcher

| Field Name | Type                                                          | Description   |
|:-----------|:--------------------------------------------------------------|:--------------|
| name       | [StringMatcher](#stringmatcher)                               | Matched name  |
| value      | [AnnotationEncodeValueMatcher](#annotationencodevaluematcher) | Matched value |

### AnnotationElementsMatcher

| Field Name | Type                                                                    | Description                         |
|:-----------|:------------------------------------------------------------------------|:------------------------------------|
| elements   | Collection&lt;[AnnotationElementMatcher](#annotationelementmatcher)&gt; | List of matched annotation elements |
| matchType  | [MatchType](#matchtype)                                                 | Matching mode                       |
| count      | [IntRange](#intrange)                                                   | Number of matched elements          |

### AnnotationEncodeArrayMatcher

| Field Name | Type                                                                            | Description                         |
|:-----------|:--------------------------------------------------------------------------------|:------------------------------------|
| values     | Collection&lt;[AnnotationEncodeValueMatcher](#annotationencodevaluematcher)&gt; | List of matched annotation elements |
| matchType  | [MatchType](#matchtype)                                                         | Matching mode                       |
| count      | [IntRange](#intrange)                                                           | Number of matched elements          |

### AnnotationMatcher

| Field Name         | Type                                                    | Description                              |
|:-------------------|:--------------------------------------------------------|:-----------------------------------------|
| type               | [ClassMatcher](#classmatcher)                           | Matched annotation type                  |
| targetElementTypes | [TargetElementTypesMatcher](#targetelementtypesmatcher) | List of matched annotation element types |
| policy             | [RetentionPolicyType](#retentionpolicytype)             | Matched retention policy                 |
| elements           | [AnnotationElementsMatcher](#annotationelementsmatcher) | List of matched annotation elements      |
| usingStrings       | Collection&lt;[StringMatcher](#stringmatcher)&gt;       | List of strings used in the annotation   |

### AnnotationsMatcher

| Field Name  | Type                                                      | Description                   |
|:------------|:----------------------------------------------------------|:------------------------------|
| annotations | Collection&lt;[AnnotationMatcher](#annotationmatcher)&gt; | List of matched annotations   |
| matchType   | [MatchType](#matchtype)                                   | Matching mode                 |
| count       | [IntRange](#intrange)                                     | Number of matched annotations |

### ClassMatcher

| Field Name   | Type                                              | Description                                                   |
|:-------------|:--------------------------------------------------|:--------------------------------------------------------------|
| source       | [StringMatcher](#stringmatcher)                   | Source file name of the class, i.e., `.source` field in smali |
| className    | [StringMatcher](#stringmatcher)                   | Name of the class                                             |
| modifiers    | [AccessFlagsMatcher](#accessflagsmatcher)         | Modifiers of the class                                        |
| superClass   | [ClassMatcher](#classmatcher)                     | Superclass of the class                                       |
| interfaces   | [InterfacesMatcher](#interfacesmatcher)           | List of interfaces implemented by the class                   |
| annotations  | [AnnotationsMatcher](#annotationsmatcher)         | List of annotations for the class                             |
| fields       | [FieldsMatcher](#fieldsmatcher)                   | List of fields in the class                                   |
| methods      | [MethodsMatcher](#methodsmatcher)                 | List of methods in the class                                  |
| usingStrings | Collection&lt;[StringMatcher](#stringmatcher)&gt; | List of strings used in the class                             |

### InterfacesMatcher

| Field Name | Type                                            | Description                  |
|:-----------|:------------------------------------------------|:-----------------------------|
| interfaces | Collection&lt;[ClassMatcher](#classmatcher)&gt; | List of matched interfaces   |
| matchType  | [MatchType](#matchtype)                         | Matching mode                |
| count      | [IntRange](#intrange)                           | Number of matched interfaces |

### FieldMatcher

| Field Name    | Type                                      | Description                       |
|:--------------|:------------------------------------------|:----------------------------------|
| name          | [StringMatcher](#stringmatcher)           | Name of the field                 |
| modifiers     | [AccessFlagsMatcher](#accessflagsmatcher) | Modifiers of the field            |
| declaredClass | [ClassMatcher](#classmatcher)             | Declaring class of the field      |
| type          | [ClassMatcher](#classmatcher)             | Type of the field                 |
| annotations   | [AnnotationsMatcher](#annotationsmatcher) | List of annotations for the field |
| getMethods    | [MethodsMatcher](#methodsmatcher)         | List of methods to get the field  |
| putMethods    | [MethodsMatcher](#methodsmatcher)         | List of methods to set the field  |

### FieldsMatcher

| Field Name | Type                                            | Description              |
|:-----------|:------------------------------------------------|:-------------------------|
| fields     | Collection&lt;[FieldMatcher](#fieldmatcher)&gt; | List of matched fields   |
| matchType  | [MatchType](#matchtype)                         | Matching mode            |
| count      | [IntRange](#intrange)                           | Number of matched fields |

### MethodMatcher

| Field Name    | Type                                                      | Description                           |
|:--------------|:----------------------------------------------------------|:--------------------------------------|
| name          | [StringMatcher](#stringmatcher)                           | Name of the method                    |
| modifiers     | [AccessFlagsMatcher](#accessflagsmatcher)                 | Modifiers of the method               |
| declaredClass | [ClassMatcher](#classmatcher)                             | Declaring class of the method         |
| returnType    | [ClassMatcher](#classmatcher)                             | Return type of the method             |
| params        | [ParametersMatcher](#parametersmatcher)                   | List of parameters                    |
| annotations   | [AnnotationsMatcher](#annotationsmatcher)                 | List of annotations for the method    |
| opCodes       | [OpcodesMatcher](#opcodesmatcher)                         | List of opcodes for the method        |
| usingStrings  | Collection&lt;[StringMatcher](#stringmatcher)&gt;         | List of strings used in the method    |
| usingFields   | Collection&lt;[UsingFieldMatcher](#usingfieldmatcher)&gt; | List of fields used in the method     |
| usingNumbers  | Collection&lt;Number&gt;                                  | List of numbers used in the method    |
| invokeMethods | [MethodsMatcher](#methodsmatcher)                         | List of methods invoked by the method |
| callMethods   | [MethodsMatcher](#methodsmatcher)                         | List of methods that call the method  |

### MethodsMatcher

| Field Name | Type                                              | Description               |
|:-----------|:--------------------------------------------------|:--------------------------|
| methods    | Collection&lt;[MethodMatcher](#methodmatcher)&gt; | List of matched methods   |
| matchType  | [MatchType](#matchtype)                           | Matching mode             |
| count      | [IntRange](#intrange)                             | Number of matched methods |

### ParameterMatcher

| Field Name  | Type                                      | Description                           |
|:------------|:------------------------------------------|:--------------------------------------|
| type        | [ClassMatcher](#classmatcher)             | Type of the parameter                 |
| annotations | [AnnotationsMatcher](#annotationsmatcher) | List of annotations for the parameter |

### ParametersMatcher

| Field Name | Type                                                    | Description                  |
|:-----------|:--------------------------------------------------------|:-----------------------------|
| params     | Collection&lt;[ParameterMatcher](#parametermatcher)&gt; | List of matched parameters   |
| count      | [IntRange](#intrange)                                   | Number of matched parameters |

### StringMatchersGroup

| Field Name | Type                                              | Description                       |
|:-----------|:--------------------------------------------------|:----------------------------------|
| groupName  | String                                            | Group name for the query          |
| matchers   | Collection&lt;[StringMatcher](#stringmatcher)&gt; | List of strings used in the query |

### UsingFieldMatcher

| Field Name | Type                          | Description   |
|:-----------|:------------------------------|:--------------|
| field      | [FieldMatcher](#fieldmatcher) | Matched field |
| usingType  | [UsingType](#usingtype)       | Type of usage |

### EncodeValueByte

| Field Name | Type | Description |
|:-----------|:-----|:------------|
| value      | Byte | byte value  |

### EncodeValueShort

| Field Name | Type  | Description         |
|:-----------|:------|:--------------------|
| value      | Short | short integer value |

### EncodeValueChar

| Field Name | Type | Description     |
|:-----------|:-----|:----------------|
| value      | Char | character value |

### EncodeValueInt

| Field Name | Type | Description   |
|:-----------|:-----|:--------------|
| value      | Int  | integer value |

### EncodeValueLong

| Field Name | Type | Description        |
|:-----------|:-----|:-------------------|
| value      | Long | long integer value |

### EncodeValueFloat

| Field Name | Type  | Description |
|:-----------|:------|:------------|
| value      | Float | float value |

### EncodeValueDouble

| Field Name | Type   | Description  |
|:-----------|:-------|:-------------|
| value      | Double | double value |

### EncodeValueString

| Field Name | Type   | Description  |
|:-----------|:-------|:-------------|
| value      | String | string value |

### EncodeValueNull

This object has no fields.

### EncodeValueBoolean

| Field Name | Type    | Description   |
|:-----------|:--------|:--------------|
| value      | Boolean | boolean value |

## Enumerations

### AnnotationEncodeValueType

| Field Name      | Type | Description                 |
|:----------------|:-----|:----------------------------|
| ByteValue       | Enum | Byte type                   |
| ShortValue      | Enum | Short integer type          |
| CharValue       | Enum | Character type              |
| IntValue        | Enum | Integer type                |
| LongValue       | Enum | Long integer type           |
| FloatValue      | Enum | Float type                  |
| DoubleValue     | Enum | Double precision float type |
| StringValue     | Enum | String type                 |
| TypeValue       | Enum | Type type                   |
| MethodValue     | Enum | Method type                 |
| EnumValue       | Enum | Enum type                   |
| ArrayValue      | Enum | Array type                  |
| AnnotationValue | Enum | Annotation type             |
| NullValue       | Enum | Null type                   |
| BoolValue       | Enum | Boolean type                |

### AnnotationVisibilityType

Refer to [Visibility values](https://source.android.com/docs/core/runtime/dex-format#visibility)

| Field Name | Type | Description           |
|:-----------|:-----|:----------------------|
| Build      | Enum | Visible at build time |
| Runtime    | Enum | Visible at runtime    |
| System     | Enum | Visible to the system |

### MatchType

| Field Name | Type | Description |
|:-----------|:-----|:------------|
| Contains   | Enum | Contains    |
| Equals     | Enum | Equals      |

### NumberEncodeValueType

| Field Name  | Type | Description                 |
|:------------|:-----|:----------------------------|
| ByteValue   | Enum | Byte type                   |
| ShortValue  | Enum | Short integer type          |
| CharValue   | Enum | Character type              |
| IntValue    | Enum | Integer type                |
| LongValue   | Enum | Long integer type           |
| FloatValue  | Enum | Float type                  |
| DoubleValue | Enum | Double precision float type |

### OpCodeMatchType

| Field Name | Type | Description |
|:-----------|:-----|:------------|
| Contains   | Enum | Contains    |
| StartsWith | Enum | Starts with |
| EndsWith   | Enum | Ends with   |
| Equals     | Enum | Equals      |

### RetentionPolicyType

This Enum corresponds to `java.lang.annotation.RetentionPolicy`.

| Field Name | Type | Description   |
|:-----------|:-----|:--------------|
| Source     | Enum | Source level  |
| Class      | Enum | Class level   |
| Runtime    | Enum | Runtime level |

### StringMatchType

| Field Name   | Type | Description                                     |
|:-------------|:-----|:------------------------------------------------|
| Contains     | Enum | Contains                                        |
| StartsWith   | Enum | Starts with                                     |
| EndsWith     | Enum | Ends with                                       |
| SimilarRegex | Enum | Regex-like pattern, supporting only `^` and `$` |
| Equals       | Enum | Equals                                          |

### TargetElementType

This Enum corresponds to `java.lang.annotation.ElementType`.

| Field Name     | Type | Description     |
|:---------------|:-----|:----------------|
| Type           | Enum | Type            |
| Field          | Enum | Field           |
| Method         | Enum | Method          |
| Parameter      | Enum | Parameter       |
| Constructor    | Enum | Constructor     |
| LocalVariable  | Enum | Local variable  |
| AnnotationType | Enum | Annotation type |
| Package        | Enum | Package         |
| TypeParameter  | Enum | Type parameter  |
| TypeUse        | Enum | Type use        |

### UsingType

| Field Name | Type | Description |
|:-----------|:-----|:------------|
| Any        | Enum | Any usage   |
| Get        | Enum | Get usage   |
| Set        | Enum | Set usage   |
