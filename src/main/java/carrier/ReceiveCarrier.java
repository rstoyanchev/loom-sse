package carrier;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.jspecify.annotations.Nullable;

/**
 *
 * @param <T>
 */
public interface ReceiveCarrier<T> extends Carriable<T> {

	/**
	 * Return {@code true} after the sending side is
	 * {@link SendCarrier#isClosedForSend() closedForSend} and all queued
	 * elements have been received.
	 */
	boolean isClosedForReceive();

	/**
	 * Consume or block if empty.
	 */
	T receive() throws ClosedException, InterruptedException;

	/**
	 * Consume or block if empty for the given duration.
	 */
	T receive(Duration timeout) throws ClosedException, InterruptedException, TimeoutException;

	/**
	 * Try to receive, and return {@code null} if empty or closed.
	 */
	@Nullable T tryReceive();

}
