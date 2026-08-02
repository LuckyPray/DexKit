package org.luckypray.dexkit.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.luckypray.dexkit.DexKitCacheBridge
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindMethod

@OptIn(DexKitExperimentalApi::class)
class CacheBridgeKeysTest {

    @Test
    fun keyContainsTypeAndResultShape() {
        assertEquals(
            "dkcb:app:c:s:user:target",
            CacheBridgeKeys.cacheKeyOf(
                appTag = "app",
                queryKind = DexKitCacheBridge.QueryKind.CLASS_SINGLE,
                key = "target"
            )
        )
        assertEquals(
            "dkcb:app:m:l:user:target",
            CacheBridgeKeys.cacheKeyOf(
                appTag = "app",
                queryKind = DexKitCacheBridge.QueryKind.METHOD_LIST,
                key = "target"
            )
        )
    }

    @Test
    fun queryTypeSeparatesEqualFinderHashes() {
        val classQuery = FindClass().searchPackages("org.example")
        val methodQuery = FindMethod().searchPackages("org.example")
        assertEquals(classQuery.hashKey(), methodQuery.hashKey())

        assertNotEquals(
            CacheBridgeKeys.cacheKeyOf(
                appTag = "app",
                queryKind = DexKitCacheBridge.QueryKind.CLASS_LIST,
                key = null,
                query = classQuery
            ),
            CacheBridgeKeys.cacheKeyOf(
                appTag = "app",
                queryKind = DexKitCacheBridge.QueryKind.METHOD_LIST,
                key = null,
                query = methodQuery
            )
        )
    }
}
