package net.rubyworks.urlshortener.command;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import net.rubyworks.urlshortener.command.domain.CShorten;

@RequiredArgsConstructor
@Async("fluxPool")
@Component
public class CommandCompletableFuture {
    private final CommandShortener shortner;

    public CompletableFuture<URI> id(String id) {
        var shorten = shortner.findAndUpdate(id);
        return CompletableFuture.completedFuture(shorten != null ? shorten.uri() : null);
    }

    public CompletableFuture<CShorten> save(CShorten.Param param) {
        var shorten = shortner.create(param.url(), param.count(), param.ttl());
        return CompletableFuture.completedFuture(shorten);
    }

    public CompletableFuture<URI> delete(String id) {
        shortner.deleteBy(id);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<List<?>> bulk(List<CShorten.Param> params) {
        params.forEach(param -> shortner.create(param.id(), param.url(), param.count(), param.ttl()));
        var list = shortner.findAll();
        return CompletableFuture.completedFuture(list);
    }

}
