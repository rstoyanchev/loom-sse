package carrier;

import org.jspecify.annotations.Nullable;

/**
 *
 * @param <T>
 */
public interface Carriable<T> extends AutoCloseable {

	/**
	 * Return {@code true} if there are no queued elements.
	 */
	boolean isEmpty();

	/**
	 * Return the cause for closing the sending side via {@link #closeExceptionally(Throwable)}.
	 */
	@Nullable Throwable getCloseException();

	/**
	 * Close the sending side. Queued elements remain available to receive.
	 */
	@Override
	void close();

	/**
	 * Variant of {@link #close()} with an exception cause.
	 */
	void closeExceptionally(Throwable cause);

}
