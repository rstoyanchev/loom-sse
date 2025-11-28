package source;

public interface Producer<T> {

	/**
	 *
	 * @param sink
	 * @throws InterruptedException
	 */
	void produce(Sink<T> sink) throws InterruptedException;

}
