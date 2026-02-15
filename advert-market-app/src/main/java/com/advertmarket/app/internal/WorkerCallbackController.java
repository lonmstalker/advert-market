package com.advertmarket.app.internal;

import com.advertmarket.shared.event.WorkerCallback;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP fallback endpoint for worker callback events.
 *
 * <p>Used for debug/manual replay when Kafka is unavailable.
 */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class WorkerCallbackController {

    private final WorkerCallbackHandler handler;

    /** Receives a worker callback and dispatches it for processing. */
    @PostMapping("/worker-events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public WorkerCallbackResponse handleCallback(
            @RequestBody WorkerCallback callback) {
        return handler.handle(callback);
    }
}

