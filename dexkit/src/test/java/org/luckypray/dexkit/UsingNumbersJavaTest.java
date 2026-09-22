package org.luckypray.dexkit;

import org.junit.Test;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.UsingNumberData;

import java.util.List;

import static org.junit.Assert.*;

public class UsingNumbersJavaTest {
    static { LibLoader.loadLibrary("dexkit"); }

    @Test
    public void javaGetterReturnsAnUnmodifiableValueSnapshot() {
        List<UsingNumberData> values;
        try (DexKitBridge bridge = DexKitBridge.create(new byte[][] { UsingNumbersFixture.dexBytes() })) {
            MethodData method = bridge.getMethodData("Lorg/luckypray/dexkit/fixture/UsingNumbers;->mixed(I)V");
            assertNotNull(method);
            values = method.getUsingNumbers();
            assertEquals(6, values.size());
            assertEquals(0x12, values.get(0).getOpCode());
            assertEquals(32, values.get(0).getBitWidth());
            assertEquals(1L, values.get(0).getRawBits());
            assertEquals(-1L, values.get(1).longValue());
            assertThrows(UnsupportedOperationException.class, () -> values.add(values.get(0)));
        }
        assertEquals(-1L, values.get(4).longValue());
    }
}
