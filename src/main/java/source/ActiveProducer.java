package source;

/**
 * Contract to start and stop a producer on a dedicated thread, also exposing a
 * {@link Source} for receiving items when {@link Sink} the producer pushes
 * into is a {@link BufferedSource}.
 *
 * @param <T> the types of items produced
 */
public interface ActiveProducer<T> {

	/**
	 * Return a {@link Source} for receiving items when the producer pushes
	 * into a {@link BufferedSource}.
	 * @throws IllegalStateException if the producer is pushing into a
	 * {@link Sink} that is not a {@link BufferedSource}.
	 */
	BufferedSource<T> bufferedSource();

	/**
	 * Start the producer in a dedicated thread.
	 */
	void start();

	/**
	 * Stop the producer.
	 */
	void stop() throws InterruptedException;

}
