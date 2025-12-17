package source;

import java.io.IOException;
import java.time.Duration;

/**
 * Extension of {@link Source} that uses an active task to prefetch items from
 * another {@link Source} and stores fetched items in a
 * {@link java.util.concurrent.BlockingQueue}.
 *
 * @param <T> the types of items received through the Source
 */
public interface ActiveSource<T> extends Source<T> {

	/**
	 * Whether the Source is closed. This is useful after a call to
	 * {@link #tryReceiveNext(Duration)}, which may return {@code false} due to
	 * a timeout before an item could be received.
	 */
	boolean isClosed();

	/**
	 * Trigger receiving and block for up to the specified time until at least
	 * one item is received. If {@code true}, {@link #next()} will return an item.
	 * @param timeout how long to wait before returning control
	 * @return {@code true} if at least one item was successfully received;
	 * {@code false} if an item was not received.
	 */
	boolean tryReceiveNext(Duration timeout) throws IOException, InterruptedException;

	/**
	 * Try receiving, but return immediately if an item cannot be received
	 * without blocking. If {@code true}, then {@link #next()} will return an item.
	 * @return {@code true} if at least one item is available;
	 * {@code false} if an item was not received.
	 */
	boolean tryReceiveNext() throws IOException;

}
