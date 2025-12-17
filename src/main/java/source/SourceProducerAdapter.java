package source;

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
			try {
				while (this.source.receiveNext()) {
					sink.send(this.source.next());
				}
				sink.complete();
			}
			catch (InterruptedException ex) {
				sink.completeExceptionally(ex);
				throw ex;
			}
			catch (Throwable ex) {
				sink.completeExceptionally(ex);
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " for " + this.source;
	}

}
