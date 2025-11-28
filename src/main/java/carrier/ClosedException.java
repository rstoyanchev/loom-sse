package carrier;

public class ClosedException extends IllegalStateException {

	private final AutoCloseable source;


	public ClosedException(AutoCloseable source) {
		this.source = source;
	}


	public AutoCloseable getSource() {
		return this.source;
	}
}
