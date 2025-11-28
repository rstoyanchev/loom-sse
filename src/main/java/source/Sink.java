package source;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import carrier.ClosedException;
import org.jspecify.annotations.Nullable;

public interface Sink<T> {

	/**
	 // TODO: what if closed from Source side?
	 * Return {@code true} if the sink is closed for sending.
	 */
	boolean isComplete();

	/**
	 * If the Sink completed exceptionally, return the Exception passed into
	 * {@link #completeExceptionally(Throwable)}.
	 */
	@Nullable Throwable getCompletionException();

	/**
	 * Send or block until an item can be sent.
	 */
	void send(T item) throws ClosedException, InterruptedException;

	/**
	 * Close the sink for sending.
	 */
	void complete();

	/**
	 * Close the sink with an Exception cause.
	 */
	void completeExceptionally(Throwable cause);

}
