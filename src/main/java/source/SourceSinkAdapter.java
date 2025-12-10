package source;

import java.io.IOException;

public class SourceSinkAdapter<T> implements Producer<T> {

	private final Source<T> source;


	public SourceSinkAdapter(Source<T> source) {
		this.source = source;
	}


	@Override
	public String toString() {
		return "SourceSinkAdapter for " + this.source;
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

}
