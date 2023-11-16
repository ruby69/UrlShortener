package net.rubyworks.urlshortener.query;

import static net.rubyworks.urlshortener.support.Responses.json;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
public class QueryHandler {
    private final QueryCompletableFuture cfService;

    public Mono<ServerResponse> byAll(ServerRequest request) {
        return json(cfService.byAll());
    }

    public Mono<ServerResponse> byDuration(ServerRequest request) {
        return json(cfService.byDuration(request.pathVariable("duration")));
    }
}
