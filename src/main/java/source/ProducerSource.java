package source;

import java.time.Duration;

import carrier.ClosedException;
import org.jspecify.annotations.Nullable;

public class ProducerSource<T> implements BufferedSource<T> {

	private final Producer<T> producer;

	private final Conduit<T> conduit;


	public ProducerSource(Producer<T> producer) {
		this.producer = producer;
		this.conduit = new BlockingQueueConduit<>();
	}


	public void startAndWait() throws InterruptedException {
		this.producer.produce(this.conduit);
	}


	@Override
	public boolean isClosed() {
		return this.conduit.isClosed();
	}

	@Override
	public @Nullable Throwable getCompletionException() {
		return this.conduit.getCompletionException();
	}

	@Override
	public T receive() throws ClosedException, InterruptedException {
		return this.conduit.receive();
	}

	@Override
	public T receive(Duration timeout) throws ClosedException, InterruptedException {
		return this.conduit.receive(timeout);
	}

	@Override
	public @Nullable T tryReceive() {
		return this.conduit.tryReceive();
	}

	@Override
	public void close() {
		this.conduit.close();
	}

}
