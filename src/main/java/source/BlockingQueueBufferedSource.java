package source;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

public class BlockingQueueBufferedSource<T> implements BufferedSource<T>, Sink<T> {

	private final BlockingQueue<T> queue;

	private @Nullable T nextItem;

	private volatile @Nullable Object completion; // Boolean.TRUE | IOException | RuntimeException

	private volatile boolean closed;


	public BlockingQueueBufferedSource() {
		this(128);
	}

	public BlockingQueueBufferedSource(int capacity) {
		this.queue = (capacity > 0 ? new LinkedBlockingQueue<>(capacity) : new SynchronousQueue<>());
	}


	// Sink


	@Override
	public void send(T item) throws InterruptedException {
		this.queue.put(item);
	}

	@Override
	public boolean trySend(T item, Duration timeout) throws InterruptedException {
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
	public boolean receiveNext() throws IOException, InterruptedException {
		if (this.closed) {
			return returnCompletion();
		}
		try {
			this.nextItem = this.queue.take();
		}
		catch (InterruptedException ex) {
			close();
			throw ex;
		}
		closeAfterCompletion();
		return true;
	}

	@Override
	public boolean tryReceiveNext(Duration timeout) throws IOException, InterruptedException {
		if (this.closed) {
			return returnCompletion();
		}
		try {
			this.nextItem = this.queue.poll(
					TimeUnit.MILLISECONDS.convert(timeout), TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException ex) {
			close();
			throw ex;
		}
		closeAfterCompletion();
		return (this.nextItem != null);
	}

	@Override
	public boolean tryReceiveNext() throws IOException {
		if (this.closed) {
			return returnCompletion();
		}
		this.nextItem = this.queue.poll();
		closeAfterCompletion();
		return (this.nextItem != null);
	}

	private boolean returnCompletion() throws IOException {
		return switch (this.completion) {
			case IOException ex -> throw ex;
			case RuntimeException ex -> throw ex;
			case Throwable ex -> throw new IllegalStateException(ex);
			case null, default -> false;
		};
	}

	private void closeAfterCompletion() {
		if (this.completion != null && this.queue.isEmpty()) {
			this.closed = true;
		}
	}

	@Override
	public T next() {
		T item = this.nextItem;
		if (item == null) {
			throw new IllegalStateException("No received event");
		}
		this.nextItem = null;
		return item;
	}

	@Override
	public void close() {
		this.closed = true;
		this.queue.clear(); // discarded items
	}

}
