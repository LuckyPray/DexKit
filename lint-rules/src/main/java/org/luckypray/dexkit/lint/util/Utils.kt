package org.luckypray.dexkit.lint.util

import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.getIoFile
import java.util.Locale

internal val lang: String = runCatching { Locale.getDefault().language }.getOrElse { "en" }

internal fun loge(format: String, vararg args: Any?) {
    System.err.println(String.format(format, *args))
    System.out.flush()
}

internal fun JavaContext.log(title: String, explanation: String, node: UElement, location: Location) {
    loge(buildString {
        append(title)
        append(explanation)
        append("\n\t")
        append(node.getContainingUFile()?.getIoFile()?.absolutePath)
        append(":")
        append(location.start?.line?.let { it + 1 })
        append(":")
        append(location.start?.column?.let { it + 1 })
        append(": '${node.asSourceString()}'")
    })
}