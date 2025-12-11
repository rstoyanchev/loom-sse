package source;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

public class ExecutorActiveProducer<T> extends ActiveProducer<T> {

	private static final Logger logger = LogManager.getLogger(ExecutorActiveProducer.class);

	private final Executor executor;


	private ExecutorActiveProducer(Producer<T> producer, @Nullable Executor executor) {
		super(producer, new BlockingQueueBufferingSource<>());
		this.executor = (executor != null ? executor : Executors.newVirtualThreadPerTaskExecutor());
	}


	@Override
	protected void startInternal() {
		this.executor.execute(() -> {
			logger.info("Starting " + getProducer());
			try {
				getProducer().produce(getSink());
				// TODO: register for onClose notification to interrupt
			}
			catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	@Override
	protected void stopInternal() {
	}


	public static <T> ExecutorActiveProducer<T> create(Source<T> source) {
		SourceProducer<T> producer = new SourceProducer<>(source);
		return new ExecutorActiveProducer<>(producer, null);
	}

}
