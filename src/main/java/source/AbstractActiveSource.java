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
 * Abstract base class for {@link ActiveSource} implementations.
 *
 * <p>Provides most of the implementation, by receiving from the delegate Source
 * and pushing into a {@link BlockingQueue}. Subclasses are implement methods to
 * {@link #start(Callable) start} and {@link #stop() stop} the receiver task.
 */
public abstract class AbstractActiveSource<T> implements ActiveSource<T> {

	protected final Logger logger = LogManager.getLogger(getClass());

	private static final Object COMPLETE = new Object();


	private final Source<T> delegate;

	private final BlockingQueue<Object> queue;

	private @Nullable T receivedItem;

	private boolean started;

	private volatile @Nullable Completion completion;

	private volatile boolean closed;


	public AbstractActiveSource(Source<T> delegate) {
		this.delegate = delegate;
		this.queue = new LinkedBlockingQueue<>(128);
	}


	/**
	 * Start the Receiver task.
	 */
	protected abstract void start(Callable<Void> receiver);

	/**
	 * Stop the Receiver task.
	 */
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
		startIfNecessary();
		try {
			Object item = this.queue.take();
			setReceivedItem(item);
		}
		catch (InterruptedException ex) {
			close();
			throw ex;
		}
		closeAfterCompletion();
		return (this.receivedItem != null);
	}

	@Override
	public boolean tryReceiveNext(Duration timeout) throws IOException, InterruptedException {
		if (this.closed) {
			return returnCompletion();
		}
		startIfNecessary();
		try {
			Object item = this.queue.poll(TimeUnit.MILLISECONDS.convert(timeout), TimeUnit.MILLISECONDS);
			setReceivedItem(item);
		}
		catch (InterruptedException ex) {
			close();
			throw ex;
		}
		closeAfterCompletion();
		return (this.receivedItem != null);
	}

	@Override
	public boolean tryReceiveNext() throws IOException {
		if (this.closed) {
			return returnCompletion();
		}
		startIfNecessary();
		Object item = this.queue.poll();
		setReceivedItem(item);
		closeAfterCompletion();
		return (this.receivedItem != null);
	}

	private boolean returnCompletion() throws IOException {
		Completion c = this.completion;
		if (c != null) {
			c.throwIfCompletedExceptionally();
		}
		return false;
	}

	private void startIfNecessary() {
		if (!this.started) {
			this.started = true;
			start(new ReceiveTask());
		}
	}

	@SuppressWarnings("unchecked")
	private void setReceivedItem(Object item) throws IOException {
		if (item == COMPLETE) {
			Completion c = this.completion;
			if (c == null) {
				throw new IllegalStateException("Completion not set");
			}
			c.throwIfCompletedExceptionally();
		}
		else {
			this.receivedItem = (T) item;
		}
	}

	private void closeAfterCompletion() {
		if (this.completion != null && this.queue.isEmpty()) {
			this.closed = true;
		}
	}

	@Override
	public T next() {
		T item = this.receivedItem;
		if (item == null) {
			throw new IllegalStateException("No received event");
		}
		this.receivedItem = null;
		return item;
	}

	@Override
	public void close() {
		this.closed = true;
		try {
			stop();
		}
		finally {
			this.queue.clear(); // discarded items
		}
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " for " + this.delegate.toString();
	}


	/**
	 * Represents the completion of the receiver task.
	 */
	private record Completion(@Nullable Throwable exception) {

		public void throwIfCompletedExceptionally() throws IOException {
			if (exception() == null) {
				return;
			}
			switch (exception) {
				case IOException ex -> throw ex;
				case RuntimeException ex -> throw ex;
				case Throwable ex -> throw new IllegalStateException(ex);
			}
		}
	}


	/**
	 * Receive items from the delegate Source, and put them in the BlockingQueue.
	 */
	private class ReceiveTask implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			try (delegate) {
				try {
					while (delegate.receiveNext()) {
						queue.put(delegate.next());
					}
					complete(null);
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

		private void complete(@Nullable Throwable ex) throws InterruptedException {
			completion = new Completion(ex);
			queue.put(COMPLETE);
		}
	}

}
