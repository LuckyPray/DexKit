package org.luckypray.dexkit.util

import org.junit.Test

class InstanceUtilTest {

    companion object {
        val classLoader = InstanceUtilTest::class.java.classLoader

        @JvmStatic
        fun getType(typeName: String): Class<*> {
            return InstanceUtil.getClassInstance(classLoader, typeName)
        }
    }

    @Test
    fun getTypeInstance() {
        assert(getType("java.lang.String").typeName == "java.lang.String")
        assert(getType("java.lang.String[]").typeName == "java.lang.String[]")
        assert(getType("java.lang.String[][]").typeName == "java.lang.String[][]")
        assert(getType("int").typeName == "int")
        assert(getType("int[]").typeName == "int[]")
        assert(getType("void").typeName == "void")
    }
}