package org.cardanofoundation.hydra.cardano.client.lib.utils;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public class MoreObjects {

    public static @Nullable String toStringNullSafe(@Nullable Object obj) {
        return obj != null ? obj.toString() : null;
    }

    public static @Nullable BigDecimal toBigDecimal(@Nullable Integer obj) {
        return obj != null ? BigDecimal.valueOf(obj) : null;
    }

}
