package org.luckypray.dexkit;

import static org.luckypray.dexkit.LibLoader.loadLibrary;

import org.junit.Test;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.enums.MatchType;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.AnnotationElementMatcher;
import org.luckypray.dexkit.query.matchers.AnnotationMatcher;
import org.luckypray.dexkit.query.matchers.AnnotationsMatcher;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.FieldsMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.query.matchers.MethodsMatcher;
import org.luckypray.dexkit.result.ClassData;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.List;

public class JavaReadMeTest {

    private static DexKitBridge bridge;

    static {
        loadLibrary("dexkit");
        String path = System.getProperty("apk.path");
        File demoApk = new File(path, "demo.apk");
        bridge = DexKitBridge.create(demoApk.getAbsolutePath());
        assert bridge != null;
    }

    @Test
    public void testGetDexNum() {
        assert bridge.getDexNum() > 0;
    }

    @Test
    public void readmeEnTest() {
        ClassData classData = bridge.findClass(FindClass.create()
            // Search within the specified package name range
            .searchPackages("org.luckypray.dexkit.demo")
            // Exclude the specified package name range
            .excludePackages("org.luckypray.dexkit.demo.annotations")
            .matcher(ClassMatcher.create()
                // ClassMatcher Matcher for classes
                .className("org.luckypray.dexkit.demo.PlayActivity")
                // FieldsMatcher Matcher for fields in a class
                .fields(FieldsMatcher.create()
                    // Add a matcher for the field
                    .add(FieldMatcher.create()
                        // Specify the modifiers of the field
                        .modifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL)
                        // Specify the type of the field
                        .type("java.lang.String")
                        // Specify the name of the field
                        .name("TAG")
                    )
                    // Add a matcher for the field of the specified type
                    .addForType("android.widget.TextView")
                    .addForType("android.os.Handler")
                    // Specify the number of fields in the class
                    .count(3)
                )
                // MethodsMatcher Matcher for methods in a class
                .methods(MethodsMatcher.create()
                    // Add a matcher for the method
                    .methods(List.of(
                        MethodMatcher.create()
                            // Specify the modifiers of the method
                            .modifiers(Modifier.PROTECTED)
                            // Specify the name of the method
                            .name("onCreate")
                            // Specify the return type of the method
                            .returnType("void")
                            // Specify the parameter type of the method
                            .paramTypes("android.os.Bundle")
                            // Specify the strings used by the method
                            .usingStrings("onCreate"),
                        MethodMatcher.create()
                            .paramTypes("android.view.View")
                            // Specify the numbers used in the method, the type is Byte, Short, Int, Long, Float, Double
                            .usingNumbers(0.01, -1, 0.987, 0, 114514),
                        MethodMatcher.create()
                            .modifiers(Modifier.PUBLIC)
                            .paramTypes("boolean")
                            // Specify the methods called in the method list
                            .invokeMethods(MethodsMatcher.create()
                                .add(MethodMatcher.create()
                                    .modifiers(Modifier.PUBLIC | Modifier.STATIC)
                                    .returnType("int")
                                    // Specify the strings used in the method called in the method,
                                    .usingStrings(List.of("getRandomDice: "), StringMatchType.Equals)
                                )
                                // Only need to contain the call to the above method
                                .matchType(MatchType.Contains)
                            )
                    ))
                    // Specify the number of methods in the class, a minimum of 1, and a maximum of 10
                    .count(1, 10)
                )
                // AnnotationsMatcher Matcher for annotations in a class
                .annotations(AnnotationsMatcher.create()
                    // Add a matcher for the annotation
                    .add(AnnotationMatcher.create()
                        // Specify the type of the annotation
                        .type("org.luckypray.dexkit.demo.annotations.Router")
                        // The annotation needs to contain the specified element
                        .addElement(AnnotationElementMatcher.create()
                            // Specify the name of the element
                            .name("path")
                            // Specify the value of the element
                            .stringValue("/play")
                        )
                    )
                )
                // Strings used by all methods in the class
                .usingStrings("PlayActivity", "onClick", "onCreate")
            )
        ).firstOrThrow(() -> new IllegalStateException("No class found"));
        // Print the found class: org.luckypray.dexkit.demo.PlayActivity
        System.out.println(classData.getName());
        // Get the corresponding class instance
        // Class<?> clazz = classData.getInstance(loadPackageParam.classLoader);
    }

    @Test
    public void readmeZhTest() {
        ClassData classData = bridge.findClass(FindClass.create()
            // 指定搜索的包名范围
            .searchPackages("org.luckypray.dexkit.demo")
            // 排除指定的包名范围
            .excludePackages("org.luckypray.dexkit.demo.annotations")
            .matcher(ClassMatcher.create()
                // ClassMatcher 针对类的匹配器
                .className("org.luckypray.dexkit.demo.PlayActivity")
                // FieldsMatcher 针对类中包含字段的匹配器
                .fields(FieldsMatcher.create()
                    // 添加对于字段的匹配器
                    .add(FieldMatcher.create()
                        // 指定字段的修饰符
                        .modifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL)
                        // 指定字段的类型
                        .type("java.lang.String")
                        // 指定字段的名称
                        .name("TAG")
                    )
                    // 添加指定字段的类型的字段匹配器
                    .addForType("android.widget.TextView")
                    .addForType("android.os.Handler")
                    // 指定类中字段的数量
                    .count(3)
                )
                // MethodsMatcher 针对类中包含方法的匹配器
                .methods(MethodsMatcher.create()
                    // 添加对于方法的匹配器
                    .methods(List.of(
                        MethodMatcher.create()
                            // 指定方法的修饰符
                            .modifiers(Modifier.PROTECTED)
                            // 指定方法的名称
                            .name("onCreate")
                            // 指定方法的返回值类型
                            .returnType("void")
                            // 指定方法的参数类型，如果参数类型不确定，使用 null，使用此方法会隐式声明参数个数
                            .paramTypes("android.os.Bundle")
                            // 指定方法中使用的字符串
                            .usingStrings("onCreate"),
                        MethodMatcher.create()
                            .paramTypes("android.view.View")
                            // 指定方法中使用的数字，类型为 Byte, Short, Int, Long, Float, Double 之一
                            .usingNumbers(0.01, -1, 0.987, 0, 114514),
                        MethodMatcher.create()
                            .modifiers(Modifier.PUBLIC)
                            .paramTypes("boolean")
                            // 指定方法中调用的方法列表
                            .invokeMethods(MethodsMatcher.create()
                                .add(MethodMatcher.create()
                                    .modifiers(Modifier.PUBLIC | Modifier.STATIC)
                                    .returnType("int")
                                    // 指定方法中调用的方法中使用的字符串，所有字符串均使用 Equals 匹配
                                    .usingStrings(List.of("getRandomDice: "), StringMatchType.Equals)
                                )
                                // 只需要包含上述方法的调用即可
                                .matchType(MatchType.Contains)
                            )
                    ))
                    // 指定类中方法的数量，最少不少于1个，最多不超过10个
                    .count(1, 10)
                )
                // AnnotationsMatcher 针对类中包含注解的匹配器
                .annotations(AnnotationsMatcher.create()
                    // 添加对于注解的匹配器
                    .add(AnnotationMatcher.create()
                        // 指定注解的类型
                        .type("org.luckypray.dexkit.demo.annotations.Router")
                        // 该注解需要包含指定的 element
                        .addElement(AnnotationElementMatcher.create()
                            // 指定 element 的名称
                            .name("path")
                            // 指定 element 的值
                            .stringValue("/play")
                        )
                    )
                )
                // 类中所有方法使用的字符串
                .usingStrings("PlayActivity", "onClick", "onCreate")
            )
        ).firstOrThrow(() -> new IllegalStateException("No class found"));
        // 打印找到的类：org.luckypray.dexkit.demo.PlayActivity
        System.out.println(classData.getName());
        // 获取对应的类实例
        // Class<?> clazz = classData.getInstance(loadPackageParam.classLoader);
    }
}
