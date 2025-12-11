package source;

public abstract class ActiveProducer<T> {

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
			this.state = State.RUNNING;
			startInternal();
		}
	}

	protected abstract void startInternal();

	public final void stop() {
		if (this.state == State.RUNNING) {
			this.state = State.STOPPED;
			stopInternal();
		}
	}

	protected abstract void stopInternal();


	private enum State {
		NEW, RUNNING, STOPPED
	}

}
