package org.readutf.buildformat.exception;

public class BuildSerializeException extends BuildFormatException {

    public BuildSerializeException(String message) {
        super(message);
    }

    public BuildSerializeException(String message, Throwable cause) {
        super(message, cause);
    }

    public BuildSerializeException(Throwable cause) {
        super(cause);
    }
}
