package carriersse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.SmartHttpMessageConverter;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClientException;

final class HttpMessageConverterDelegate {

	private final HttpMessageConverters converters;


	HttpMessageConverterDelegate(HttpMessageConverters converters) {
		this.converters = converters;
	}


	@SuppressWarnings({"rawtypes", "unchecked"})
	public <T> @Nullable T readWithMessageConverter(
			byte[] content, ResolvableType targetType, @Nullable MediaType contentType) {

		if (ObjectUtils.isEmpty(content)) {
			return null;
		}

		HttpInputMessage inputMessage = new ByteArrayHttpInputMessage(contentType, content);

		try {
			for (HttpMessageConverter<?> converter : this.converters) {
				if (converter instanceof GenericHttpMessageConverter genericConverter) {
					if (genericConverter.canRead(targetType.getType(), null, contentType)) {
						return (T) genericConverter.read(targetType.getType(), null, inputMessage);
					}
				}
				else if (converter instanceof SmartHttpMessageConverter smartConverter) {
					if (smartConverter.canRead(targetType, contentType)) {
						return (T) smartConverter.read(targetType, inputMessage, null);
					}
				}
				else {
					if (converter.canRead(targetType.resolve(Object.class), contentType)) {
						return (T) converter.read((Class) targetType.resolve(Object.class), inputMessage);
					}
				}
			}
			throw new IllegalArgumentException(
					"No HttpMessageConverter for type " + targetType + " and content type " + contentType);
		}
		catch (UncheckedIOException | IOException | HttpMessageNotReadableException exc) {
				Throwable cause;
				if (exc instanceof UncheckedIOException uncheckedIOException) {
					cause = uncheckedIOException.getCause();
				}
				else {
					cause = exc;
				}
				throw new RestClientException("Error while converting to type [" + targetType + "]", cause);
			}
	}


	private static class ByteArrayHttpInputMessage implements HttpInputMessage {

		private final HttpHeaders headers;

		private final InputStream content;

		ByteArrayHttpInputMessage(@Nullable MediaType mediaType, byte[] content) {
			this.headers = new HttpHeaders();
			if (mediaType != null) {
				this.headers.setContentType(mediaType);
			}
			this.content = new ByteArrayInputStream(content);
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
		}

		@Override
		public InputStream getBody() {
			return this.content;
		}
	}

}
