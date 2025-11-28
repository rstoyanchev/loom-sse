package source;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

public class BlockingQueueConduit<T> implements Conduit<T> {

	private final BlockingQueue<T> queue;

	private volatile @Nullable Object completion; // Boolean.TRUE, Exception, or null

	private volatile boolean closed;


	public BlockingQueueConduit() {
		this(128);
	}

	public BlockingQueueConduit(int capacity) {
		this.queue = (capacity > 0 ? new LinkedBlockingQueue<>(capacity) : new SynchronousQueue<>());
	}


	// Sink


	@Override
	public boolean isComplete() {
		return (this.completion != null);
	}

	@Override
	public @Nullable Throwable getCompletionException() {
		return (this.completion instanceof Throwable throwable ? throwable : null);
	}

	@Override
	public void send(T item) throws ClosedException, InterruptedException {
		this.queue.put(item);
	}

	@Override
	public void complete() {
		if (this.completion != null) {
			this.completion = Boolean.TRUE;
		}
	}

	@Override
	public void completeExceptionally(Throwable cause) {
		if (this.completion != null) {
			this.completion = (cause != null ? cause : Boolean.TRUE);
		}
	}


	// Source

	@Override
	public boolean isClosed() {
		return this.closed;
	}

	@Override
	public T receive() throws ClosedException, InterruptedException {
		assertNotClosed();
		T item = this.queue.take();
		closeAfterCompletion();
		return item;
	}

	@Override
	public T receive(Duration timeout) throws ClosedException, InterruptedException {
		assertNotClosed();
		T item = this.queue.poll(timeout.getNano(), TimeUnit.NANOSECONDS);
		closeAfterCompletion();
		return item;
	}

	@Override
	public @Nullable T tryReceive() {
		assertNotClosed();
		T item = this.queue.poll();
		closeAfterCompletion();
		return item;
	}

	private void assertNotClosed() {
		if (isClosed()) {
			throw new carrier.ClosedException(this);
		}
	}

	private void closeAfterCompletion() {
		if (this.completion != null && this.queue.isEmpty()) {
			this.closed = true;
		}
	}

	@Override
	public void close() {
		this.closed = true;
		if (this.completion != null) {
			this.completion = new CancellationException();
		}
		this.queue.clear(); // discarded items
	}

}
