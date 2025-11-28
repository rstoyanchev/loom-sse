package carrier;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jspecify.annotations.Nullable;

/**
 *
 * @param <T>
 */
public class BlockingQueueCarrier<T> implements Carrier<T> {

	private final BlockingQueue<T> queue;

	private volatile @Nullable Object closed; // Boolean.TRUE or Throwable


	public BlockingQueueCarrier(int capacity) {
		this.queue = (capacity > 0 ? new LinkedBlockingQueue<>(capacity) : new SynchronousQueue<>());
	}


	@Override
	public boolean isEmpty() {
		return this.queue.isEmpty();
	}

	@Override
	public boolean isClosedForSend() {
		return (this.closed != null);
	}

	@Override
	public boolean isClosedForReceive() {
		return (isClosedForSend() && this.queue.isEmpty());
	}

	@Override
	public @Nullable Throwable getCloseException() {
		return (this.closed instanceof Throwable ? (Throwable) this.closed : null);
	}


	@Override
	public T receive() throws ClosedException, InterruptedException {
		assertNotClosedForReceive();
		return this.queue.take();
	}

	@Override
	public T receive(Duration timeout) throws ClosedException, InterruptedException, TimeoutException {
		assertNotClosedForReceive();
		T element = this.queue.poll(timeout.getNano(), TimeUnit.NANOSECONDS);
		if (element == null) {
			throw new TimeoutException();
		}
		return element;
	}

	@Override
	public @Nullable T tryReceive() {
		if (isClosedForReceive()) {
			return null;
		}
		return this.queue.poll();
	}

	private void assertNotClosedForReceive() {
		if (isClosedForReceive()) {
			throw new ClosedException(this);
		}
	}

	@Override
	public void send(T element) throws ClosedException, InterruptedException {
		assertNotClosedForSend();
		this.queue.put(element);
	}

	@Override
	public void send(T element, Duration timeout) throws ClosedException, InterruptedException, TimeoutException {
		assertNotClosedForSend();
		if (!this.queue.offer(element, timeout.getNano(), TimeUnit.NANOSECONDS)) {
			throw new TimeoutException();
		}
	}

	@Override
	public boolean trySend(T element) {
		return this.queue.offer(element);
	}

	private void assertNotClosedForSend() {
		if (isClosedForSend()) {
			throw new ClosedException(this);
		}
	}

	@Override
	public void close() {
		if (isClosedForSend()) {
			return;
		}
		this.closed = Boolean.TRUE;
	}

	@Override
	public void closeExceptionally(Throwable cause) {
		if (isClosedForSend()) {
			return;
		}
		this.closed = cause;
	}

}
