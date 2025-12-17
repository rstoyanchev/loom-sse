package source;

import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;

public class StructuredBufferedSource<T> extends AbstractBufferedSource<T> {

	private final StructuredTaskScope<Void, ?> scope;

	public StructuredBufferedSource(Source<T> source) {
		super(source);
		this.scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.anySuccessfulResultOrThrow());
	}


	@Override
	protected void start(Callable<Void> producer) {
		this.scope.fork(producer);
	}

	@Override
	protected void stop() {
		this.scope.fork(() -> {});
		try {
			this.scope.join();
		}
		catch (InterruptedException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug(ex.getMessage(), ex);
			}
			Thread.currentThread().interrupt();
		}
		finally {
			this.scope.close();
		}
	}


	public static <T> StructuredBufferedSource<T> create(Source<T> source) {
		return new StructuredBufferedSource<>(source);
	}

}
