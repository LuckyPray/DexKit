package org.luckypray.dexkit.testflags;

// Shared by the D8 fixture and host reflection tests. Bypass R8 when comparing
// source modifiers: optimization may widen visibility or remove metadata.
public class AccessFlagsFixture {
    public volatile int visible;
    private transient Object hidden;
    int plain;

    public AccessFlagsFixture() {}
    AccessFlagsFixture(int value) { plain = value; }
    public synchronized void synchronizedMethod() { visible++; }
    public static synchronized void staticSynchronizedMethod() {}
    public native synchronized void nativeSynchronizedMethod();
    public native void nativeMethod();
    void plainMethod() {}
    public void varargsMethod(String... values) { visible = values.length; }
    public int readVisible() { return visible; }

    public interface Generic<T> { T value(); }
    public static class Bridge implements Generic<String> {
        @Override public String value() { return "bridge"; }
    }
    public static final class PublicNested {}
    private static final class PrivateNested {}
    protected abstract static class ProtectedNested { public abstract void abstractMethod(); }
    static class PackageNested {}
    public class Inner { public Object outer() { return AccessFlagsFixture.this; } }
    public enum Choice { FIRST, SECOND }

    public static Class<?>[] types() {
        return new Class<?>[] { AccessFlagsFixture.class, Generic.class, Bridge.class,
            PublicNested.class, PrivateNested.class, ProtectedNested.class,
            PackageNested.class, Inner.class, Choice.class };
    }
}
