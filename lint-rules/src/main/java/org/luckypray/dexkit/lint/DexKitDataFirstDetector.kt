package org.luckypray.dexkit.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import java.util.Locale

class DexKitDataFirstDetector : Detector(), Detector.UastScanner {
    companion object {
        private val lang: String = runCatching { Locale.getDefault().language }.getOrElse { "en" }
        val title = when (lang) {
            "zh" -> "结果可能不唯一！"
            else -> "The result may not be unique! "
        }
        val explanation = when (lang) {
            "zh" -> "可能会导致预期外的问题，不推荐使用。"
            else -> "May cause unexpected problems and are not recommended"
        }

        private const val ISSUE_ID = "DataMayNonUnique"
        val ISSUE = Issue.create(
            ISSUE_ID,
            title,
            explanation,
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.WARNING,
            implementation = Implementation(
                DexKitDataFirstDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            ),
        )

        val fixMap = mutableMapOf(
            "firstOrNull" to "singleOrNull",
            "first" to "single",
            "firstOrThrow" to "singleOrThrow",
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        return fixMap.keys.toList()
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator.isMemberInClass(method, "org.luckypray.dexkit.result.BaseDataList")) {
            val replaceFix = fix()
                .name("Replace with '${fixMap[method.name]}'")
                .replace()
                .text(method.name)
                .with(fixMap[method.name]!!)
                .build()
            context.report(
                ISSUE,
                node,
                context.uastParser.getCallLocation(
                    context,
                    node,
                    includeReceiver = false,
                    includeArguments = false
                ),
                title + explanation,
                replaceFix
            )
        }
    }
}