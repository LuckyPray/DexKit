@file:JvmName("LibLoader")

package org.luckypray.dexkit

import java.util.Locale

val isWindows
    get() = System.getProperty("os.name")
        .lowercase(Locale.getDefault())
        .contains("windows")

fun loadLibrary(name: String) {
    System.loadLibrary(if (isWindows) "lib$name" else name)
}