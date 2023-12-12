# Performance Optimization

In DexKit, various queries may achieve the same functionality, but the difference in performance can
be significant, varying by several tens of times. This section will introduce some techniques for
performance optimization.

At the native layer, DexKit maintains lists of classes, methods, and fields in the Dex file. How
does DexKit scan these lists in several APIs? The traversal order of `findClass`, `findMethod`, and
`findField` is based on the respective lists' sequential order. Then, each condition is matched one
by one.

## declaredClass condition is too heavy

Some users may use the `declaredClass` condition to write queries like the following when using:

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

This search takes `4310ms`.

At first glance, this query seems fine, but in reality, its performance is very poor. Why? As
mentioned earlier, the `findMethod` API traverses all methods and then matches each condition one by
one. However, there is a many-to-one relationship between methods and classes, meaning a class may
contain multiple methods, but a method can only belong to one class. Therefore, during the process
of traversing all methods, each method will be matched once with the `declaredClass` condition,
leading to performance waste.

So, let's change our approach. By first searching for `declaredClass` and then using chain calls, we
can search for methods within the classes that meet the criteria. Won't this help avoid the issue?

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

This search takes `77ms`, showing a performance improvement by several tens of times.

When using `findMethod` or `findField`, the `declaredClass` condition should be avoided as much as
possible.
