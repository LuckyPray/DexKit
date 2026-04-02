package org.luckypray.dexkit.annotations

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is experimental. It can be incompatibly changed in the future."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class DexKitExperimentalApi