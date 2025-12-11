package source;

public interface Sink<T> {

	/**
	 * Return {@code true} if the sink can no longer send either due to completion
	 * via {@link #complete} or {@link #completeExceptionally}, or because the
	 * {@code Sink} is closed.
	 */
	boolean isComplete();

	/**
	 * Send or block until an item can be sent.
	 */
	void send(T item) throws ClosedException, InterruptedException;

	/**
	 * Close the sink for sending.
	 */
	void complete();

	/**
	 * Close the sink with an Exception cause.
	 */
	void completeExceptionally(Throwable cause);

}
