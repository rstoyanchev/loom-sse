package source;

import java.util.concurrent.Executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import org.springframework.core.task.VirtualThreadTaskExecutor;

class ConduitActiveSource<T> implements ActiveSource<T> {

	private static final Logger logger = LogManager.getLogger(ConduitActiveSource.class);


	private final Producer<T> producer;

	private final Conduit<T> conduit;

	private final Executor executor;


	public ConduitActiveSource(Producer<T> producer, @Nullable Executor executor) {
		this.producer = producer;
		this.conduit = new BlockingQueueConduit<>();
		this.executor = (executor != null ? executor : new VirtualThreadTaskExecutor());
	}


	@Override
	public Source<T> start() {
		this.executor.execute(() -> {
			logger.info("Starting " + this.producer);
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
