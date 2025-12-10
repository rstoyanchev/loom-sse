package source;

public interface Sink<T> {

	/**
	 * Return {@code true} if the sink is closed for sending.
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
