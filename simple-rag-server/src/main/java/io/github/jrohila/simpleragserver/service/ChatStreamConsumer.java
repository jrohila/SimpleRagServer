package io.github.jrohila.simpleragserver.service;

/**
 * Optional hook to capture streamed assistant output.
 * Implement as a Spring bean (e.g., @Component) and it will be used if present.
 */
public interface ChatStreamConsumer {
    /** Called for each streamed delta chunk (non-empty). */
    void onDelta(String streamId, String delta);

    /** Called once at the end with the full accumulated assistant response. */
    void onComplete(String streamId, String fullResponse);
}
