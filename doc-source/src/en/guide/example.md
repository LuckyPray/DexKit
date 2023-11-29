# Usage Examples

During the reading of this section, you may need to refer to the [Structural Quick Reference Table](./structural-zoom-table) for a better understanding.

> You can get the source code and some test cases for the demo below.

- APP Demo [Click to View](https://github.com/LuckyPray/DexKit/tree/master/demo)
- APP Test Cases [Click to View](https://github.com/LuckyPray/DexKit/blob/master/dexkit/src/test/java/org/luckypray/dexkit/UnitTest.kt)

## Demo App

Here is a simple Demo Activity, PlayActivity, where the internal properties and methods are obfuscated, and they change in each version.

> The four major components are not obfuscated by default. We assume this Activity is obfuscated.


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

## Multiple Conditions Matching Class - Example Usage

In this scenario, we want to find the PlayActivity using the following code:

> This is just an example. In actual use, you don't need conditions as complex and comprehensive 
> as this. Use as needed.

```kotlin
private fun findPlayActivity(bridge: DexKitBridge) {
    val classData = bridge.findClass {
        // Search within the specified package name range
        searchPackages("org.luckypray.dexkit.demo")
        // Exclude the specified package name range
        excludePackages("org.luckypray.dexkit.demo.annotations")
        // ClassMatcher Matcher for classes
        matcher {
            // FieldsMatcher Matcher for fields in a class
            fields {
                // Add a matcher for the field
                add {
                    // Specify the modifiers of the field
                    modifiers = Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL
                    // Specify the type of the field
                    type = "java.lang.String"
                    // Specify the name of the field
                    name = "TAG"
                }
                // Add a matcher for the field of the specified type
                addForType("android.widget.TextView")
                addForType("android.os.Handler")
                // Specify the number of fields in the class
                count = 3
            }
            // MethodsMatcher Matcher for methods in a class
            methods {
                // Add a matcher for the method
                add {
                    // Specify the modifiers of the method
                    modifiers = Modifier.PROTECTED
                    // Specify the name of the method
                    name = "onCreate"
                    // Specify the return type of the method
                    returnType = "void"
                    // Specify the parameter types of the method, if the parameter types are uncertain,
                    // use null, and this method will implicitly declare the number of parameters
                    paramTypes("android.os.Bundle")
                    // Specify the strings used in the method
                    usingStrings("onCreate")
                }
                add {
                    paramTypes("android.view.View")
                    // Specify the numbers used in the method, the type is Byte, Short, Int, Long, Float, Double
                    usingNumbers(0.01, -1, 0.987, 0, 114514)
                }
                add {
                    paramTypes("boolean")
                    // Specify the methods called in the method list
                    invokeMethods {
                        add {
                            modifiers = Modifier.PUBLIC or Modifier.STATIC
                            returnType = "int"
                            // Specify the strings used in the method called in the method,
                            usingStrings(listOf("getRandomDice: "), StringMatchType.Equals)
                        }
                        // Only need to contain the call to the above method
                        matchType = MatchType.Contains
                    }
                }
                count(1..10)
            }
            // AnnotationsMatcher Matcher for annotations in a class
            annotations {
                // Add a matcher for the annotation
                add {
                    // Specify the type of the annotation
                    type = "org.luckypray.dexkit.demo.annotations.Router"
                    // The annotation needs to contain the specified element
                    addElement {
                        // Specify the name of the element
                        name = "path"
                        // Specify the value of the element
                        stringValue("/play")
                    }
                }
            }
            // Strings used by all methods in the class
            usingStrings("PlayActivity", "onClick", "onCreate")
        }
    }.single()
    println(classData.name)
}
```

The result is as follows:

```text
org.luckypray.dexkit.demo.PlayActivity
```

## Parent Class Condition Nesting

if there is such a class, its only feature is that the ancestors are not obfuscated, 
and the middle parents are all obfuscated, we can also use `DexKit` to find it.

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

The result is as follows:

```text
org.luckypray.dexkit.demo.MainActivity
org.luckypray.dexkit.demo.PlayActivity
```

::: tip
In `DexKit`, any logical relationship can be used as a query condition
:::
