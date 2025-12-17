package source;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

public class ExecutorActiveSource<T> extends AbstractActiveSource<T> {

	private final ExecutorService executorService;

	private @Nullable Future<?> future;

	private final CountDownLatch producerLatch = new CountDownLatch(1);


	private ExecutorActiveSource(Source<T> source) {
		super(source);
		this.executorService = Executors.newVirtualThreadPerTaskExecutor();
	}


	@Override
	protected void start(Callable<Void> producer) {
		this.future = this.executorService.submit(() -> {
			try {
				return producer.call();
			}
			finally {
				this.producerLatch.countDown();
			}
		});
	}

	@Override
	protected void stop() {
		Assert.state(this.future != null, "Expected Future of Producer");
		this.future.cancel(true);
		try {
			this.producerLatch.await();
		}
		catch (InterruptedException ex) {
			logger.info("Interrupted while waiting for producer to stop");
		}
	}


	public static <T> ExecutorActiveSource<T> create(Source<T> source) {
		return new ExecutorActiveSource<>(source);
	}

}
