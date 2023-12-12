# 性能优化

在 DexKit 中，多种查询或许能实现同样的功能，但是性能差距却可能相差几十倍。本节将介绍一些性能优化的技巧。

在 native 层，DexKit 会维护 Dex 中的类、方法以及字段的列表，那么在几个 API 中，DexKit
是如何扫描这些列表的呢？`findClass`、`findMethod`、`findField` 
的遍历顺序均是按照各自列表的先后顺序进行遍历，然后再逐一对各个条件进行匹配。

## declaredClass 条件过重

可能有些用户在使用 `findMethod` 或 `findField` 时，会使用 `declaredClass` 条件写出如下的查询：

```kotlin
private fun badCode(bridge: DexKitBridge) {
    bridge.findMethod {
        matcher {
            declaredClass {
                usingStrings("getUid", "", "_event")
            }
            modifiers = Modifier.PUBLIC or Modifier.STATIC
            returnType = "long"
            addInvoke {
                name = "parseLong"
            }
            addInvoke {
                name = "toString"
            }
        }
    }.single().let {
        println(it)
    }
}
```

这个搜索耗时 `4310ms`。
乍一看这个查询似乎没有什么问题，但是实际上这个查询的性能是非常差。为什么？前面提到过，`findMethod` API
会遍历一遍所有的方法，然后再逐一对各个条件进行匹配。而 method 与 class 之间却是一个多对一的关系，即一个
class 中可能包含多个 method，但是一个 method 只能属于一个 class。因此，遍历所有方法的过程中，每个 method
都会被匹配一次 `declaredClass` 条件，这就导致了性能的浪费。

那么，我们换一个思路，先搜索 declaredClass，配合链式调用就能在符合条件的类中再搜索 method，这样不就可以避免了吗？

```kotlin
private fun goodCode(bridge: DexKitBridge) {
    bridge.findClass {
        matcher {
            usingStrings("getUid", "", "_event")
        }
    }.findMethod {
        matcher {
            modifiers = Modifier.PUBLIC or Modifier.STATIC
            returnType = "long"
            addInvoke {
                name = "parseLong"
            }
            addInvoke {
                name = "toString"
            }
        }
    }.single().let {
        println(it)
    }
}
```

这个搜索耗时 `77ms`, 性能提升了数十倍之多。

在使用 `findMethod` 或 `findField` 时，尽量避免使用 `declaredClass` 附带过于复杂的逻辑。
