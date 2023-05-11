package org.cardanofoundation.hydra.core.utils;

import org.jetbrains.annotations.Nullable;

public final class StringUtils {

    @Nullable
    public static String[] split(@Nullable String toSplit, @Nullable String delimiter) {
        if (toSplit == null || delimiter == null) {
            return null;
        }
        if (toSplit.isEmpty() || delimiter.isEmpty()) {
            return null;
        }
        int offset = toSplit.indexOf(delimiter);
        if (offset < 0) {
            return null;
        }

        String beforeDelimiter = toSplit.substring(0, offset);
        String afterDelimiter = toSplit.substring(offset + delimiter.length());

        return new String[] {beforeDelimiter, afterDelimiter};
    }
}
