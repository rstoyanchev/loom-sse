package client;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import source.ClosedException;
import source.Source;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.util.StringUtils;


public class ServerSentEventSource<T> implements Source<ServerSentEvent<T>> {

	private static final Logger logger = LogManager.getLogger(ServerSentEventSource.class);


	private final HttpRequest request;

	private final HttpMessageConverterDelegate converterDelegate;

	private final Function<String, ResolvableType> typeResolver;

	private final Function<String, MediaType> contentTypeResolver;

	private final BufferedReader reader;

	private volatile Object closure;


	public ServerSentEventSource(HttpRequest request, ClientHttpResponse response) throws IOException {
		this(request, response, HttpMessageConverters.forClient().build(),
				_ -> ResolvableType.forClass(String.class), _ -> MediaType.TEXT_PLAIN);
	}

	public ServerSentEventSource(
			HttpRequest request, ClientHttpResponse response,
			HttpMessageConverters converters,
			Function<String, ResolvableType> typeResolver,
			Function<String, MediaType> contentTypeResolver) throws IOException {

		this.request = request;
		this.converterDelegate = new HttpMessageConverterDelegate(converters);
		this.typeResolver = typeResolver;
		this.contentTypeResolver = contentTypeResolver;
		this.reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
	}


	@Override
	public boolean isClosed() {
		return (this.closure != null);
	}

	@Override
	public @Nullable Throwable getCompletionException() {
		return ((this.closure instanceof Throwable ex) ? ex : null);
	}


	@SuppressWarnings("unchecked")
	@Override
	public ServerSentEvent<T> receive() throws IOException, ClosedException, InterruptedException {
		assertNotClosed();
		logger.info("In receive()...");
		try {
			ServerSentEvent.Builder<T> eventBuilder = ServerSentEvent.builder();
			StringBuilder sb = new StringBuilder();
			String eventType = "";
			boolean empty = true;

			while (true) {
				String line = this.reader.readLine();
				if (line == null) {
					logger.info("End of input stream");
					close();
					if (!empty) {
						throw new EOFException("Partial event: " + eventBuilder.build());
					}
					return null;
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
					logger.info("Received " + event);
					return event;
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
		catch (Throwable ex) {
			close(ex);
			throw ex;
		}
	}

	@Override
	public @Nullable ServerSentEvent<T> tryReceive(Duration timeout) throws ClosedException {
		assertNotClosed();
		return null;
	}

	@Override
	public @Nullable ServerSentEvent<T> tryReceive() {
		assertNotClosed();
		return null;
	}

	@Override
	public void close() {
		close(Boolean.TRUE);
	}

	private void close(Object closure) {
		if (this.closure != null) {
			return;
		}
		this.closure = closure;
		logger.info("Closed" + (this.closure instanceof Throwable ex ? ": " + ex : ""));
	}

	private void assertNotClosed() {
		if (isClosed()) {
			throw new ClosedException(this);
		}
	}

	@Override
	public String toString() {
		return "ServerSentEventSource[\"" + this.request.getURI() + "\"]";
	}
}
