package dev.marshall.hounded.config;

/** A config or messages file could not be read. The message says which file and why. */
public final class ConfigLoadException extends Exception {
    private static final long serialVersionUID = 1L;

    public ConfigLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
