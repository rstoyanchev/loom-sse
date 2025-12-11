package source;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeoutException;

public class StructuredActiveProducer<T> extends ActiveProducer<T> {

	private final StructuredTaskScope<Void, ?> scope;

	public StructuredActiveProducer(Source<T> source, Sink<T> sink) {
		super(source, sink);
		this.scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.anySuccessfulResultOrThrow());
	}


	@Override
	protected void startInternal() {
		this.scope.fork(getProducerTask());
	}

	@Override
	protected void stopInternal() throws InterruptedException {
		this.scope.fork(() -> {});
		try {
			this.scope.join();
		}
		finally {
			this.scope.close();
		}
	}


	public static <T> StructuredActiveProducer<T> create(Source<T> source) {
		return new StructuredActiveProducer<>(source, null);
	}

}
