package source;

import java.util.concurrent.StructuredTaskScope;

public class StructuredActiveProducer<T> extends ActiveProducer<T> {

	private final StructuredTaskScope<?, ?> scope;

	public StructuredActiveProducer(Source<T> source, Sink<T> sink) {
		super(source, sink);
		this.scope = StructuredTaskScope.open();
	}


	@Override
	protected void startInternal() {
		this.scope.fork(() -> {
			produce();
			return null;
		});
	}

	@Override
	protected void stopInternal() {
		this.scope.close();
	}


	public static <T> StructuredActiveProducer<T> create(Source<T> source) {
		return new StructuredActiveProducer<>(source, null);
	}

}
