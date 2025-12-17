package client;

import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import source.ActiveSource;
import source.StructuredActiveSource;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.RequiredValueExchangeFunction;

public class ClientApp {

	private final static Log logger =  LogFactory.getLog(ClientApp.class);


	static void main(String[] args) throws Exception {

		RestClient client = RestClient.create("http://localhost:8080");

		runSourceWithReceive(client);
//		runActiveSourceWithTryReceive(client);

//		closeSource(client);
//		sourceCompletes(client);
//		sourceCompletesWithError(client);

		logger.info("Exiting");
		System.exit(0);
	}


	/**
	 * Receive from an ActiveSource in a loop.
	 */
	private static void runSourceWithReceive(RestClient client) throws Exception {

		try (ActiveSource<ServerSentEvent<String>> source =
					 client.get().uri("/sse").exchangeForRequiredValue(toActiveSource(), false)) {

			while (source.receiveNext()) {
				ServerSentEvent<String> event = source.next();
				logger.info("Got " + event);
			}
		}
	}

	/**
	 * Receive via ActiveSource with tryReceiveNext.
	 */
	private static void runActiveSourceWithTryReceive(RestClient client) throws Exception {

		try (ActiveSource<ServerSentEvent<String>> source =
					 client.get().uri("/sse").exchangeForRequiredValue(toActiveSource(), false)) {

			while (!source.isClosed()) {
				if (source.tryReceiveNext(Duration.ofSeconds(2))) {
					logger.info("Got " + source.next());
				}
				else {
					logger.info("Timed out");
				}
			}
		}
	}

	/**
	 * The closing of an ActiveSource should stop the receiver subtask.
	 */
	private static void closeSource(RestClient client) throws Exception {

		try (ActiveSource<ServerSentEvent<String>> source =
					 client.get().uri("/sse").exchangeForRequiredValue(toActiveSource(), false)) {

			if (source.tryReceiveNext(Duration.ofSeconds(2))) {
				logger.info("Got " + source.next());
			}
			else {
				logger.info("Timed out");
			}

			// Exit try-with-resources after 3 seconds
			Thread.sleep(1000);
		}
	}

	/**
	 * Completion of the receiver subtask should unblock receivers.
	 */
	private static void sourceCompletes(RestClient client) throws Exception {

		try (ActiveSource<ServerSentEvent<String>> source =
					 client.get().uri("/sse-complete-empty").exchangeForRequiredValue(toActiveSource(), false)) {

			boolean result = source.receiveNext();
			logger.info("Got " + result);
		}
	}


	/**
	 * Completion of the receiver subtask should unblock receivers.
	 */
	private static void sourceCompletesWithError(RestClient client) throws Exception {

		try (ActiveSource<ServerSentEvent<String>> source =
					 client.get().uri("/sse-complete-with-error").exchangeForRequiredValue(toActiveSource(), false)) {

			if (source.receiveNext()) {
				logger.info("Got " + source.next());
			}
			else {
				logger.info("Timed out");
			}

			try {
				source.receiveNext();
				throw new IllegalStateException("Expected exception");
			}
			catch (Exception ex) {
				logger.info("Error: " + ex.getMessage());
			}
		}
	}

	private static RequiredValueExchangeFunction<ActiveSource<ServerSentEvent<String>>> toActiveSource() {
		return (request, response) -> {
			if (response.getStatusCode().isError()) {
				throw response.createException();
			}
			ServerSentEventSource<String> source = new ServerSentEventSource<>(request, response);
			StructuredActiveSource<ServerSentEvent<String>> activeSource = StructuredActiveSource.from(source);
//			ExecutorServiceActiveSource<ServerSentEvent<String>> activeSource = ExecutorServiceActiveSource.from(source);
			return activeSource;
		};
	}

}
