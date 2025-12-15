package source;

/**
 * A Source that returns items from an internal buffer, and exposes {@link Sink}
 * to push items into the buffer.
 *
 * @param <T> the types of items received from the source
 */
public interface BufferingSource<T> extends Source<T>, Sink<T> {

}
