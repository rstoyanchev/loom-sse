package client;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import source.Source;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.util.StringUtils;


public class ServerSentEventSource<T> implements Source<ServerSentEvent<T>> {

	private static final Logger logger = LogManager.getLogger(ServerSentEventSource.class);


	private final URI url;

	private final BufferedReader reader;

	private final HttpMessageConverterDelegate converterDelegate;

	private final Function<String, ResolvableType> typeResolver;

	private final Function<String, MediaType> contentTypeResolver;

	private @Nullable ServerSentEvent<T> receivedEvent;

	private volatile Object closure; // Boolean.TRUE | IOException | RuntimeException


	public ServerSentEventSource(URI url, InputStream inputStream) throws IOException {
		this(url, inputStream, HttpMessageConverters.forClient().build(),
				_ -> ResolvableType.forClass(String.class), _ -> MediaType.TEXT_PLAIN);
	}

	public ServerSentEventSource(
			URI url, InputStream responseBody, HttpMessageConverters converters,
			Function<String, ResolvableType> typeResolver, Function<String, MediaType> contentTypeResolver) {

		this.url = url;
		this.reader = new BufferedReader(new InputStreamReader(responseBody, StandardCharsets.UTF_8));
		this.converterDelegate = new HttpMessageConverterDelegate(converters);
		this.typeResolver = typeResolver;
		this.contentTypeResolver = contentTypeResolver;
	}


	@SuppressWarnings("unchecked")
	@Override
	public boolean receiveNext() throws IOException {
		if (this.closure != null) {
			return switch (this.closure) {
				case IOException ex -> throw ex;
				case RuntimeException ex -> throw ex;
				case Throwable ex -> throw wrapException(ex);
				default -> false;
			};
		}
		logger.debug("In receive()...");
		try {
			ServerSentEvent.Builder<T> eventBuilder = ServerSentEvent.builder();
			StringBuilder sb = new StringBuilder();
			String eventType = "";
			boolean empty = true;

			while (true) {
				String line = this.reader.readLine();
				if (line == null) {
					logger.debug("End of input stream");
					close();
					if (!empty) {
						throw new EOFException("Partial event: " + eventBuilder.build());
					}
					return false;
				}

				// End of event
				if (!StringUtils.hasText(line)) {
					ResolvableType targetType = this.typeResolver.apply(eventType);
					MediaType contentType = this.contentTypeResolver.apply(eventType);
					T t;
					if (targetType.getRawClass() == String.class) {
						t = (T) sb.toString();
					}
					else {
						t = this.converterDelegate.readWithMessageConverter(
								sb.toString().getBytes(StandardCharsets.UTF_8), targetType, contentType);
					}
					ServerSentEvent<T> event = eventBuilder.data(t).build();
					logger.debug("Received " + event);
					this.receivedEvent = event;
					return true;
				}

				// Ignore line
				int index = line.indexOf(':');
				if (index == 0) {
					continue;
				}

				empty = false;

				String field = (index != -1 ? line.substring(0, index) : line);
				String value = (index != -1 ? line.substring(index + 1) : "");

				switch (field) {
					case "event":
						eventBuilder.event(value);
						break;
					case "data":
						sb.append(value);
						break;
					case "id":
						eventBuilder.id(value);
						break;
					case "retry":
						eventBuilder.retry(Duration.ofMillis(Long.parseLong(value)));
						break;
				}
			}
		}
		catch (IOException | RuntimeException ex) {
			closeInternal(ex);
			throw ex;
		}
		catch (Throwable ex) {
			IllegalStateException wrapped = wrapException(ex);
			closeInternal(wrapped);
			throw wrapped;
		}
	}

	private static IllegalStateException wrapException(Throwable ex) {
		return new IllegalStateException("Unexpected exception", ex);
	}

	@Override
	public ServerSentEvent<T> next() {
		ServerSentEvent<T> event = this.receivedEvent;
		if (event == null) {
			throw new IllegalStateException("No received event");
		}
		this.receivedEvent = null;
		return event;
	}

	@Override
	public void close() {
		closeInternal(Boolean.TRUE);
	}

	private void closeInternal(Object closure) {
		if (this.closure != null) {
			return;
		}
		this.closure = closure;
		logger.debug("Closed" + (this.closure instanceof Throwable ex ? ": " + ex : ""));
	}

	@Override
	public String toString() {
		return "ServerSentEventSource[\"" + this.url + "\"]";
	}

}
