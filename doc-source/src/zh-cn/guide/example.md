# 使用示例

现在我们有这样一个示例 APP:

```java
package com.luckypray.dexkit.demo;
// ...

public class DemoActivity extends ComponentActivity {
    
    private final static String TAG = "DemoActivity";
    
    private int mCount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        Toast toast = new Toast(this);
        toast.setText("Hello World");
        toast.show();
        mCount = sum(1, 2);
    }

    private int sum(int a, int b) {
        int c =  a + b;
        Log.i(TAG, "sum: " + c);
        return c;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
```

现在我们来分析一下有哪些信息:

- 字符串常量: `Hello World`, `DemoActivity`, `sum: `
- 类: `DemoActivity`, `Toast`, `AppCompatActivity`, `Log`, `Bundle`
- 方法: `sum()`, `onCreate()`, `onDestroy()`, `setContentView()`, `setText()`, `show()`, `i()`, `new Toast()`
- 字段: `mCount`, `TAG`

现在我们来尝试一下使用 `DexKit` 来查找这些信息。

## 查找使用字符串的类

```kotlin
DexKitBridge.create("demo.apk")?.use { bridge->
    val deobfMap = mapOf(
        "DemoActivity" to setOf("DemoActivity", "Hello World", "sum: "),
        "NotFoundActivity" to setOf("DemoActivity", "Hello World", "sum: ", "not found"),
    )
    val result = bridge.batchFindClassesUsingStrings {
        queryMap(deobfMap)
        advancedMatch = MatchType.FULL
    }
    assert(result.size == 2)
    result.forEach { (tagName, searchList) ->
        println("$tagName -> [${searchList.joinToString(", ", "\"", "\"")}]")
    }
}
```

我们得到如下输出:

```text
DemoActivity -> ["com.luckypray.dexkit.demo.DemoActivity"]
NotFoundActivity -> []
```

## 查找使用字符串的方法

```kotlin
DexKitBridge.create("demo.apk")?.use { bridge ->
    listOf("^Hello", "^Hello World", "World$", "Hello World", "llo Wor").forEach { usingString ->
        val result = bridge.findMethodUsingString {
            usingString = usingString
            matchType = MatchType.SIMILAR_REGEX
        }
        assert(result.size == 1)
        assert(result.first().descriptor == "Lcom/luckypray/dexkit/demo/DemoActivity;->onCreate(Landroid/os/Bundle;)V")
    }
    println("all test pass")
}
```

我们得到如下输出:

```text
all test pass
```

## 查找对 `mCount` 字段进行赋值的方法

```kotlin
DexKitBridge.create("demo.apk")?.use { bridge ->
    val result = bridge.findMethodUsingField {
        this.fieldDeclareClass = "Lcom/luckypray/dexkit/demo/DemoActivity;"
        this.fieldName = "mCount"
        this.fieldType = "int"
        this.usingType = FieldUsingType.PUT
    }
    assert(result.size == 1)
    result.forEach { (method, fieldList) ->
        println("method: $method ->")
        fieldList.forEach { field ->
            println("\t $field")
        }
    }
}
```

我们得到如下输出:

```text
method: Lcom/luckypray/dexkit/demo/DemoActivity;->onCreate(Landroid/os/Bundle;)V ->
    Lcom/luckypray/dexkit/demo/DemoActivity;->mCount:I
```

## 查找哪些方法被 `onCreate` 调用

```kotlin
DexKitBridge.create("demo.apk")?.use { bridge ->
    val result = bridge.findMethodInvoking {
        this.methodDescriptor = "Lcom/luckypray/dexkit/demo/DemoActivity;->onCreate(Landroid/os/Bundle;)V"
    }
    result.forEach { (methodName, invokingList)->
        println("method descriptor: $methodName")
        invokingList.forEach {
            println("\t$it")
        }
    }
}
```

我们得到如下输出:

```text
method descriptor: Lcom/luckypray/dexkit/demo/DemoActivity;->onCreate(Landroid/os/Bundle;)V
    Landroidx/activity/ComponentActivity;->onCreate(Landroid/os/Bundle;)V
    Landroidx/activity/ComponentActivity;->setContentView(I)V
    Landroid/widget/Toast;-><init>(Landroid/content/Context;)V
    Landroid/widget/Toast;->setText(Ljava/lang/CharSequence;)V
    Landroid/widget/Toast;->show()V
    Lcom/luckypray/dexkit/demo/DemoActivity;->sum(II)I
```

## 查找方法被哪些方法调用

```kotlin
DexKitBridge.create("demo.apk")?.use { bridge ->
    val result = bridge.findMethodCaller {
        methodReturnType = "int"
        methodParameterTypes = arrayOf("int", "I")
        methodDeclareClass = "com.luckypray.dexkit.demo.DemoActivity"
    }
    result.forEach { (callerMethod, beInvokedList)->
        println("caller method: $callerMethod")
        beInvokedList.forEach {
            println("\t$it")
        }
    }
}
```

我们得到如下输出:

```text
caller method: Lcom/luckypray/dexkit/demo/DemoActivity;->onCreate(Landroid/os/Bundle;)V
    Lcom/luckypray/dexkit/demo/DemoActivity;->sum(II)I
```

> 所有API均可在 [API 文档](https://luckypray.org/DexKit-Doc/dexkit/io.luckypray.dexkit/-dex-kit-bridge/index.html)
> 中进行查阅，你可以对API进行组合调用以获取你需要的结果。