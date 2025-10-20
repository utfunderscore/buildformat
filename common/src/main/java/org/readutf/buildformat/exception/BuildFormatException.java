package org.readutf.buildformat.exception;

public abstract class BuildFormatException extends Exception {

    public BuildFormatException(String message) {
        super(message);
    }

    public BuildFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public BuildFormatException(Throwable cause) {
        super(cause);
    }
}
