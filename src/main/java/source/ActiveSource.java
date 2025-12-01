package source;

import java.util.concurrent.Executor;

public interface ActiveSource<T> {


	Source<T> start();


	static <T> Builder<T> builder(Source<T> source) {
		return builder((Producer<T>) new SourceSinkAdapter<T>(source));
	}

	static <T> Builder<T> builder(Producer<T> producer) {
		return new DefaultActiveSourceBuilder<>(producer);
	}


	interface Builder<T> {

		Builder<T> executor(Executor executor);

		ActiveSource<T> build();
	}

}
