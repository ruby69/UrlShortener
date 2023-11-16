package net.rubyworks.urlshortener.query;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Async("fluxPool")
@Component
public class QueryCompletableFuture {
    private final QueryShortener shortner;

    public CompletableFuture<List<?>> byAll() {
        var list = shortner.findAll();
        return CompletableFuture.completedFuture(list);
    }

    private static final String REGEX_DURATION = "^[0-9]+[mhdw]$";
    private static final Pattern PATTERN_DURATION = Pattern.compile(REGEX_DURATION);
    public CompletableFuture<List<?>> byDuration(String duration) {
        if (!StringUtils.hasText(duration) || !PATTERN_DURATION.matcher(duration).matches()) {
            return byAll();
        }

        var list = shortner.findByDuration(duration);
        return CompletableFuture.completedFuture(list);
    }
}
