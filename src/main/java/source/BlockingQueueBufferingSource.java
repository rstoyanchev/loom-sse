package source;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

public class BlockingQueueBufferingSource<T> implements BufferingSource<T> {

	private final BlockingQueue<T> queue;

	private volatile @Nullable Object completion; // Boolean.TRUE, Exception, or null

	private volatile boolean closed;


	public BlockingQueueBufferingSource() {
		this(128);
	}

	public BlockingQueueBufferingSource(int capacity) {
		this.queue = (capacity > 0 ? new LinkedBlockingQueue<>(capacity) : new SynchronousQueue<>());
	}


	// Sink


	@Override
	public boolean isComplete() {
		return (this.completion != null || this.closed);
	}

	@Override
	public void send(T item) throws ClosedException, InterruptedException {
		this.queue.put(item);
	}

	@Override
	public void complete() {
		complete(Boolean.TRUE);
	}

	@Override
	public void completeExceptionally(Throwable cause) {
		complete(cause != null ? cause : Boolean.TRUE);
	}

	private void complete(Object completion) {
		if (this.completion == null) {
			this.completion = completion;
		}
	}


	// Source

	@Override
	public boolean isClosed() {
		return this.closed;
	}

	@Override
	public @Nullable Throwable getCompletionException() {
		return (this.completion instanceof Throwable throwable ? throwable : null);
	}

	@Override
	public @Nullable T receive() throws ClosedException, InterruptedException {
		assertNotClosed();
		T item = this.queue.take();
		// TODO: catch interrupt and close
		closeAfterCompletion();
		return item;
	}

	@Override
	public T tryReceive(Duration timeout) throws ClosedException, InterruptedException {
		assertNotClosed();
		// TODO: catch interrupt and close
		T item = this.queue.poll(TimeUnit.MILLISECONDS.convert(timeout), TimeUnit.MILLISECONDS);
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
			throw new ClosedException(this);
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
		if (this.completion == null) {
			this.completion = new CancellationException();
		}
		// call onClose callback
		this.queue.clear(); // discarded items
	}

	// onClose callback

}
