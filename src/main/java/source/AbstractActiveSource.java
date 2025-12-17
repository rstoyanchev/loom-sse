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
 * implementation, with subclasses responsible for managing the starting and
 * stopping the active task.
 */
public abstract class AbstractActiveSource<T> implements ActiveSource<T> {

	protected final Logger logger = LogManager.getLogger(getClass());


	private final Source<T> source;

	private final BlockingQueue<Object> queue;

	private @Nullable T nextItem;

	private boolean started;

	private volatile @Nullable Completion completion;

	private volatile boolean closed;


	public AbstractActiveSource(Source<T> source) {
		this.source = source;
		this.queue = new LinkedBlockingQueue<>(128);
	}


	/**
	 * Start the receiving task.
	 */
	protected abstract void start(Callable<Void> receiver);

	/**
	 * Stop the receiving task.
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
			setNextItem(item);
		}
		catch (InterruptedException ex) {
			close();
			throw ex;
		}
		closeAfterCompletion();
		return (this.nextItem != null);
	}

	@Override
	public boolean tryReceiveNext(Duration timeout) throws IOException, InterruptedException {
		if (this.closed) {
			return returnCompletion();
		}
		startIfNecessary();
		try {
			Object item = this.queue.poll(TimeUnit.MILLISECONDS.convert(timeout), TimeUnit.MILLISECONDS);
			setNextItem(item);
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
		startIfNecessary();
		Object item = this.queue.poll();
		setNextItem(item);
		closeAfterCompletion();
		return (this.nextItem != null);
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
	private void setNextItem(Object item) throws IOException {
		if (item instanceof Completion c) {
			this.completion = c;
			c.throwIfCompletedExceptionally();
		}
		else {
			this.nextItem = (T) item;
		}
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
		try {
			stop();
		}
		finally {
			this.queue.clear(); // discarded items
		}
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " for " + this.source.toString();
	}


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
	 * Task that receives from the target Source, and puts items into the BlockingQueue.
	 */
	private class ReceiveTask implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			try (source) {
				try {
					while (source.receiveNext()) {
						queue.put(source.next());
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
			Completion c = new Completion(ex);
			queue.put(c);
		}
	}

}
