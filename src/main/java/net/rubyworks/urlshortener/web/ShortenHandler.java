package net.rubyworks.urlshortener.web;

import static net.rubyworks.urlshortener.support.Responses.empty;
import static net.rubyworks.urlshortener.support.Responses.json;
import static net.rubyworks.urlshortener.support.Responses.seeOthers;
import static org.springframework.web.reactive.function.server.ServerResponse.seeOther;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.RequiredArgsConstructor;
import net.rubyworks.urlshortener.domain.Shorten;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
public class ShortenHandler {
    @Value("${app_props.shortener-url}") private String shortenerUrl;
    @Value("${app_props.home-url}") private String homeUrl;

    private final ShortenCompletableFuture cfService;

    public Mono<ServerResponse> id(ServerRequest request) {
        var defaultUri = URI.create(homeUrl);
        var userAgent = request.headers().firstHeader("user-agent");
        if (!StringUtils.hasText(userAgent) || !userAgent.startsWith("Mozilla")) {
            return seeOther(defaultUri).build();
        }

        return seeOthers(cfService.id(request.pathVariable("id")), defaultUri);
    }

    public Mono<ServerResponse> save(ServerRequest request) {
        return request.bodyToMono(Shorten.Param.class)
                .flatMap(param -> {
                    if (!param.isValid()) {
                        return json(Map.of("to", param.url()));
                    }

                    return Mono.fromCompletionStage(cfService.save(param))
                            .flatMap(item -> json(item.url(shortenerUrl)))
                            .switchIfEmpty(empty());
                });
    }

    public Mono<ServerResponse> byAll(ServerRequest request) {
        return json(cfService.byAll());
    }

    private static final URI URI_LIST_ALL = URI.create("/api/list/all");
    public Mono<ServerResponse> delete(ServerRequest request) {
        return seeOthers(cfService.delete(request.pathVariable("id")), URI_LIST_ALL);
    }

    public Mono<ServerResponse> bulk(ServerRequest request) {
        return request.bodyToMono(new ParameterizedTypeReference<List<Shorten.Param>>() {})
                .flatMap(params -> json(cfService.bulk(params)));
    }

    public Mono<ServerResponse> byDuration(ServerRequest request) {
        return json(cfService.byDuration(request.pathVariable("duration")));
    }
}
