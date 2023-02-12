# Basic knowledge

You will need some basic knowledge in order to use DexKit more effectively, including but not limited to:

- Dex disassembling tools
- JVM signatures
    - Primitive type signatures
    - Reference type signatures
    - Array type signatures
    - Method signatures
    - Field signatures

## Decompilation Tool

Usually, [jadx](https://github.com/skylot/jadx) is enough to meet most of the requirements,
as long as the Java code is restorable to some extent.

## JVM signature

### Primitive type signature

| type signature | Primitive type | size (bytes) |
|:---------------|:---------------|:-------------|
| V              | void           | -            |
| Z              | boolean        | 1            |
| B              | byte           | 1            |
| C              | char           | 2            |
| S              | short          | 2            |
| I              | int            | 4            |
| J              | long           | 8            |
| F              | float          | 4            |
| D              | double         | 8            |

### Reference type signature

Reference data types include classes, interfaces, arrays, etc.
The type signature for classes and interfaces is denoted by `L`, followed by the fully qualified name of the class,
ending with `;`, such as `Ljava/lang/String;`.

e.g.

| type signature     | type in java     |
|:-------------------|:-----------------|
| Ljava/lang/String; | java.lang.String |
| Ljava/util/List;   | java.util.List   |

### Array type signature

Array type signatures begin with `[` and are followed by the type signature of the array elements, for example,
`[[I` represents a two-dimensional array with elements of type `int`.

e.g.

| type signature      | type in java       |
|:--------------------|:-------------------|
| [I                  | int[]              |
| [[C                 | char[][]           |
| [Ljava/lang/String; | java.lang.String[] |

### Method signature

Method signatures are composed of the return type signature and parameter type signatures of the method,
For example, `()V` represents a `void` method with no parameters.

e.g.
> To make it easier to express, all method names are `function`.

| method signature        | method declare in java              |
|:------------------------|:------------------------------------|
| ()V                     | void function()                     |
| (I)V                    | void function(int)                  |
| (II)V                   | void function(int, int)             |
| (Ljava/lang/String;)V   | void function(java.lang.String)     |
| ([I)V                   | void function(int[])                |
| ([[Ljava/lang/String;)V | void function(java.lang.String[][]) |

## Dalvik descriptor

In dex, specific methods/fields are expressed via the `Dalvik descriptor`.

### Method descriptor

The format of a method description is `[ClassTypeSignature]->[MethodName][MethodSignature]`,
such as `Ljava/lang/String;->length()I`.

> **Note**：
> In `Dalvik description`, the method name for a constructor is `<init>`,
> and the method name for a static initialization function is `<clinit>`.

### Field descriptor

The format of a field description is `[ClassTypeSignature]->[FieldName]:[TypeSignature]`,
such as `Ljava/lang/String;->count:I`.

> **Note**: DexKit query parameters support both the dalvik signature and the original Java version, for example:
>
> - For className related query parameters, you can use either `Ljava/lang/String;` or `java.lang.String`. But `java/lang/String` will not work.
>
> - For `fieldType`/`returnType` related query parameters, you can use either `I` or `int`.
