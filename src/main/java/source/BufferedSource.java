package source;

import java.io.IOException;
import java.time.Duration;

import org.jspecify.annotations.Nullable;

/**
 * {@link Source} that returns items from a buffer.
 * @param <T> the types of items received through the Source
 */
public interface BufferedSource<T> extends Source<T> {

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
	@Nullable T tryReceive() throws IOException, ClosedException;

}
