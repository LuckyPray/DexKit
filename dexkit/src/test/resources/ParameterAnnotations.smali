.class public abstract Ltest/ParameterAnnotations;
.super Ljava/lang/Object;

.method public static leadingAndTrailing(IIII)V
    .registers 4
    .param p2
        .annotation runtime Ltest/Marker;
        .end annotation
    .end param
    return-void
.end method

.method public static middle(IIII)V
    .registers 4
    .param p0
        .annotation runtime Ltest/Marker;
        .end annotation
    .end param
    .param p3
        .annotation runtime Ltest/Marker;
        .end annotation
    .end param
    return-void
.end method

.method public static wide(JID)V
    .registers 5
    .param p2
        .annotation runtime Ltest/Marker;
        .end annotation
    .end param
    return-void
.end method

.method public abstract abstractMethod(IIII)V
    .param p3
        .annotation runtime Ltest/Marker;
        .end annotation
    .end param
.end method
