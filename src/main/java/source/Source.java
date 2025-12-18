package source;

import java.io.IOException;

/**
 * Contract to receive items from a Source.
 * @param <T> the types of items received from the Source
 */
public interface Source<T> extends AutoCloseable {

	/**
	 * Request to receive the next item from the Source.
	 * <ul>
	 * <li>For direct {@link Source} implementations, this triggers receiving,
	 * and blocks until at least one item is received.
	 * <li>For {@link ActiveSource} implementations with an active receiver task
	 * this method waits until an item appears in the {@code BlockingQueue}.
	 * </ul>
	 * A return value of {@code true} guarantees {@link #next()} will return an
	 * item, while {@code false} means the Source will not return more items.
	 * @return {@code true} if there at least one received item
	 */
	boolean receiveNext() throws IOException, InterruptedException;

	/**
	 * Return the next received item. Use this only after a preceding call to
	 * {@link #receiveNext()} that returns {@code true}.
	 */
	T next();

	/**
	 * Close the Source from the receiving side.
	 */
	@Override
	void close();

}
