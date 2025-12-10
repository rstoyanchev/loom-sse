package source;

import java.util.concurrent.Executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import org.springframework.core.task.VirtualThreadTaskExecutor;

class DefaultActiveSource<T> implements ActiveSource<T> {

	private static final Logger logger = LogManager.getLogger(DefaultActiveSource.class);


	private final Producer<T> producer;

	private final BlockingQueueSinkSource<T> sinkSource;

	private final Executor executor;


	public DefaultActiveSource(Producer<T> producer, @Nullable Executor executor) {
		this.producer = producer;
		this.sinkSource = new BlockingQueueSinkSource<>();
		this.executor = (executor != null ? executor : new VirtualThreadTaskExecutor());
	}


	@Override
	public Source<T> start() {
		// fork
		this.executor.execute(() -> {
			logger.info("Starting " + this.producer);
			try {
				this.producer.produce(this.sinkSource);
				// TODO: register for onClose notification to interrupt
			}
			catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		});
		return this.sinkSource;
	}

}
