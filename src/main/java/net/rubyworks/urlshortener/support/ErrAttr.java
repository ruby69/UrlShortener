package net.rubyworks.urlshortener.support;

import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;

public class ErrAttr extends DefaultErrorAttributes {
    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        return Map.of("result", HttpStatus.BAD_REQUEST);
    }
}
