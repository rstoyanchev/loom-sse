package source;

import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

class DefaultActiveSourceBuilder<T> implements ActiveSource.Builder<T> {

	private final Producer<T> producer;

	private @Nullable Executor executor;


	DefaultActiveSourceBuilder(Producer<T> producer) {
		this.producer = producer;
	}


	@Override
	public ActiveSource.Builder<T> executor(@Nullable Executor executor) {
		this.executor = executor;
		return this;
	}

	@Override
	public ActiveSource<T> build() {
		return new DefaultActiveSource<>(this.producer, this.executor);
	}
}
