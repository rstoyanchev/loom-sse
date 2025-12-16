package source;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

public class BlockingQueueSource<T> implements Source<T>, Sink<T> {

	private final BlockingQueue<T> queue;

	private volatile @Nullable Object completion; // Boolean.TRUE, Exception, or null

	private volatile boolean closed;


	public BlockingQueueSource() {
		this(128);
	}

	public BlockingQueueSource(int capacity) {
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
	public boolean trySend(T item, Duration timeout) throws ClosedException, InterruptedException {
		return this.queue.offer(item, TimeUnit.MILLISECONDS.convert(timeout), TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean trySend(T item) {
		return this.queue.offer(item);
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
		// TODO: how to interrupt blocked receivers?
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
	public @Nullable T receive() throws IOException, ClosedException, InterruptedException {
		assertNotClosed();
		T item;
		try {
			item = this.queue.take();
		}
		catch (InterruptedException ex) {
			close();
			throw ex;
		}
		closeAfterCompletion();
		return item;
	}

	@Override
	public T tryReceive(Duration timeout) throws IOException, ClosedException, InterruptedException {
		assertNotClosed();
		T item;
		try {
			item = this.queue.poll(TimeUnit.MILLISECONDS.convert(timeout), TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException ex) {
			close();
			throw ex;
		}
		closeAfterCompletion();
		return item;
	}

	@Override
	public @Nullable T tryReceive() throws IOException, ClosedException {
		assertNotClosed();
		T item = this.queue.poll();
		closeAfterCompletion();
		return item;
	}

	private void assertNotClosed() throws IOException {
		if (isClosed()) {
			switch (this.completion) {
				case IOException ex -> throw ex;
				case RuntimeException ex -> throw ex;
				case Throwable ex -> throw new ClosedException(this, ex);
				case null, default -> throw (new ClosedException(this));
			}
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
		// onClose notifications
		this.queue.clear(); // discarded items
	}

}
