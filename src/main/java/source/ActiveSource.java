package source;

import java.io.IOException;
import java.time.Duration;

/**
 * Extension of {@code Source} that launches an active receiver task to prefetch
 * items from a delegate {@link Source}, and stores them in a
 * {@link java.util.concurrent.BlockingQueue}.
 *
 * @param <T> the types of items received through the Source
 */
public interface ActiveSource<T> extends Source<T> {

	/**
	 * Whether the Source is closed. This is useful in conjunction with calls to
	 * {@link #tryReceiveNext(Duration)}, which may return {@code false} due to
	 * a timeout before an item could be received.
	 */
	boolean isClosed();

	/**
	 * Request to receive the next item from the Source, and wait for up to
	 * specified time for at least one item to be received.
	 * A return value of {@code true} guarantees {@link #next()} will return an
	 * item, while {@code false} means that an item could not be received within
	 * the specified time.
	 * @param timeout how long to wait before returning control
	 * @return {@code true} if at least one item was received
	 */
	boolean tryReceiveNext(Duration timeout) throws IOException, InterruptedException;

	/**
	 * Request to receive the next item from the Source, but return immediately
	 * if an item cannot be received without blocking. If {@code true}, then
	 * {@link #next()} will return an item.
	 * @return {@code true} if at least one item was received
	 */
	boolean tryReceiveNext() throws IOException;

}
