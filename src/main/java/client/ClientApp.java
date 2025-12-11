package client;

import java.io.IOException;
import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import source.ActiveProducer;
import source.Source;
import source.StructuredActiveProducer;

import org.springframework.http.HttpRequest;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;

public class ClientApp {

	private final static Log logger =  LogFactory.getLog(ClientApp.class);


	public static void main(String[] args) throws Exception {

		RestClient restClient = RestClient.create("http://localhost:8080");

//		runSource(restClient);
		runBufferingSource(restClient);

		logger.info("Exiting");
		System.exit(0);
	}

	private static void runSource(RestClient restClient) throws IOException, InterruptedException {

		try (ServerSentEventSource<String> source =
					 restClient.get().uri("/sse").exchangeForRequiredValue(ServerSentEventSource::new, false)) {

			while (true) {
				ServerSentEvent<String> event = source.receive();
				if (event == null) {
					logger.info("No more events");
					break;
				}
				logger.info("Got " + event.data());
			}
		}
	}

	private static void runBufferingSource(RestClient restClient) throws IOException, InterruptedException {

		try (Source<ServerSentEvent<String>> source =
					 restClient.get().uri("/sse").exchangeForRequiredValue(ClientApp::toBufferingSource, false)) {

			while (true) {
				ServerSentEvent<String> event = source.tryReceive(Duration.ofSeconds(2));
				if (event == null) {
					if (source.isClosed()) {
						logger.info("Source closed");
						break;
					}
					logger.info("Timed out waiting for event");
					continue;
				}
				logger.info("Got " + event.data());
			}
		}
	}

	private static Source<ServerSentEvent<String>> toBufferingSource(
			HttpRequest request, ConvertibleClientHttpResponse response) throws IOException {

		ServerSentEventSource<String> source = new ServerSentEventSource<>(request, response);
		ActiveProducer<ServerSentEvent<String>> producer = StructuredActiveProducer.create(source);
		producer.start();
		return producer.getSource();
	}

}
