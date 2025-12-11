package source;

public interface Producer<T> {

	void produce(Sink<T> sink) throws InterruptedException;


	static <T> Producer<T> fromSource(Source<T> source) {
		return new SourceProducerAdapter<>(source);
	}

}
