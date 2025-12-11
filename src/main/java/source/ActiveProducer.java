package source;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ActiveProducer<T> {

	protected final Logger logger = LogManager.getLogger(getClass());


	private final Producer<T> producer;

	private final Sink<T> sink;

	private State state = State.NEW;


	protected ActiveProducer(Producer<T> producer, Sink<T> sink) {
		this.producer = producer;
		this.sink = sink;
	}


	public Source<T> getSource() {
		if (this.sink instanceof BufferingSource<T> source) {
			return source;
		}
		throw new IllegalStateException(
				this.sink.getClass().getName() + " is not a BufferingSource");
	}

	protected Producer<T> getProducer() {
		return this.producer;
	}

	protected Sink<T> getSink() {
		return this.sink;
	}


	public final void start() {
		if (this.state == State.NEW) {
			if (logger.isInfoEnabled()) {
				logger.info("Starting " + getProducer());
			}
			this.state = State.RUNNING;
			startInternal();
		}
	}

	protected abstract void startInternal();

	public final void stop() {
		if (this.state == State.RUNNING) {
			if (logger.isInfoEnabled()) {
				logger.info("Stopping " + getProducer());
			}
			this.state = State.STOPPED;
			stopInternal();
		}
	}

	protected abstract void stopInternal();

	protected void produce() throws InterruptedException {
		this.producer.produce(this.sink);
	}


	private enum State {
		NEW, RUNNING, STOPPED
	}

}
