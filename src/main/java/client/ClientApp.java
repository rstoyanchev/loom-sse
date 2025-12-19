package client;

import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import source.ActiveSource;
import source.StructuredActiveSource;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.client.RestClient;

public class ClientApp {

	private final static Logger logger =  LogManager.getLogger(ClientApp.class);

	private final static RestClient client = RestClient.create("http://localhost:8080");


	static void main(String[] args) throws Exception {
		receiveScenario();
//		tryReceiveScenario();
//		closeSourceScenario();
//		sourceCompletesScenario();
//		sourceCompletesWithErrorScenario();
	}


	/**
	 * Receive in a loop.
	 */
	private static void receiveScenario() throws Exception {
		try (ActiveSource<ServerSentEvent<String>> source = performSseRequest("/sse")) {
			while (source.receiveNext()) {
				ServerSentEvent<String> event = source.next();
				logger.info("Got " + event);
			}
		}
	}

	/**
	 * Use tryReceive with a timeout that returns control.
	 */
	private static void tryReceiveScenario() throws Exception {
		try (ActiveSource<ServerSentEvent<String>> source = performSseRequest("/sse")) {
			while (!source.isClosed()) {
				if (source.tryReceiveNext(Duration.ofSeconds(2))) {
					logger.info("Got " + source.next());
				}
				else {
					logger.info("Timed out, trying again");
				}
			}
		}
	}

	/**
	 * Closing the Source from the receiving side stops the receiver subtask.
	 */
	private static void closeSourceScenario() throws Exception {
		try (ActiveSource<ServerSentEvent<String>> source = performSseRequest("/sse")) {
			if (source.tryReceiveNext(Duration.ofSeconds(2))) {
				logger.info("Got " + source.next());
			}
			else {
				logger.info("Timed out");
			}
		}
	}

	/**
	 * Completion of the receiver subtask should unblock receivers.
	 */
	private static void sourceCompletesScenario() throws Exception {
		try (ActiveSource<ServerSentEvent<String>> source = performSseRequest("/sse-complete-empty")) {
			boolean result = source.receiveNext();
			logger.info("Got " + result);
		}
	}

	/**
	 * Completion of the receiver subtask with an error should propagate to blocked receivers.
	 */
	private static void sourceCompletesWithErrorScenario() throws Exception {
		try (ActiveSource<ServerSentEvent<String>> source = performSseRequest("/sse-complete-with-error")) {
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

	private static ActiveSource<ServerSentEvent<String>> performSseRequest(String path) {
		return client.get().uri(path).exchangeForRequiredValue((request, response) -> {
			if (response.getStatusCode().isError()) {
				throw response.createException();
			}
			ServerSentEventSource<String> source = new ServerSentEventSource<>(request.getURI(), response.getBody());
			return StructuredActiveSource.from(source); // or use ExecutorServiceActiveSource

		}, false);
	}

}
