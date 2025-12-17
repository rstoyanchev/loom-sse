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

	private final CountDownLatch receiverLatch = new CountDownLatch(1);


	private ExecutorActiveSource(Source<T> source) {
		super(source);
		this.executorService = Executors.newVirtualThreadPerTaskExecutor();
	}


	@Override
	protected void start(Callable<Void> receiver) {
		this.future = this.executorService.submit(() -> {
			try {
				return receiver.call();
			}
			finally {
				this.receiverLatch.countDown();
			}
		});
	}

	@Override
	protected void stop() {
		Assert.state(this.future != null, "Expected Future of receiver task");
		this.future.cancel(true);
		try {
			this.receiverLatch.await();
		}
		catch (InterruptedException ex) {
			logger.info("Interrupted while waiting for receiver task to stop");
		}
	}


	public static <T> ExecutorActiveSource<T> from(Source<T> source) {
		return new ExecutorActiveSource<>(source);
	}

}
