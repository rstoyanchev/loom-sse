package source;

import java.io.IOException;
import java.time.Duration;

/**
 * Contract to send items to a consumer.
 * @param <T> the types of items that can be pushed into the Sink
 */
public interface Sink<T> {

	/**
	 * Send or block until an item can be sent.
	 */
	void send(T item) throws IOException, InterruptedException;

	/**
	 * Try to send the item, blocking for up to the specified duration.
	 * @return {@code true} if the item was sent, {@code false} otherwise
	 */
	boolean trySend(T item, Duration timeout) throws InterruptedException;

	/**
	 * Try to send the item without blocking.
	 * @return {@code true} if the item was sent, {@code false} otherwise
	 */
	boolean trySend(T item);

	/**
	 * Complete the sink, indicating there are no more items to send.
	 */
	void complete();

	/**
	 * Complete the sink with an Exception that provides a cause for the completion.
	 */
	void completeExceptionally(Throwable cause);

}
