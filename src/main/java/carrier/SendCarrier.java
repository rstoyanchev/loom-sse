package carrier;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 *
 * @param <T>
 */
public interface SendCarrier<T> extends Carriable<T> {

	/**
	 * Return {@code true} if either {@link #close()} or {@link #closeExceptionally}
	 * have been called, and it is no longer possible to send.
	 */
	boolean isClosedForSend();

	/**
	 * Send or block until element can be received or queued.
	 */
	void send(T element) throws ClosedException, InterruptedException;

	/**
	 * Variant of {@link #send(Object)} that times out after the given duration if blocked.
	 */
	void send(T element, Duration timeout) throws ClosedException, InterruptedException, TimeoutException;

	/**
	 * Try to send without blocking.
	 * @return return false if full or closed
	 */
	boolean trySend(T element);

}
