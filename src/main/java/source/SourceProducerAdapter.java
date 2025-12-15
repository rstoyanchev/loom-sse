package source;

import java.io.IOException;

/**
 * {@link Producer} that reads from a {@link Source} and pushes into a {@link Sink}.
 * In effect, converting from pull to a push style stream of items.
 *
 * @param <T> the items of items received from the source
 */
public class SourceProducerAdapter<T> implements Producer<T> {

	private final Source<T> source;


	public SourceProducerAdapter(Source<T> source) {
		this.source = source;
	}


	@Override
	public void produce(Sink<T> sink) throws InterruptedException {
		try (this.source) {
			while (true) {
				if (this.source.isClosed()) {
					Throwable cause = this.source.getCompletionException();
					if (cause != null) {
						sink.completeExceptionally(cause);
					}
					else {
						sink.complete();
					}
					return;
				}
				else if (sink.isComplete()) {
					this.source.close();
					break;
				}
				else {
					T item = null;
					try {
						item = this.source.receive();
						if (item == null) {
							continue;
						}
						sink.send(item);
					}
					catch (IOException ex) {
						sink.completeExceptionally(ex);
						return;
					}
					catch (ClosedException ex) {
						if (item != null) {
							// discarded item
						}
						return;
					}
					catch (InterruptedException ex) {
						sink.completeExceptionally(ex);
						throw ex;
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " for " + this.source;
	}

}
