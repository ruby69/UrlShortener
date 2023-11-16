package net.rubyworks.urlshortener.web;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import net.rubyworks.urlshortener.domain.Shorten;

@RequiredArgsConstructor
@Async("fluxPool")
@Component
public class ShortenCompletableFuture {
    private final ShortenerService shortnerService;

    public CompletableFuture<URI> id(String id) {
        var shorten = shortnerService.findAndUpdate(id);
        return CompletableFuture.completedFuture(shorten.uri());
    }

    public CompletableFuture<Shorten> save(Shorten.Param param) {
        var shorten = shortnerService.create(param.url(), param.count(), param.ttl());
        return CompletableFuture.completedFuture(shorten);
    }

    public CompletableFuture<List<?>> byAll() {
        var list = shortnerService.findAll();
        return CompletableFuture.completedFuture(list);
    }

    public CompletableFuture<URI> delete(String id) {
        shortnerService.deleteBy(id);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<List<?>> bulk(List<Shorten.Param> params) {
        params.forEach(param -> shortnerService.create(param.id(), param.url(), param.count(), param.ttl()));
        return byAll();
    }

    private static final String REGEX_DURATION = "^[0-9]+[mhdw]$";
    private static final Pattern PATTERN_DURATION = Pattern.compile(REGEX_DURATION);
    public CompletableFuture<List<?>> byDuration(String duration) {
        if (!StringUtils.hasText(duration) || !PATTERN_DURATION.matcher(duration).matches()) {
            return byAll();
        }

        var list = shortnerService.findByDuration(duration);
        return CompletableFuture.completedFuture(list);
    }

}
