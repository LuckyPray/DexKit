package io.luckypray.dexkit;

import static org.luckypray.dexkit.query.matchers.base.AnnotationEncodeValueMatcher.createString;
import static io.luckypray.dexkit.util.LibLoader.loadLibrary;

import org.junit.Test;
import org.luckypray.dexkit.DexKitBridge;
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
    public void readmeTest() {
        bridge.findClass(FindClass.create()
                // Search within the specified package name range
                .searchPackages("org.luckypray.dexkit.demo")
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
                                                .usingNumbers(114514, 0.987f),
                                        MethodMatcher.create()
                                                .modifiers(Modifier.PUBLIC)
                                                .paramTypes("boolean")
                                ))
                                // Specify the number of methods in the class, a minimum of 4, and a maximum of 10
                                .count(1, 10)
                        )
                        // AnnotationsMatcher for matching interfaces within the class
                        .annotations(AnnotationsMatcher.create()
                                .add(AnnotationMatcher.create()
                                        .type("org.luckypray.dexkit.demo.annotations.Router")
                                        .addElement(
                                                AnnotationElementMatcher.create()
                                                        .name("path")
                                                        .value(createString("/play"))
                                        )
                                )
                        )
                        // Strings used by all methods in the class
                        .usingStrings("PlayActivity", "onClick", "onCreate")
                )
        ).forEach(classData -> {
            // Print the found class: org.luckypray.dexkit.demo.PlayActivity
            System.out.println(classData.getClassName());
//            Class<?> clazz = classData.getInstance(loadPackageParam.classLoader);
        });
    }
}
