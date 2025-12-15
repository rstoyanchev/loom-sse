package source;

import java.io.IOException;
import java.time.Duration;

import org.jspecify.annotations.Nullable;

/**
 * Contract to consume items from a source.
 * @param <T> the types of items received through the Source
 */
public interface Source<T> extends AutoCloseable {

	/**
	 * Return {@code true} if the Source is closed. This may be due to a call to
	 * {@link #close()} from the receiving side, or because the Source itself
	 * completed, possibly with an {@link #getCompletionException() error}.
	 */
	boolean isClosed();

	/**
	 * If the Source completed due to an Exception, this method provides access
	 * to that Exception.
	 * @return the Exception that caused the Source to end, or {@code null} if
	 * the Source completed with success, or has not yet completed.
	 */
	@Nullable Throwable getCompletionException();

	/**
	 * Receive the next item, blocking if necessary.
	 * @return the received item, or {@code null} if the Source completed without an item.
	 */
	@Nullable T receive() throws IOException, ClosedException, InterruptedException;

	/**
	 * Receive the next item, blocking if necessary up to the given duration.
	 * @return the item, or {@code null} if an item could not be received
	 * before the timeout.
	 */
	@Nullable T tryReceive(Duration timeout) throws IOException, ClosedException, InterruptedException;

	/**
	 * Try to receive an item.
	 * @return the item, or {@code null} if there aren't any items available to
	 * receive without blocking at this time.
	 */
	@Nullable T tryReceive();

	/**
	 * Close the Source from the receiving side.
	 */
	@Override
	void close();

}
