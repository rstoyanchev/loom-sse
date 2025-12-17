package source;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Base class for {@link ActiveSource} implementations that provides most of the
 * implementation, and allows subclasses to manage the producing subtask.
 */
public abstract class AbstractActiveSource<T> implements ActiveSource<T> {

	protected final Logger logger = LogManager.getLogger(getClass());


	private final Source<T> source;

	private final BlockingQueue<T> queue;

	private @Nullable T nextItem;

	private boolean started;

	private volatile @Nullable Object completion; // Boolean.TRUE | IOException | RuntimeException

	private volatile boolean closed;


	public AbstractActiveSource(Source<T> source) {
		this.source = source;
		this.queue = new LinkedBlockingQueue<>(128);
	}


	protected abstract void start(Callable<Void> producer);

	private void checkStarted() {
		if (!this.started) {
			this.started = true;
			start(new Producer());
		}
	}

	protected abstract void stop();


	@Override
	public boolean isClosed() {
		return this.closed;
	}

	@Override
	public boolean receiveNext() throws IOException, InterruptedException {
		if (this.closed) {
			return returnCompletion();
		}
		checkStarted();
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
		checkStarted();
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
		checkStarted();
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
		try {
			stop();
		}
		finally {
			this.closed = true;
			this.queue.clear(); // discarded items
		}
	}


	private void complete(Object completion) {
		// TODO: how to interrupt blocked receivers?
		if (this.completion == null) {
			this.completion = completion;
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " for " + this.source.toString();
	}


	private class Producer implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			try (source) {
				try {
					while (source.receiveNext()) {
						queue.put(source.next());
					}
					complete(Boolean.TRUE);
				}
				catch (InterruptedException ex) {
					complete(ex);
					throw ex;
				}
				catch (Throwable ex) {
					complete(ex);
				}
			}
			return null;
		}
	}

}
