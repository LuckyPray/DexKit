package org.luckypray.dexkit.lint.detector

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.luckypray.dexkit.lint.util.lang
import org.luckypray.dexkit.lint.util.log

class DexKitDataFirstDetector : Detector(), Detector.UastScanner {
    companion object {
        val title = when (lang) {
            "zh" -> "返回结果不唯一！"
            else -> "The result is not unique! "
        }
        val explanation = when (lang) {
            "zh" -> "可能会导致预期外的问题，不推荐使用。"
            else -> "May cause unexpected problems and are not recommended."
        }

        private const val ISSUE_ID = "NonUniqueDexKitData"
        val ISSUE = Issue.create(
            ISSUE_ID,
            title,
            explanation,
            category = Category.CORRECTNESS,
            priority = 5,
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
            val location = context.uastParser.getCallLocation(
                context,
                node,
                includeReceiver = false,
                includeArguments = false
            )
            context.report(
                ISSUE,
                node,
                location,
                title + explanation,
                replaceFix
            )
            context.log(title, explanation, node, location)
        }
    }
}