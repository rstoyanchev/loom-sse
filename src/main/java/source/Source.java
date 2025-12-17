package source;

import java.io.IOException;

/**
 * Contract to consume items from a source.
 * @param <T> the types of items received through the Source
 */
public interface Source<T> extends AutoCloseable {

	/**
	 * Return {@code true} if the Source is closed, and can no longer receive.
	 */
	boolean isClosed();

	/**
	 * Trigger receiving and block if necessary until at least one item is received.
	 * If {@code true}, {@link #next()} will return an item.
	 * @return {@code true} if at least one item was successfully received;
	 * {@code false} if no more items can be received.
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
