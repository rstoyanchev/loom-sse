package source;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

public abstract class AbstractActiveProducer<T> implements ActiveProducer<T> {

	protected final Logger logger = LogManager.getLogger(getClass());

	private final Producer<T> producer;

	private final Sink<T> sink;

	private State state = State.NEW;


	protected AbstractActiveProducer(Source<T> source, @Nullable Sink<T> sink) {
		this(new SourceProducerAdapter<>(source), sink);
	}

	protected AbstractActiveProducer(Producer<T> producer, @Nullable Sink<T> sink) {
		this.producer = producer;
		this.sink = (sink != null ? sink : new BlockingQueueBufferedSource<>());
	}


	@SuppressWarnings("unchecked")
	@Override
	public BufferedSource<T> bufferedSource() {
		if (this.sink instanceof BufferedSource) {
			return new CancellationPropagatingBufferedSource<>((BufferedSource<T>) this.sink);
		}
		throw new IllegalStateException(
				this.sink.getClass().getName() + " is not a Source");
	}


	@Override
	public final void start() {
		if (this.state == State.NEW) {
			if (logger.isInfoEnabled()) {
				logger.info("Starting " + this.producer);
			}
			this.state = State.RUNNING;
			startInternal();
		}
	}

	protected abstract void startInternal();

	@Override
	public final void stop() throws InterruptedException {
		if (this.state == State.RUNNING) {
			if (logger.isInfoEnabled()) {
				logger.info("Stopping " + this.producer);
			}
			this.state = State.STOPPED;
			stopInternal();
		}
	}

	protected abstract void stopInternal() throws InterruptedException;

	protected Callable<Void> getProducerTask() {
		return () -> {
			this.producer.produce(this.sink);
			return null;
		};
	}


	private enum State {
		NEW, RUNNING, STOPPED
	}


	/**
	 * Intercept {@link Source#close()} and consumer thread interrupts, and
	 * propagate those to the Producer task by calling {@link #stop()}.
	 */
	private class CancellationPropagatingBufferedSource<T> implements BufferedSource<T> {

		private final BufferedSource<T> delegate;

		CancellationPropagatingBufferedSource(BufferedSource<T> source) {
			this.delegate = source;
		}

		@Override
		public @Nullable T receive() throws IOException, ClosedException, InterruptedException {
			try {
				return this.delegate.receive();
			}
			catch (InterruptedException ex) {
				stop();
				throw ex;
			}
		}

		@Override
		public @Nullable T tryReceive(Duration timeout) throws IOException, ClosedException, InterruptedException {
			try {
				return this.delegate.tryReceive(timeout);
			}
			catch (InterruptedException ex) {
				stop();
				throw ex;
			}
		}

		@Override
		public void close() {
			try {
				stop();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			finally {
				this.delegate.close();
			}
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
		public @Nullable T tryReceive() throws IOException, ClosedException {
			return this.delegate.tryReceive();
		}
	}

}
