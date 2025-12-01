package source;

import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

class ConduitActiveSource<T> implements ActiveSource<T> {

	private final Producer<T> producer;

	private final Conduit<T> conduit;

	private final Executor executor;


	public ConduitActiveSource(Producer<T> producer, @Nullable Executor executor) {
		this.producer = producer;
		this.conduit = new BlockingQueueConduit<>();
		this.executor = executor;
	}


	@Override
	public Conduit<T> start() {
		this.executor.execute(() -> {
			try {
				this.producer.produce(this.conduit);
			}
			catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		});
		return this.conduit;
	}

}
