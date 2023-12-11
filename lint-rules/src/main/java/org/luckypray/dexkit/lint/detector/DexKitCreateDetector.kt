package org.luckypray.dexkit.lint.detector

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
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


class DexKitCreateDetector : Detector(), Detector.UastScanner {
    companion object {
        val title = when (lang) {
            "zh" -> "请不要重复创建相同的 DexKitBridge 对象！"
            else -> "Don't create the same DexKitBridge object repeatedly! "
        }
        val explanation = when (lang) {
            "zh" -> "DexKitBridge 的创建是一个耗时操作，应该全局维护一个对象，而不是每次搜索都调用 `create()` 方法。"
            else -> "Creating DexKitBridge is a time-consuming operation, and you should maintain a global object instead of calling `create()` every time you search."
        }

        private const val ISSUE_ID = "DuplicateCreateDexKit"
        val ISSUE = Issue.create(
            ISSUE_ID,
            title,
            explanation,
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.WARNING,
            implementation = Implementation(
                DexKitCreateDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            ),
        )

        val createParamSet = mutableSetOf<String>()
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf("create")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator.isMemberInClass(method, "org.luckypray.dexkit.DexKitBridge.Companion")
            || context.evaluator.isMemberInClass(method, "org.luckypray.dexkit.DexKitBridge")) {
            val sourceString = node.asSourceString()
            val params = sourceString.substring(sourceString.indexOf('(') + 1, sourceString.length - 1)
            if (createParamSet.contains(params)) {
                val location = context.getLocation(node)
                context.report(ISSUE, node, location, title + explanation)
                context.log(title, explanation, node, location)
            } else {
                createParamSet.add(params)
            }
        }
    }

    override fun beforeCheckRootProject(context: Context) {
        createParamSet.clear()
    }

}