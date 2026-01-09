package org.bhel.hrm.common.utils;

import java.util.stream.Stream;

/**
 * Supported TLS protocol versions in order of preference.
 */
public enum TLSProtocol {
    TLS_1_3("TLSv1.3"),
    TLS_1_2("TLSv1.2");

    private final String protocol;

    TLSProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    public static String[] toStringArray() {
        return Stream.of(values())
            .map(TLSProtocol::getProtocol)
            .toArray(String[]::new);
    }
}
