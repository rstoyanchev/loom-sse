package sourcesse;

import java.io.IOException;
import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import source.ActiveSource;
import source.Source;

import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.client.RestClient;

public class ClientApp {

	private final static Log logger =  LogFactory.getLog(ClientApp.class);


	public static void main(String[] args) throws Exception {

		RestClient restClient = RestClient.create("http://localhost:8080");

		runSource(restClient);
//		runActiveSource(restClient);

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

	private static void runActiveSource(RestClient restClient) throws IOException, InterruptedException {

		ActiveSource<ServerSentEvent<String>> activeSource = restClient.get().uri("/sse")
				.exchangeForRequiredValue((request, response) -> {
					ServerSentEventSource<String> source = new ServerSentEventSource<>(request, response);
					return ActiveSource.builder(source).build();
				}, false);

		try (Source<ServerSentEvent<String>> source = activeSource.start()) {
			while (true) {
				ServerSentEvent<String> event = source.receive(Duration.ofSeconds(2));
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

}
