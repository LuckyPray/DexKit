# Basic Knowledge

> Here we provide some basic knowledge to help you better understand the usage of `DexKit`.
> Experienced developers can skip this section.

When using `DexKit`, there are some fundamental concepts you need to understand, including but not
limited to:

- Dex Decompilation Tools
- JVM Signatures
    - Primitive Type Signatures
    - Reference Type Signatures
    - Array Type Signatures
    - Method Signatures
    - Field Signatures

::: warning
The content in the basic knowledge is not necessarily completely accurate. Please read it according
to your own understanding. If you find any inaccuracies, feel free to point them out and help
improve.
:::

## Decompilation Tools

Usually, you can use [jadx](https://github.com/skylot/jadx) to meet most of your needs.
It can restore readable Java code in most cases.

## JVM Signatures

### Primitive Types (PrimitiveType)

| Type Signature | Primitive Type | Size (Bytes) |
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

### Reference Types (ReferenceType)

Reference types are divided into classes and arrays.

#### Class (ClassType)

The type signature of a class starts with `L`, followed by the fully qualified name (FullClassName)
of the class, and ends with `;`. For example, `Ljava/lang/String;` represents the `java.lang.String`
class.

For example:

| Type Signature     | Java Type Definition |
|:-------------------|:---------------------|
| Ljava/lang/String; | java.lang.String     |
| Ljava/util/List;   | java.util.List       |

#### Array (ArrayType)

The type signature of an array starts with `[`, followed by the type signature of the array
elements.
For example, `[[I` represents a two-dimensional array where the element type is `int`.

For example:

| Type Signature      | Java Type Definition |
|:--------------------|:---------------------|
| [I                  | int[]                |
| [[C                 | char[][]             |
| [Ljava/lang/String; | java.lang.String[]   |

::: tip
The term 'class' and 'type' are not entirely equivalent: 'type' is Type, while 'class' is Class.
'Class' is a subset of 'type'. For example:

- `java.lang.Integer` is a 'class' and also a 'type'
- `java.lang.Integer[]` is an 'array type', but not a 'class'
- `int` is a 'primitive type', but not a 'class'

For method parameters, return types, and field types, we use the term 'type,' specifically 'Type.'
:::

### Method Signatures

A method signature consists of the signature of the return type and the signature of the parameter
types. For example, `()V` represents a parameterless `void` method.

For example:
> For ease of description, all methods in the table are named as `function`.

| Method Signature        | Java Method Definition                     |
|:------------------------|:-------------------------------------------|
| ()V                     | void function()                            |
| (I)V                    | void function(int)                         |
| (II)V                   | void function(int, int)                    |
| (ILjava/lang/String;J)V | void function(int, java.lang.String, long) |
| (I[II)V                 | void function(int, int[], int)             |
| ([[Ljava/lang/String;)V | void function(java.lang.String[][])        |
| ()[Ljava/lang/String;   | java.lang.String[] function()              |

### Method Prototype Shorthand (ProtoShorty)

The method prototype shorthand is a compact string representation of a method’s return and parameter
types. Each character represents a type: the first character is the return type, and the remaining
characters are the parameter types.

#### Type Character Mapping

| Character | Type    | Description                                                         |
|:----------|:--------|:--------------------------------------------------------------------|
| V         | void    | no value                                                            |
| Z         | boolean | Boolean                                                             |
| B         | byte    | Byte                                                                |
| S         | short   | Short                                                               |
| C         | char    | Character                                                           |
| I         | int     | Integer                                                             |
| J         | long    | Long                                                                |
| F         | float   | Single‐precision floating point                                     |
| D         | double  | Double‐precision floating point                                     |
| L         | Object  | Reference type (including object arrays and primitive type arrays)) |

#### Usage Examples

| Shorthand | Corresponding Method Signature                  |
|:----------|:------------------------------------------------|
| VL        | `void method(Object)`                           |
| ZLL       | `boolean method(Object, Object)`                |
| VILFD     | `void method(int, Object, long, float, double)` |
| LL        | `Object method(Object)`                         |
| ILI       | `int method(Object, int)`                       |
| LIL       | `Object method(int, Object)`                    |

::: tip
In the shorthand, all reference types (classes, interfaces, arrays, etc.) are represented by the
character `L` to keep the string compact. This means `String`, `String[]`, `int[]`, and similar
types are all encoded as `L`.
:::

## Dalvik Descriptor

In a Dex file, we can represent specific classes, methods, or fields using the 'Dalvik Descriptor.'
In `DexKit` API, the term 'descriptor' is commonly used.

### Class Descriptor

The format of a class descriptor is `[class signature]`, such as `Ljava/lang/String;`.

### Method Descriptor

The format of a method descriptor is `[class signature]->[method name][method signature]`,
such as `Ljava/lang/String;->length()I`.

::: tip
In 'Dalvik Descriptor,' the method name for constructors is `<init>`, and for static initialization
methods, it's `<clinit>`. Therefore, in `DexKit`, to find constructors, you need to use `<init>` as
the method name.
:::

### Field Descriptor

The format of a field descriptor is `[class signature]->[field name]:[type signature]`, such
as `Ljava/lang/String;->count:I`.

::: tip
In DexKit, the className/Type query parameter only supports the Java primitive syntax. For example:

- For primitive types, use Java PrimitiveType forms like `void`, `int`, `boolean`.
- For reference types, use FullClassName forms like `java.lang.String` or `java/lang/String`.
- For array types, use ArrayTypeName forms like `int[]`, `java.lang.String[][]`
  or `java/lang/String[][]`.

:::
