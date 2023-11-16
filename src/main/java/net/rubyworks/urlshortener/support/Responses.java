package net.rubyworks.urlshortener.support;

import static org.springframework.web.reactive.function.server.ServerResponse.noContent;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.seeOther;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

public final class Responses {
    private Responses() {}

    public static Mono<ServerResponse> empty() {
        return noContent().build();
    }

    public static Mono<ServerResponse> json(Object body) {
        return ok().contentType(MediaType.APPLICATION_JSON).bodyValue(body);
    }

    public static Mono<ServerResponse> json(CompletableFuture<?> body) {
        return Mono.fromCompletionStage(body)
                .flatMap(Responses::json)
                .switchIfEmpty(empty());
    }

    public static Mono<ServerResponse> seeOthers(CompletableFuture<URI> body, URI defaultUri) {
        return Mono.fromCompletionStage(body)
                .flatMap(item -> seeOther(item).build())
                .switchIfEmpty(seeOther(defaultUri).build());
    }
}
