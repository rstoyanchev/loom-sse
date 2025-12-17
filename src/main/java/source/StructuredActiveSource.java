package source;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.StructuredTaskScope;

/**
 * ActiveSource that uses structured concurrency to execute the receiver task.
 */
@SuppressWarnings("preview")
public class StructuredActiveSource<T> extends AbstractActiveSource<T> {

	private final StructuredTaskScope<Void, ?> scope;

	public StructuredActiveSource(Source<T> source) {
		super(source);
		this.scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.anySuccessfulResultOrThrow());
	}


	@Override
	protected void start(Callable<Void> receiver) {
		this.scope.fork(receiver);
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


	public static <T> StructuredActiveSource<T> from(Source<T> source) {
		return new StructuredActiveSource<>(source);
	}

}
