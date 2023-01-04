# 基础知识

您需要适当的了解一些基础知识，以便更好地使用 `DexKit`，包括但不限于：

- Dex反编译工具
- JVM 签名
    - 原始类型签名
    - 引用类型签名
    - 数组类型签名
    - 方法签名
    - 字段签名

## 反编译工具

正常情况下 [jadx](https://github.com/skylot/jadx) 足以应付大部分的需求，只需要一定程度上还原出Java的代码即可。

## JVM 签名

### 原始类型签名

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

### 引用类型签名

引用数据类型包括类、接口、数组等，其中类和接口的类型签名都是以 `L` 开头，以 `;`
结尾，中间是类的全限定名，如 `Ljava/lang/String;`。

例如:

| 类型签名               | java中类型          |
|:-------------------|:-----------------|
| Ljava/lang/String; | java.lang.String |
| Ljava/util/List;   | java.util.List   |

### 数组类型签名

数组类型的类型签名以 `[` 开头，后面跟着数组元素的类型签名，如 `[[I` 表示一个二维数组，数组元素是 `int`。

例如:

| 类型签名                | java中类型定义          |
|:--------------------|:-------------------|
| [I                  | int[]              |
| [[C                 | char[][]           |
| [Ljava/lang/String; | java.lang.String[] |

### 方法签名

方法签名由方法的返回值类型签名和参数类型签名组成，如 `()V` 表示一个无参的 `void` 方法。

例如:
> 为了方便表述，所有的方法名都是 `function`

| 方法签名                    | java中方法定义                           |
|:------------------------|:------------------------------------|
| ()V                     | void function()                     |
| (I)V                    | void function(int)                  |
| (II)V                   | void function(int, int)             |
| (Ljava/lang/String;)V   | void function(java.lang.String)     |
| ([I)V                   | void function(int[])                |
| ([[Ljava/lang/String;)V | void function(java.lang.String[][]) |

## Dalvik 描述

那么在dex中我们应该通过怎样的方式表达一个特定的方法/字段呢？没错，就是 `Dalvik 描述`。

### 方法描述

方法描述的格式为 `[类签名]->[方法名][方法签名]`，如 `Ljava/lang/String;->length()I`。

> **注意**：
> 在 `Dalvik 描述` 中，构造函数的方法名为 `<init>`，静态初始化函数的方法名为 `<clinit>`。

### 字段描述

字段描述的格式为 `[类签名]->[字段名]:[类型签名]`，如 `Ljava/lang/String;->count:I`。

> **注意**: DexKit 查询参数支持填写 签名 或 java 原始写法，例如：
> 
> - 对 className 相关查询参数，可以填写 `Ljava/lang/String;` 或 `java.lang.String`， 而 `java/lang/String` 是不受支持的。
> 
> - 对 `fieldType`/`returnType` 相关查询参数，可以填写 `I` 或 `int`。