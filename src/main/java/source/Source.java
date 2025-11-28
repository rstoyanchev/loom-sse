package source;

import carrier.ClosedException;
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
	@Nullable T receive() throws ClosedException, InterruptedException;

	/**
	 * Close the Source from the receiving side.
	 */
	@Override
	void close();


	/**
	 *
	 * @return
	 */
	default Producer<T> asProducer() {
		return sink -> {
			try (this) {
				while (true) {
					if (isClosed()) {
						Throwable cause = getCompletionException();
						if (cause != null) {
							sink.completeExceptionally(cause);
						}
						else {
							sink.complete();
						}
						break;
					}
					else if (sink.isComplete()) {
						close();
						break;
					}
					else {
						T item = null;
						try {
							item = receive();
							sink.send(item);
						}
						catch (ClosedException ex) {
							if (item != null) {
								// discarded item
							}
							break;
						}
					}
				}
			}
		};
	}
}
