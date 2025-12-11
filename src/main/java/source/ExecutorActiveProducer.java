package source;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jspecify.annotations.Nullable;

public class ExecutorActiveProducer<T> extends ActiveProducer<T> {

	private final Executor executor;


	private ExecutorActiveProducer(Source<T> source, @Nullable Executor executor) {
		super(source, null);
		this.executor = (executor != null ? executor : Executors.newVirtualThreadPerTaskExecutor());
	}


	@Override
	protected void startInternal() {
		this.executor.execute(() -> {
			try {
				produce();
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
		return new ExecutorActiveProducer<>(source, null);
	}

}
