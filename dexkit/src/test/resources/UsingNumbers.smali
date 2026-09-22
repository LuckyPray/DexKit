# Keep the exact instruction forms for numeric matching regression tests.
.class public abstract Lorg/luckypray/dexkit/fixture/UsingNumbers;
.super Ljava/lang/Object;

.method public static int4Min()I
    .registers 1
    const/4 v0, -8
    return v0
.end method
.method public static int4Max()I
    .registers 1
    const/4 v0, 7
    return v0
.end method
.method public static int16Min()I
    .registers 1
    const/16 v0, -32768
    return v0
.end method
.method public static int16Max()I
    .registers 1
    const/16 v0, 32767
    return v0
.end method
.method public static intMin()I
    .registers 1
    const v0, -0x80000000
    return v0
.end method
.method public static intMax()I
    .registers 1
    const v0, 0x7fffffff
    return v0
.end method
.method public static intHigh16Min()I
    .registers 1
    const/high16 v0, -0x80000000
    return v0
.end method
.method public static wide16Min()J
    .registers 2
    const-wide/16 v0, -32768
    return-wide v0
.end method
.method public static wide16Max()J
    .registers 2
    const-wide/16 v0, 32767
    return-wide v0
.end method
.method public static wide32Min()J
    .registers 2
    const-wide/32 v0, -0x80000000
    return-wide v0
.end method
.method public static wide32Max()J
    .registers 2
    const-wide/32 v0, 0x7fffffff
    return-wide v0
.end method
.method public static wideMin()J
    .registers 2
    const-wide v0, -0x8000000000000000L
    return-wide v0
.end method
.method public static wideMax()J
    .registers 2
    const-wide v0, 0x7fffffffffffffffL
    return-wide v0
.end method
.method public static wideHigh16Min()J
    .registers 2
    const-wide/high16 v0, -0x8000000000000000L
    return-wide v0
.end method
.method public static zero4()F
    .registers 1
    const/4 v0, 0
    return v0
.end method
.method public static zero16()F
    .registers 1
    const/16 v0, 0
    return v0
.end method
.method public static zero32()F
    .registers 1
    const v0, 0
    return v0
.end method
.method public static zeroHigh16()F
    .registers 1
    const/high16 v0, 0
    return v0
.end method
.method public static zeroWide16()D
    .registers 2
    const-wide/16 v0, 0
    return-wide v0
.end method
.method public static zeroWide32()D
    .registers 2
    const-wide/32 v0, 0
    return-wide v0
.end method
.method public static zeroWide()D
    .registers 2
    const-wide v0, 0
    return-wide v0
.end method
.method public static zeroWideHigh16()D
    .registers 2
    const-wide/high16 v0, 0
    return-wide v0
.end method
.method public static floatOne()F
    .registers 1
    const v0, 0x3f800000
    return v0
.end method
.method public static floatHigh16One()F
    .registers 1
    const/high16 v0, 0x3f800000
    return v0
.end method
.method public static doubleOne()D
    .registers 2
    const-wide v0, 0x3ff0000000000000L
    return-wide v0
.end method
.method public static doubleHigh16One()D
    .registers 2
    const-wide/high16 v0, 0x3ff0000000000000L
    return-wide v0
.end method
.method public static wide32FloatBits()D
    .registers 2
    const-wide/32 v0, 0x3f800000
    return-wide v0
.end method
.method public static negative4()F
    .registers 1
    const/4 v0, -1
    return v0
.end method
.method public static negative16()F
    .registers 1
    const/16 v0, -1
    return v0
.end method
.method public static negativeWide16()D
    .registers 2
    const-wide/16 v0, -1
    return-wide v0
.end method
.method public static negativeWide32()D
    .registers 2
    const-wide/32 v0, -1
    return-wide v0
.end method
.method public static tiny4()F
    .registers 1
    const/4 v0, 1
    return v0
.end method
.method public static tiny16()F
    .registers 1
    const/16 v0, 1
    return v0
.end method
.method public static tinyWide16()D
    .registers 2
    const-wide/16 v0, 1
    return-wide v0
.end method
.method public static lit8Zero(I)I
    .registers 2
    add-int/lit8 v0, p0, 0
    return v0
.end method
.method public static lit16Zero(I)I
    .registers 2
    add-int/lit16 v0, p0, 0
    return v0
.end method
.method public static lit8Min(I)I
    .registers 2
    add-int/lit8 v0, p0, -128
    return v0
.end method
.method public static lit16Min(I)I
    .registers 2
    add-int/lit16 v0, p0, -32768
    return v0
.end method
.method public static negativeZeroFloat()F
    .registers 1
    const/high16 v0, -0x80000000
    return v0
.end method
.method public static negativeZeroDouble()D
    .registers 2
    const-wide/high16 v0, -0x8000000000000000L
    return-wide v0
.end method
.method public static positiveInfinityFloat()F
    .registers 1
    const/high16 v0, 0x7f800000
    return v0
.end method
.method public static negativeInfinityDouble()D
    .registers 2
    const-wide/high16 v0, -0x10000000000000L
    return-wide v0
.end method

.method public static mixed(I)V
    .registers 3
    const/4 v0, 1
    add-int/lit8 v0, p0, -1
    const/4 v0, 1
    const/16 v0, 1
    const-wide/16 v0, -1
    const/16 v0, 0
    return-void
.end method
.method public static nanFloatOne()F
    .registers 1
    const v0, 0x7fc00001
    return v0
.end method
.method public static nanFloatTwo()F
    .registers 1
    const v0, 0x7fc00002
    return v0
.end method
.method public static nanDoubleOne()D
    .registers 2
    const-wide v0, 0x7ff8000000000001L
    return-wide v0
.end method
.method public static nanDoubleTwo()D
    .registers 2
    const-wide v0, 0x7ff8000000000002L
    return-wide v0
.end method
.method public abstract emptyAbstract()V
.end method
.method public static native emptyNative()V
.end method
.method public static noNumbers()V
    .registers 0
    return-void
.end method
.method public static payloads(I[I)V
    .registers 2
    packed-switch p0, :packed
    sparse-switch p0, :sparse
    fill-array-data p1, :array
    :done
    return-void
    :packed
    .packed-switch 0x7f010123
        :done
    .end packed-switch
    :sparse
    .sparse-switch
        0x7f010456 -> :done
    .end sparse-switch
    :array
    .array-data 4
        0x7f010789
        0x7f010abc
    .end array-data
.end method
