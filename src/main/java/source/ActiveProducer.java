package source;

import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

public abstract class ActiveProducer<T> {

	protected final Logger logger = LogManager.getLogger(getClass());


	private final Producer<T> producer;

	private final Sink<T> sink;

	private State state = State.NEW;


	protected ActiveProducer(Source<T> source, @Nullable Sink<T> sink) {
		this(new SourceProducerAdapter<>(source), sink);
	}

	protected ActiveProducer(Producer<T> producer, @Nullable Sink<T> sink) {
		this.producer = producer;
		this.sink = (sink != null ? sink : new BlockingQueueBufferingSource<>());
	}


	public Source<T> getSource() {
		if (this.sink instanceof BufferingSource<T> source) {
			return new CloseInterceptingSource<>(source);
		}
		throw new IllegalStateException(
				this.sink.getClass().getName() + " is not a BufferingSource");
	}


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



	private class CloseInterceptingSource<T> extends DecoratorSource<T> {

		CloseInterceptingSource(Source<T> source) {
			super(source);
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
				super.close();
			}
		}
	}

}
