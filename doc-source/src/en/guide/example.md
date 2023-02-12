# Usage Example

Let's look at the following example app:

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

Now let's analyze what information is available:

- String constants: `Hello World`, `DemoActivity`, `sum: `
- Classes: `DemoActivity`, `Toast`, `AppCompatActivity`, `Log`, `Bundle`
- Methods: `sum()`, `onCreate()`, `onDestroy()`, `setContentView()`, `setText()`, `show()`, `i()`, `new Toast()`
- Fields: `mCount`, `TAG`

Now let's try to use `DexKit` to find this.

## Find classes use strings

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

We get the following output:

```text
DemoActivity -> ["com.luckypray.dexkit.demo.DemoActivity"]
NotFoundActivity -> []
```

## Find methods based on strings

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

We get the following output:

```text
all test pass
```

## Find the method that sets `mCount`

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

We get the following output:

```text
caller method: Lcom/luckypray/dexkit/demo/DemoActivity;->onCreate(Landroid/os/Bundle;)V ->
    Lcom/luckypray/dexkit/demo/DemoActivity;->mCount:I
```

## Find which methods are called by `onCreate`

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

We get the following output:

```text
method descriptor: Lcom/luckypray/dexkit/demo/DemoActivity;->onCreate(Landroid/os/Bundle;)V
    Landroidx/activity/ComponentActivity;->onCreate(Landroid/os/Bundle;)V
    Landroidx/activity/ComponentActivity;->setContentView(I)V
    Landroid/widget/Toast;-><init>(Landroid/content/Context;)V
    Landroid/widget/Toast;->setText(Ljava/lang/CharSequence;)V
    Landroid/widget/Toast;->show()V
    Lcom/luckypray/dexkit/demo/DemoActivity;->sum(II)I
```

## Find which methods a method is called by

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

We get the following output:

```text
caller method: Lcom/luckypray/dexkit/demo/DemoActivity;->onCreate(Landroid/os/Bundle;)V
    Lcom/luckypray/dexkit/demo/DemoActivity;->sum(II)I
```


> All APIs can be found in the [API documentation](https://luckypray.org/DexKit-Doc/dexkit/io.luckypray.dexkit/-dex-kit-bridge/index.html),
> where you can combine them to get the results you need.
