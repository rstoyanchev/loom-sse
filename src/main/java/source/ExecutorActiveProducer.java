package source;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

public class ExecutorActiveProducer<T> extends ActiveProducer<T> {

	private final ExecutorService executorService;

	private @Nullable Future<?> future;

	private final CountDownLatch producerLatch = new CountDownLatch(1);


	private ExecutorActiveProducer(Source<T> source, @Nullable ExecutorService executorService) {
		super(source, null);
		this.executorService = (executorService != null ?
				executorService : Executors.newVirtualThreadPerTaskExecutor());
	}


	@Override
	protected void startInternal() {
		this.future = this.executorService.submit(() -> {
			try {
				return getProducerTask().call();
			}
			finally {
				this.producerLatch.countDown();
			}
		});
	}

	@Override
	protected void stopInternal() {
		Assert.state(this.future != null, "Expected Future of Producer");
		this.future.cancel(true);
		try {
			this.producerLatch.await();
		}
		catch (InterruptedException ex) {
			logger.info("Interrupted while waiting for producer to stop");
		}
	}


	public static <T> ExecutorActiveProducer<T> create(Source<T> source) {
		return new ExecutorActiveProducer<>(source, null);
	}

}
