package client;

import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import source.ActiveProducer;
import source.BufferedSource;
import source.StructuredActiveProducer;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.RequiredValueExchangeFunction;

public class ClientApp {

	private final static Log logger =  LogFactory.getLog(ClientApp.class);


	static void main(String[] args) throws Exception {

		RestClient client = RestClient.create("http://localhost:8080");

//		runSource(restClient);
		runBlockingQueueSource(client);
//		cancelBlockingQueueSource(client);

		logger.info("Exiting");
		System.exit(0);
	}

	private static void runSource(RestClient client) throws Exception {

		try (ServerSentEventSource<String> source =
					 client.get().uri("/sse").exchangeForRequiredValue(ServerSentEventSource::new, false)) {

			while (true) {
				ServerSentEvent<String> event = source.receive();
				if (event == null) {
					logger.info("No more events");
					break;
				}
				logger.info("Got " + event);
			}
		}
	}

	private static void runBlockingQueueSource(RestClient client) throws Exception {

		try (BufferedSource<ServerSentEvent<String>> source =
					 client.get().uri("/sse").exchangeForRequiredValue(toBufferedSource(), false)) {

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
				logger.info("Got " + event);
			}
		}
	}

	private static void cancelBlockingQueueSource(RestClient client) throws Exception {

		try (BufferedSource<ServerSentEvent<String>> source =
					 client.get().uri("/sse").exchangeForRequiredValue(toBufferedSource(), false)) {

			ServerSentEvent<String> event = source.tryReceive(Duration.ofSeconds(2));
			logger.info("Got " + event);

			Thread.sleep(1000);
		}
	}

	private static RequiredValueExchangeFunction<BufferedSource<ServerSentEvent<String>>> toBufferedSource() {
		return (request, response) -> {
			ServerSentEventSource<String> source = new ServerSentEventSource<>(request, response);
			ActiveProducer<ServerSentEvent<String>> producer = StructuredActiveProducer.create(source);
//			ActiveProducer<ServerSentEvent<String>> producer = ExecutorActiveProducer.create(source);
			producer.start();
			return producer.bufferedSource();
		};
	}

}
