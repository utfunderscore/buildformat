package org.readutf.buildformat.common.exception;

public class BuildFormatException extends Exception {
    public BuildFormatException(String message) {
        super(message);
    }

    public BuildFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
