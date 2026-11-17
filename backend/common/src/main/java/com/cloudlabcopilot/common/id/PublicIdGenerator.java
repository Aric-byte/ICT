package com.cloudlabcopilot.common.id;

import java.security.SecureRandom;

public final class PublicIdGenerator {

    private static final char[] CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private PublicIdGenerator() {
    }

    public static String nextId() {
        long timestamp = System.currentTimeMillis();
        char[] out = new char[32];
        for (int i = 9; i >= 0; i--) {
            out[i] = CROCKFORD[(int) (timestamp & 0x1F)];
            timestamp >>>= 5;
        }
        for (int i = 10; i < 32; i++) {
            out[i] = CROCKFORD[RANDOM.nextInt(32)];
        }
        return new String(out);
    }
}