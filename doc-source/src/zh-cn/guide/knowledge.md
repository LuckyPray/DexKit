# 基础知识

> 这里提供了一些基础知识，帮助您更好的理解 `DexKit` 的使用，已经掌握的开发者可以跳过这一章节。

在使用 `DexKit` 时，有一些基础知识是必要的，其中包括但不限于以下内容：

- Dex 反编译工具
- JVM 签名
    - 原始类型签名
    - 引用类型签名
    - 数组类型签名
    - 方法签名
    - 字段签名

::: warning
基础知识中的内容<u>**不一定完全准确**</u>，请根据自己的见解酌情阅读，若发现内容有误，欢迎指正并帮助改进。
:::

## 反编译工具

通常，您可以使用 [jadx](https://github.com/skylot/jadx) 来满足大部分需求，
它在大多数情况下能还原出可读的 Java 代码。

## JVM 签名

### 原始类型(PrimitiveType)

| 类型签名 | 原始类型    | 大小（字节） |
|:-----|:--------|:-------|
| V    | void    | -      |
| Z    | boolean | 1      |
| B    | byte    | 1      |
| C    | char    | 2      |
| S    | short   | 2      |
| I    | int     | 4      |
| J    | long    | 8      |
| F    | float   | 4      |
| D    | double  | 8      |

### 引用类型(ReferenceType)

引用类型分为类与数组。

#### 类(ClassType)

类的类型签名都是以 `L` 开头，以 `;` 结尾，中间是类的全限定名(FullClassName)，如 `Ljava/lang/String;`。

例如:

| 类型签名               | Java 中类型定义       |
|:-------------------|:-----------------|
| Ljava/lang/String; | java.lang.String |
| Ljava/util/List;   | java.util.List   |

#### 数组(ArrayType)

数组类型的类型签名以 `[` 开头，后面跟着数组元素的类型签名，如 `[[I` 表示一个二维数组，数组中的元素类型是
`int`。

例如:

| 类型签名                | Java 中类型定义         |
|:--------------------|:-------------------|
| [I                  | int[]              |
| [[C                 | char[][]           |
| [Ljava/lang/String; | java.lang.String[] |

::: tip
`类` 与 `类型` 并不完全等价：类型为 Type，而类为 Class。 `类` 是 `类型` 的子集。 例如：

- `java.lang.Integer` 是 `类`，也是 `类型`
- `java.lang.Integer[]` 是 `数组类型`，但不是 `类`
- `int` 是 `原始类型`，但不是 `类`

对于方法参数，返回值类型以及字段的类型，我们统一称为 `类型` 即 `Type`
:::

### 方法签名

方法签名由方法的返回值类型签名和参数类型签名组成，如 `()V` 表示一个无参的 `void` 方法。

例如:
> 为了方便表述，表格中所有的方法都命名为 `function`

| 方法签名                    | Java 中方法定义                                 |
|:------------------------|:-------------------------------------------|
| ()V                     | void function()                            |
| (I)V                    | void function(int)                         |
| (II)V                   | void function(int, int)                    |
| (ILjava/lang/String;J)V | void function(int, java.lang.String, long) |
| (I[II)V                 | void function(int, int[], int)             |
| ([[Ljava/lang/String;)V | void function(java.lang.String[][])        |
| ()[Ljava/lang/String;   | java.lang.String[] function()              |

### 方法原型简写 (ProtoShorty)

方法原型简写是一个表示方法返回类型和参数类型的紧凑形式字符串。每个字符代表一种类型，第一个字符代表返回类型，其余字符代表参数类型。

#### 类型字符映射

| 字符 | 类型      | 说明                  |
|:---|:--------|:--------------------|
| V  | void    | 空类型                 |
| Z  | boolean | 布尔类型                |
| B  | byte    | 字节类型                |
| S  | short   | 短整型                 |
| C  | char    | 字符类型                |
| I  | int     | 整型                  |
| J  | long    | 长整型                 |
| F  | float   | 单精度浮点型              |
| D  | double  | 双精度浮点型              |
| L  | Object  | 引用类型（包括对象数组和基本类型数组） |

#### 使用示例

| 原型简写  | 对应的方法签名                                       |
|:------|:----------------------------------------------|
| VL    | void method(Object)                           |
| ZLL   | boolean method(Object, Object)                |
| VILFD | void method(int, Object, long, float, double) |
| LL    | Object method(Object)                         |
| ILI   | int method(Object, int)                       |
| LIL   | Object method(int, Object)                    |

::: tip
在方法原型简写中，所有的引用类型（包括类、接口、数组等）都用字符 `L` 表示，这使得简写更加紧凑。
这意味着 `String`、`String[]`、`int[]` 等类型在简写中都表示为 `L`。
:::

## Dalvik 描述

在 Dex 文件中，我们可以通过 `Dalvik 描述` 的方式来表示特定的类、方法或字段。在 `DexKit` API中，通常使用
`descriptor` 来命名。

### 类描述

类描述的格式为 `[类签名]`，如 `Ljava/lang/String;`。

### 方法描述

方法描述的格式为 `[类签名]->[方法名][方法签名]`，如 `Ljava/lang/String;->length()I`。

::: tip
在 `Dalvik 描述` 中，构造函数的方法名为 `<init>`，静态初始化函数的方法名为 `<clinit>`。
所以在 `DexKit` 中如果想要查找构造函数，需要使用 `<init>` 作为方法名。
:::

### 字段描述

字段描述的格式为 `[类签名]->[字段名]:[类型签名]`，如 `Ljava/lang/String;->count:I`。

::: tip
DexKit 中 className/Type 查询参数只支持 Java 原始写法，例如：

- 对于基本类型，填写 `void`，`int`，`boolean` 形式的 Java PrimitiveType
- 对于引用类型，填写 `java.lang.String` 或者 `java/lang/String` 形式的 FullClassName
- 对于数组类型，填写 `int[]`，`java.lang.String[][]` 或者 `java/lang/String[][]` 形式的 ArrayTypeName
  :::
