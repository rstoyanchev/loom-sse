package source;

import java.io.IOException;
import java.time.Duration;

import org.jspecify.annotations.Nullable;

/**
 * A Source that delegates to another Source.
 */
public class SourceDecorator<T> implements Source<T> {

	private final Source<T> delegate;


	public SourceDecorator(Source<T> delegate) {
		this.delegate = delegate;
	}


	public Source<T> getDelegate() {
		return this.delegate;
	}


	@Override
	public boolean isClosed() {
		return this.delegate.isClosed();
	}

	@Override
	public @Nullable Throwable getCompletionException() {
		return this.delegate.getCompletionException();
	}

	@Override
	public @Nullable T receive() throws IOException, ClosedException, InterruptedException {
		return this.delegate.receive();
	}

	@Override
	public @Nullable T tryReceive(Duration timeout) throws IOException, ClosedException, InterruptedException {
		return this.delegate.tryReceive(timeout);
	}

	@Override
	public @Nullable T tryReceive() throws IOException, ClosedException {
		return this.delegate.tryReceive();
	}

	@Override
	public void close() {
		this.delegate.close();
	}

}
