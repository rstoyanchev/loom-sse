package source;

import java.io.IOException;
import java.time.Duration;

import org.jspecify.annotations.Nullable;

public interface Source<T> extends AutoCloseable {

	/**
	 * Return {@code true} if the Source is closed. This may be due to a call to
	 * {@link #close()} from the receiving side, or because the Source itself
	 * has completed.
	 */
	boolean isClosed();

	/**
	 * If the Source completed with an error, return the Exception that led to it.
	 */
	@Nullable Throwable getCompletionException();

	/**
	 * Receive the next item, blocking if necessary.
	 * @return the received item, or {@code null} if the Source completed without an item.
	 */
	@Nullable T receive() throws IOException, ClosedException, InterruptedException;

	/**
	 * Receive the next item, blocking if necessary for the given duration.
	 * @return the item, or {@code null} if no items were received before timeout,
	 * or the Source was closed.
	 */
	@Nullable T tryReceive(Duration timeout) throws IOException, ClosedException, InterruptedException;

	/**
	 * Try to receive an item.
	 * @return the item, or {@code null} if there are no items at this time,
	 * or the Source is closed.
	 */
	@Nullable T tryReceive();

	/**
	 * Close the Source from the receiving side.
	 */
	@Override
	void close();

}
