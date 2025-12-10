package source;

import org.jspecify.annotations.Nullable;

public class ClosedException extends IllegalStateException {

	private final AutoCloseable source;


	public ClosedException(AutoCloseable source) {
		this(source, null);
	}

	public ClosedException(AutoCloseable source, @Nullable Throwable cause) {
		super(cause);
		this.source = source;
	}


	public AutoCloseable getSource() {
		return this.source;
	}
}
