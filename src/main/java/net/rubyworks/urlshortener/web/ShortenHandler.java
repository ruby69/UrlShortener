package net.rubyworks.urlshortener.web;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.seeOther;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
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

    private final ShortenerService shortnerService;

    public Mono<ServerResponse> id(ServerRequest request) {
        var defaultUri = URI.create(homeUrl);
        var userAgent = request.headers().firstHeader("user-agent");
        if (!StringUtils.hasText(userAgent) || !userAgent.startsWith("Mozilla")) {
            return seeOther(defaultUri).build();
        }

        var uri = Optional.ofNullable(shortnerService.findAndUpdate(request.pathVariable("id")))
                .map(Shorten::uri)
                .orElse(defaultUri);

        return seeOther(uri).build();
    }

    public Mono<ServerResponse> save(ServerRequest request) {
        return request.bodyToMono(Shorten.Param.class)
                .flatMap(param -> {
                    if (!param.isValid()) {
                        return json(Map.of("to", param.url()));
                    }

                    var shorten = shortnerService.create(param.url(), param.count(), param.ttl());
                    return json(shorten.url(shortenerUrl));
                });
    }

    private Mono<ServerResponse> json(Object body) {
        return ok().contentType(MediaType.APPLICATION_JSON).bodyValue(body);
    }

    public Mono<ServerResponse> byAll(ServerRequest request) {
        return json(shortnerService.findAll());
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        shortnerService.deleteBy(request.pathVariable("id"));
        return seeOther(URI.create("/api/list/all")).build();
    }

    public Mono<ServerResponse> bulk(ServerRequest request) {
        return request.bodyToMono(new ParameterizedTypeReference<List<Shorten.Param>>() {})
                .flatMap(params -> {
                    params.forEach(param -> shortnerService.create(param.id(), param.url(), param.count(), param.ttl()));
                    return json(shortnerService.findAll());
                });
    }

    private static final String REGEX_DURATION = "^[0-9]+[mhdw]$";
    private static final Pattern PATTERN_DURATION = Pattern.compile(REGEX_DURATION);

    public Mono<ServerResponse> byDuration(ServerRequest request) {
        var list = shortnerService.findAll();

        var duration = request.pathVariable("duration");
        if (!StringUtils.hasText(duration) || !PATTERN_DURATION.matcher(duration).matches()) {
            return json(list);
        }

        var timeMillis = timeMillis(duration);
        return json(list.stream()
                .filter(it -> !it.isFixed() && it.getModifiedAt() > timeMillis)
                .sorted(Comparator.comparing(Shorten::getModifiedAt).reversed())
                .toList());
    }

    private long timeMillis(String duration) {
        var last = duration.substring(duration.length() - 1);
        var period = Long.parseLong(duration.substring(0, duration.length() - 1));

        var now = LocalDateTime.now();
        LocalDateTime time = null;
        if ("h".equals(last)) {         // hours
            time = now.minusHours(period);
        } else if ("d".equals(last)) {  // days
            time = now.minusDays(period);
        } else if ("w".equals(last)) {  // weeks
            time = now.minusWeeks(period);
        } else {
            time = now.minusMinutes(period); // minutes
        }
        return ZonedDateTime.of(time, ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
