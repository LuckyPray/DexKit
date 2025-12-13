package org.luckypray.dexkit.util

import java.lang.reflect.Field
import java.lang.reflect.Member

object NativeReflect {

    /**
     * Retrieve a reflected [Field] object using native lookup.
     * ----------------
     * 通过 Native 查找并获取反射 [Field] 对象。
     *
     *     // Get instance field "value" from String
     *     getReflectedField(String::class.java, "value", "[B")
     *
     *     // Get static field "CASE_INSENSITIVE_ORDER" from String
     *     getReflectedField(String::class.java, "CASE_INSENSITIVE_ORDER", "Ljava/util/Comparator;")
     *
     * @param declaringClass declared class / 声明该字段的类
     * @param name field name / 字段名称
     * @param jniSig type signature of the field / 字段的类型签名
     * @param isStatic If null, native auto check / 如果为 null，native 自动判断
     * @return The reflected [Field] object, or null if not found / 反射的 [Field] 对象，如果未找到则返回 null
     */
    @JvmStatic
    external fun getReflectedField(
        declaringClass: Class<*>,
        name: String,
        jniSig: String,
        isStatic: Boolean? = null
    ): Field?

    /**
     * Retrieve a reflected [java.lang.reflect.Method] / [java.lang.reflect.Constructor] object using native lookup.
     * ----------------
     * 通过 Native 查找并获取反射 [java.lang.reflect.Method] / [java.lang.reflect.Constructor] 对象。
     *
     *     // Get instance method "substring(int)" from String
     *     getReflectedMethod(String::class.java, "substring", "(I)Ljava/lang/String;")
     *
     *     // Get static method "valueOf(int)" from String
     *     getReflectedMethod(String::class.java, "valueOf", "(I)Ljava/lang/String;")
     *
     * @param declaringClass declared class / 声明该方法的类
     * @param name method name / 方法名称
     * @param jniSig method signature / 方法签名
     * @param isStatic If null, native auto check / 如果为 null，native 自动判断
     * @return The reflected [Member] object, or null if not found / 反射的 [Member] 对象，如果未找到则返回 null
     */
    @JvmStatic
    external fun getReflectedMethod(
        declaringClass: Class<*>,
        name: String,
        jniSig: String,
        isStatic: Boolean? = null
    ): Member?
}