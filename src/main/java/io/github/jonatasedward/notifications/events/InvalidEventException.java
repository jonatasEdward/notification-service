package io.github.jonatasedward.notifications.events;

/** A message that can never be processed, no matter how many times it is retried. Goes straight to the DLT. */
class InvalidEventException extends RuntimeException {

    InvalidEventException(String message) {
        super(message);
    }

    InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
