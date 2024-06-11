# 用法示例

在阅读本部分内容的过程中可能需要搭配[结构速查表](./structural-zoom-table)以获得更好的理解。

> 您可以在下方获取 Demo 的源码以及部分测试用例

- APP Demo [点击查看](https://github.com/LuckyPray/DexKit/tree/master/demo)
- APP 测试用例 [点击查看](https://github.com/LuckyPray/DexKit/blob/master/dexkit/src/test/java/org/luckypray/dexkit/UnitTest.kt)

## Demo App

下面是一个简单的 Demo Activity。这个 PlayActivity 内部属性以及方法都是被混淆的，且每个版本都会发生变化。

> 四大组件默认不会混淆，我们假设这个 Activity 被混淆了。

```java
package org.luckypray.dexkit.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.h;
import java.util.Random;
import org.luckypray.dexkit.demo.annotations.Router;

@Router(path = "/play")
public class PlayActivity extends AppCompatActivity {
    private static final String TAG = "PlayActivity";
    private TextView a;
    private Handler b;

    public void d(View view) {
        Handler handler;
        int i;
        Log.d("PlayActivity", "onClick: rollButton");
        float nextFloat = new Random().nextFloat();
        if (nextFloat < 0.01d) {
            handler = this.b;
            i = -1;
        } else if (nextFloat < 0.987f) {
            handler = this.b;
            i = 0;
        } else {
            handler = this.b;
            i = 114514;
        }
        handler.sendEmptyMessage(i);
    }

    public void e(boolean z) {
        int i;
        if (!z) {
            i = RandomUtil.a();
        } else {
            i = 6;
        }
        String a = h.a("You rolled a ", i);
        this.a.setText(a);
        Log.d("PlayActivity", "rollDice: " + a);
    }

    protected void onCreate(Bundle bundle) {
        super/*androidx.fragment.app.FragmentActivity*/.onCreate(bundle);
        setContentView(0x7f0b001d);
        Log.d("PlayActivity", "onCreate");
        HandlerThread handlerThread = new HandlerThread("PlayActivity");
        handlerThread.start();
        this.b = new PlayActivity$1(this, handlerThread.getLooper());
        this.a = (TextView) findViewById(0x7f080134);
        ((Button) findViewById(0x7f08013a)).setOnClickListener(new a(this));
    }
}
```

## 多条件匹配类多条件用法示例

此时我们想得到这个 PlayActivity 可以使用如下代码：

> 这仅仅是个样例，实际使用中并不需要这么复杂且全面的条件，按需使用即可。

```kotlin
private fun findPlayActivity(bridge: DexKitBridge) {
    val classData = bridge.findClass {
        // 指定搜索的包名范围
        searchPackages("org.luckypray.dexkit.demo")
        // 排除指定的包名范围
        excludePackages("org.luckypray.dexkit.demo.annotations")
        // ClassMatcher 针对类的匹配器
        matcher {
            // FieldsMatcher 针对类中包含字段的匹配器
            fields {
                // 添加对于字段的匹配器
                add {
                    // 指定字段的修饰符
                    modifiers = Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL
                    // 指定字段的类型
                    type = "java.lang.String"
                    // 指定字段的名称
                    name = "TAG"
                }
                // 添加指定字段的类型的字段匹配器
                addForType("android.widget.TextView")
                addForType("android.os.Handler")
                // 指定类中字段的数量
                count = 3
            }
            // MethodsMatcher 针对类中包含方法的匹配器
            methods {
                // 添加对于方法的匹配器
                add {
                    // 指定方法的修饰符
                    modifiers = Modifier.PROTECTED
                    // 指定方法的名称
                    name = "onCreate"
                    // 指定方法的返回值类型
                    returnType = "void"
                    // 指定方法的参数类型，如果参数类型不确定，使用 null，使用此方法会隐式声明参数个数
                    paramTypes("android.os.Bundle")
                    // 指定方法中使用的字符串
                    usingStrings("onCreate")
                }
                add {
                    paramTypes("android.view.View")
                    // 指定方法中使用的数字，类型为 Byte, Short, Int, Long, Float, Double 之一
                    usingNumbers(0.01, -1, 0.987, 0, 114514)
                }
                add {
                    paramTypes("boolean")
                    // 指定方法中调用的方法列表
                    invokeMethods {
                        add {
                            modifiers = Modifier.PUBLIC or Modifier.STATIC
                            returnType = "int"
                            // 指定方法中调用的方法中使用的字符串，所有字符串均使用 Equals 匹配
                            usingStrings(listOf("getRandomDice: "), StringMatchType.Equals)
                        }
                        // 只需要包含上述方法的调用即可
                        matchType = MatchType.Contains
                    }
                }
                // 指定类中方法的数量，最少不少于1个，最多不超过10个
                count(1..10)
            }
            // AnnotationsMatcher 针对类中包含注解的匹配器
            annotations {
                // 添加对于注解的匹配器
                add {
                    // 指定注解的类型
                    type = "org.luckypray.dexkit.demo.annotations.Router"
                    // 该注解需要包含指定的 element
                    addElement {
                        // 指定 element 的名称
                        name = "path"
                        // 指定 element 的值
                        stringValue("/play")
                    }
                }
            }
            // 类中所有方法使用的字符串
            usingStrings("PlayActivity", "onClick", "onCreate")
        }
    }.single()
    println(classData.name)
}
```

获得的结果如下：

```text
org.luckypray.dexkit.demo.PlayActivity
```

## 父类条件嵌套

如果存在这么一个类，它唯一的特征是祖辈没被混淆，中间的父类都被混淆了，我们也可以使用 `DexKit` 来找到它。

```kotlin
private fun findActivity(bridge: DexKitBridge) {
    bridge.findClass {
        matcher { // ClassMatcher
            // androidx.appcompat.app.AppCompatActivity
            superClass { // ClassMatcher
                // androidx.fragment.app.FragmentActivity
                superClass { // ClassMatcher
                    // androidx.activity.ComponentActivity
                    superClass { // ClassMatcher
                        // androidx.core.app.ComponentActivity
                        superClass { // ClassMatcher
                            superClass = "android.app.Activity"
                        }
                    }
                }
            }
        }
    }.forEach {
        // org.luckypray.dexkit.demo.MainActivity
        // org.luckypray.dexkit.demo.PlayActivity
        println(it.name)
    }
}
```

获得的结果如下：

```text
org.luckypray.dexkit.demo.MainActivity
org.luckypray.dexkit.demo.PlayActivity
```

::: tip
在 `DexKit` 中，一切符合逻辑的关系都可以作为查询条件
:::

## 模糊参数匹配

如果我们需要寻找的方法中存在一个被混淆的参数，我们可以使用 `null` 来替代，这样它就能匹配任意类型的参数。

```kotlin
private fun findMethodWithFuzzyParam(bridge: DexKitBridge) {
    bridge.findMethod {
        matcher {
            modifiers = Modifier.PUBLIC or Modifier.STATIC
            returnType = "void"
            // 指定方法的参数类型，如果参数类型不确定，使用 null
            paramTypes("android.view.View", null)
            // paramCount = 2 // paramTypes 长度为 2 已经隐式确定了参数个数
            usingStrings("onClick")
        }
    }.single().let {
        println(it)
    }
}
```

## 查询结果保存与读取

使用 DexKit 查询到的结果如何序列化保存下来，以便下次使用呢？

DexKit 中对 Class、Method、Field 提供了相应的包装类，分别是 `DexClass`、`DexMethod`、`DexField`。
包装类继承了 `ISerializable` 接口，可以使用它将包装类与字符串自由转换。对于查询返回的对象，可以直接使用 
`toDexClass()`、`toDexMethod()`、`toDexField()` 方法来转换为包装类。

```kotlin
private fun saveData(bridge: DexKitBridge) {
    bridge.findMethod {
        matcher {
            modifiers = Modifier.PUBLIC or Modifier.STATIC
            returnType = "void"
            paramTypes("android.view.View", null)
            usingStrings("onClick")
        }
    }.single().let {
        val dexMethod = it.toDexMethod()
        val serialize = dexMethod.serialize()
        val sp = getSharedPreferences("dexkit", Context.MODE_PRIVATE)
        sp.edit().putString("onClickMethod", serialize).apply()
    }
}

private fun readData(): Method {
    val sp = getSharedPreferences("dexkit", Context.MODE_PRIVATE)
    val descriptor = sp.getString("onClickMethod", null)
    if (descriptor != null) {
        val dexMethod = DexMethod(descriptor)
        // val dexMethod = DexMethod.deserialize(serialize)
        // val dexMethod = ISerializable.deserialize(serialize) as DexMethod
        // val dexMethod = ISerializable.deserializeAs<DexMethod>(serialize)
        val method = dexMethod.getMethodInstance(hostClassLoader)
        return method
    }
    error("No saved")
}
```
