package source;

import carrier.ClosedException;

public class SourceSinkAdapter<T> implements Producer<T> {

	private final Source<T> source;


	public SourceSinkAdapter(Source<T> source) {
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
					break;
				}
				else if (sink.isComplete()) {
					this.source.close();
					break;
				}
				else {
					T item = null;
					try {
						item = this.source.receive();
						sink.send(item);
					}
					catch (ClosedException ex) {
						if (item != null) {
							// discarded item
						}
						break;
					}
				}
			}
		}
	}

}
