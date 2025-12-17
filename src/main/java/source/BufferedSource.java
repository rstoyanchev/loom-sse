package source;

import java.io.IOException;
import java.time.Duration;

/**
 * A {@link Source} that returns items from a buffer.
 * @param <T> the types of items received through the Source
 */
public interface BufferedSource<T> extends Source<T> {

	/**
	 *
	 */
	boolean tryReceiveNext(Duration timeout) throws IOException, InterruptedException;

	/**
	 *
	 */
	boolean tryReceiveNext() throws IOException;

}
