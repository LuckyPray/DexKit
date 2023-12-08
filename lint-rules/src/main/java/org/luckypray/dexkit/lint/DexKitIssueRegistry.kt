package org.luckypray.dexkit.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class DexKitIssueRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = listOf(
            DexKitCreateDetector.ISSUE,
            DexKitDataFirstDetector.ISSUE,
        )
    override val minApi: Int
        get() = 9

    override val api: Int
        get() = CURRENT_API

    override val vendor: Vendor
        get() = Vendor(
            vendorName = "LuckyPray",
            identifier = "org.luckypray.dexkit:lint-rules",
            feedbackUrl = "https://github.com/LuckyPray/DexKit/issues",
            contact = "https://github.com/LuckyPray/DexKit"
        )
}