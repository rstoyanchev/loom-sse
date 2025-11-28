package source;

import java.time.Duration;

import carrier.ClosedException;
import org.jspecify.annotations.Nullable;

public interface BufferedSource<T> extends Source<T> {

	/**
	 * Receive the next item, blocking if necessary for the given duration.
	 * @return the item, or {@code null} if no items were received before timeout,
	 * or the Source was closed.
	 */
	T receive(Duration timeout) throws ClosedException, InterruptedException;

	/**
	 * Try to receive an item.
	 * @return the item, or {@code null} if there are no items at this time,
	 * or the Source is closed.
	 */
	@Nullable T tryReceive();

}
