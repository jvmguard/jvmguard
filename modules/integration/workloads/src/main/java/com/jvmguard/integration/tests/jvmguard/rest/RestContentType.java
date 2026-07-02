package com.jvmguard.integration.tests.jvmguard.rest;

public enum RestContentType {
    JSON("application/json", "application/json;charset=UTF-8"),
    XML("application/xml", "application/xml"),
    TEXT("text/plain", "text/plain;charset=UTF-8"),
    ALL("*/*", "application/json;charset=UTF-8"),
    NONE("", "application/json;charset=UTF-8");

    private final String string;
    private final String expectedReturn;

    RestContentType(String string, String expectedReturn) {
        this.string = string;
        this.expectedReturn = expectedReturn;
    }


    public String getExpectedReturn() {
        return expectedReturn;
    }

    @Override
    public String toString() {
        return string;
    }
}
