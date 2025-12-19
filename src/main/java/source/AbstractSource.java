package source;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Convenient base class for {@link Source} implementations that implements the
 * receiving loop, exception handling, and lifecycle management.
 * Subclasses need to implement {@link #receiveItem()}.
 */
public abstract class AbstractSource<T> implements Source<T> {

	protected final Logger logger = LogManager.getLogger(getClass());

	private @Nullable T receivedItem;

	private volatile Object closure; // Boolean.TRUE | IOException | RuntimeException


	protected AbstractSource() {
	}


	@Override
	public boolean receiveNext() throws IOException {
		if (this.closure != null) {
			return switch (this.closure) {
				case IOException ex -> throw ex;
				case RuntimeException ex -> throw ex;
				case Throwable ex -> throw wrapException(ex);
				default -> false;
			};
		}
		logger.debug("In receiveNext()...");
		try {
			T item = receiveItem();
			if (item == null) {
				logger.debug("Source ended");
				close();
				return false;
			}
			else {
				logger.debug("received {}", item);
				this.receivedItem = item;
				return true;
			}
		}
		catch (IOException | RuntimeException ex) {
			closeInternal(ex);
			throw ex;
		}
		catch (Throwable ex) {
			IllegalStateException wrapped = wrapException(ex);
			closeInternal(wrapped);
			throw wrapped;
		}
	}

	private static IllegalStateException wrapException(Throwable ex) {
		return new IllegalStateException("Unexpected exception", ex);
	}

	/**
	 * Subclasses implement this to retrieve the next item.
	 */
	protected abstract @Nullable T receiveItem() throws IOException, InterruptedException;

	@Override
	public T next() {
		T item = this.receivedItem;
		if (item == null) {
			throw new IllegalStateException("No received item");
		}
		this.receivedItem = null;
		return item;
	}

	@Override
	public void close() {
		closeInternal(Boolean.TRUE);
	}

	private void closeInternal(Object closure) {
		if (this.closure != null) {
			return;
		}
		this.closure = closure;
		logger.debug("Closed with " + (this.closure instanceof Throwable ex ? ex : "success"));
	}

}
