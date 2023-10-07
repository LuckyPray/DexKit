package org.luckypray.dexkit;

import static org.luckypray.dexkit.LibLoader.loadLibrary;

import org.junit.Test;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.AnnotationElementMatcher;
import org.luckypray.dexkit.query.matchers.AnnotationMatcher;
import org.luckypray.dexkit.query.matchers.AnnotationsMatcher;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.FieldsMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.query.matchers.MethodsMatcher;

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
        bridge.findClass(FindClass.create()
            // Search within the specified package name range
            .searchPackages("org.luckypray.dexkit.demo")
            // Exclude the specified package name range
            .excludePackages("org.luckypray.dexkit.demo.annotations")
            .matcher(ClassMatcher.create()
                // ClassMatcher for class matching
                .className("org.luckypray.dexkit.demo.PlayActivity")
                // FieldsMatcher for matching properties within the class
                .fields(FieldsMatcher.create()
                    // Add a matcher for properties
                    .add(FieldMatcher.create()
                        .modifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL)
                        .type("java.lang.String")
                        .name("TAG")
                    )
                    .addForType("android.widget.TextView")
                    .addForType("android.os.Handler")
                    // Specify the number of properties in the class
                    .count(3)
                )
                // MethodsMatcher for matching methods within the class
                .methods(MethodsMatcher.create()
                    // Add a matcher for methods
                    .methods(List.of(
                        MethodMatcher.create()
                            .modifiers(Modifier.PROTECTED)
                            .name("onCreate")
                            .returnType("void")
                            .paramTypes("android.os.Bundle")
                            .usingStrings("onCreate"),
                        MethodMatcher.create()
                            .paramTypes("android.view.View")
                            .usingNumbers(0.01, -1, 0.987, 0, 114514),
                        MethodMatcher.create()
                            .modifiers(Modifier.PUBLIC)
                            .paramTypes("boolean")
                    ))
                    // Specify the number of methods in the class, a minimum of 1, and a maximum of 10
                    .count(1, 10)
                )
                // AnnotationsMatcher for matching annotations within the class
                .annotations(AnnotationsMatcher.create()
                    .add(AnnotationMatcher.create()
                        .type("org.luckypray.dexkit.demo.annotations.Router")
                        .addElement(
                            AnnotationElementMatcher.create()
                                .name("path")
                                .stringValue("/play")
                        )
                    )
                )
                // Strings used by all methods in the class
                .usingStrings("PlayActivity", "onClick", "onCreate")
            )
        ).forEach(classData -> {
            // Print the found class: org.luckypray.dexkit.demo.PlayActivity
            System.out.println(classData.getClassName());
            // Get the corresponding class instance
//            Class<?> clazz = classData.getInstance(loadPackageParam.classLoader);
        });
    }

    @Test
    public void readmeZhTest() {
        bridge.findClass(FindClass.create()
            // 从指定的包名范围内进行查找
            .searchPackages("org.luckypray.dexkit.demo")
            // 排除指定的包名范围
            .excludePackages("org.luckypray.dexkit.demo.annotations")
            .matcher(ClassMatcher.create()
                // ClassMatcher 针对类的匹配器
                .className("org.luckypray.dexkit.demo.PlayActivity")
                // FieldsMatcher 针对类中包含属性的匹配器
                .fields(FieldsMatcher.create()
                    // 添加对于属性的匹配器
                    .add(FieldMatcher.create()
                        .modifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL)
                        .type("java.lang.String")
                        .name("TAG")
                    )
                    .addForType("android.widget.TextView")
                    .addForType("android.os.Handler")
                    // 指定类中属性的数量
                    .count(3)
                )
                // MethodsMatcher 针对类中包含方法的匹配器
                .methods(MethodsMatcher.create()
                    // 添加对于方法的匹配器
                    .methods(List.of(
                        MethodMatcher.create()
                            .modifiers(Modifier.PROTECTED)
                            .name("onCreate")
                            .returnType("void")
                            .paramTypes("android.os.Bundle")
                            .usingStrings("onCreate"),
                        MethodMatcher.create()
                            .paramTypes("android.view.View")
                            .usingNumbers(0.01, -1, 0.987, 0, 114514),
                        MethodMatcher.create()
                            .modifiers(Modifier.PUBLIC)
                            .paramTypes("boolean")
                    ))
                    // 指定类中方法的数量，最少不少于1个，最多不超过10个
                    .count(1, 10)
                )
                // AnnotationsMatcher 针对类中包含注解的匹配器
                .annotations(AnnotationsMatcher.create()
                    .add(AnnotationMatcher.create()
                        .type("org.luckypray.dexkit.demo.annotations.Router")
                        .addElement(
                            AnnotationElementMatcher.create()
                                .name("path")
                                .stringValue("/play")
                        )
                    )
                )
                // 类中所有方法使用的字符串
                .usingStrings("PlayActivity", "onClick", "onCreate")
            )
        ).forEach(classData -> {
            // 打印查找到的类: org.luckypray.dexkit.demo.PlayActivity
            System.out.println(classData.getClassName());
            // 获取对应的类实例
//            Class<?> clazz = classData.getInstance(loadPackageParam.classLoader);
        });
    }
}
