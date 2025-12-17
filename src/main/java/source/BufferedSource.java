package source;

import java.io.IOException;
import java.time.Duration;

/**
 * A {@link Source} that returns items from a buffer.
 * @param <T> the types of items received through the Source
 */
public interface BufferedSource<T> extends Source<T> {

	/**
	 * Trigger receiving and block up to the specified time until at least one
	 * item is received. If {@code true}, {@link #next()} will return an item.
	 * @param timeout how long to wait
	 * @return {@code true} if at least one item was successfully received;
	 * {@code false} if an item was not received.
	 */
	boolean tryReceiveNext(Duration timeout) throws IOException, InterruptedException;

	/**
	 * Try receiving but return immediately if not possible without blocking.
	 * If {@code true}, then {@link #next()} will return an item.
	 * @return {@code true} if at least one item is available;
	 * {@code false} if an item was not received.
	 */
	boolean tryReceiveNext() throws IOException;

}
