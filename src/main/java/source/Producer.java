package source;

public interface Producer<T> {

	void produce(Sink<T> sink) throws InterruptedException;

}
