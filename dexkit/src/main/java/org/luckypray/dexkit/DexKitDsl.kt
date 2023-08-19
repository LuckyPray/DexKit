package org.luckypray.dexkit

import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Modifier

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@DslMarker
annotation class DexKitDsl


fun main() {
    val bridge = DexKitBridge.create("")!!


    bridge.findClass {
        searchPackage("com.android.systemui")
        uniqueResult(true)
        searchInClass(listOf())
        matcher {
            sourceName("smaliSource")
            className("className")
            modifiers(Modifier.PUBLIC or Modifier.FINAL or Modifier.STATIC)
            superClass("superClassName")
            superClass {
                className("superClassName")
                modifiers(Modifier.ABSTRACT, MatchType.Contains)
                methods {
                    add {
                        modifiers(Modifier.PUBLIC)
                        returnType("void")
                        name("methodName")
                    }
                }
            }
            interfaces {
                add {
                    className("interfaceName")
                }
                add("interfaceName1", StringMatchType.Contains, true)
            }
            usingStrings("A123", "abc")
            usingStringsMatcher {
                add("A123", StringMatchType.EndWith)
                add("abc", StringMatchType.Contains, true)
                add {
                    value("abc")
                }
            }
        }
    }.findMethod {

    }
}