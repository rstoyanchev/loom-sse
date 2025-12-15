package source;

/**
 * Contract for a Producer to push items to a consumer through a {@link Sink}.
 * @param <T> the types of items the producer pushes
 */
public interface Producer<T> {

	/**
	 * Produce items and push them into the given sink.
	 * @param sink the sink to push into
	 * @throws InterruptedException if the producer is interrupted
	 */
	void produce(Sink<T> sink) throws InterruptedException;

}
