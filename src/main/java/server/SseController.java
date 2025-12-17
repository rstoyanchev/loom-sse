/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class SseController {

	private static final Logger logger = LogManager.getLogger(SseController.class);


	@GetMapping("/sse")
	public SseEmitter sse() {
		SseEmitter emitter = new SseEmitter();
		Thread.ofVirtual().start(() -> {
			try {
				emitter.send("data-1");
				Thread.sleep(5000);
				emitter.send("data-2");
				emitter.complete();
			}
			catch (Exception ex) {
				logger.error(ex);
			}
		});
		return emitter;
	}

	@GetMapping("/sse-complete-empty")
	public SseEmitter sseEmpty() {
		SseEmitter emitter = new SseEmitter();
		Thread.ofVirtual().start(() -> {
			try {
				Thread.sleep(2000);
				emitter.complete();
			}
			catch (Exception ex) {
				logger.error(ex);
			}
		});
		return emitter;
	}

	@GetMapping("/sse-complete-with-error")
	public SseEmitter sseError() {
		SseEmitter emitter = new SseEmitter();
		Thread.ofVirtual().start(() -> {
			try {
				emitter.send("data-1");
				Thread.sleep(2000);
				emitter.completeWithError(new Exception("simulated error"));
			}
			catch (Exception ex) {
				logger.error(ex);
			}
		});
		return emitter;
	}

}
