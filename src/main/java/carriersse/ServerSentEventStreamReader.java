package carriersse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;

import carrier.BlockingQueueCarrier;
import carrier.Carrier;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.util.StringUtils;


public class ServerSentEventStreamReader {

	private final HttpMessageConverterDelegate converterDelegate;

	private final int carrierQueueCapacity;

	private final Function<String, ResolvableType> typeResolver;

	private final Function<String, MediaType> contentTypeResolver;


	public ServerSentEventStreamReader(
			HttpMessageConverters converters, int carrierQueueCapacity,
			Function<String, ResolvableType> typeResolver, Function<String, MediaType> contentTypeResolver) {

		this.converterDelegate = new HttpMessageConverterDelegate(converters);
		this.carrierQueueCapacity = carrierQueueCapacity;
		this.typeResolver = typeResolver;
		this.contentTypeResolver = contentTypeResolver;
	}


	public <T> Carrier<ServerSentEvent<T>> readStream(ClientHttpResponse response) {

		Carrier<ServerSentEvent<T>> carrier = new BlockingQueueCarrier<>(this.carrierQueueCapacity);

		try {
			InputStream inputStream = response.getBody();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

			ServerSentEvent.Builder<T> eventBuilder = ServerSentEvent.builder();
			StringBuilder sb = new StringBuilder();
			String eventType = "";

			while (true) {
				String line;
				line = reader.readLine();

				if (line == null) {
					carrier.close();
					return;
				}

				// End of event
				if (!StringUtils.hasText(line)) {
					ResolvableType targetType = this.typeResolver.apply(eventType);
					MediaType contentType = this.contentTypeResolver.apply(eventType);

					T data = this.converterDelegate.readWithMessageConverter(
							sb.toString().getBytes(StandardCharsets.UTF_8), targetType, contentType);

					ServerSentEvent<T> event = eventBuilder.data(data).build();
					carrier.send(event);

					eventBuilder = ServerSentEvent.builder();
					sb = new StringBuilder();
					eventType = "";
				}

				// Ignore line
				int index = line.indexOf(':');
				if (index == 0) {
					continue;
				}

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
		catch (IOException ex) {
			// ...
		}

		return carrier;
	}

}
