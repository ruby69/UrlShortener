package net.rubyworks.urlshortener.web;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.util.Map;

import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.extern.slf4j.Slf4j;

@Configuration
public class ShortenRouter {
    @Bean
    RouterFunction<?> router(ShortenHandler handler) {
        return route()
                .GET("/{id}", handler::id)
                .POST("/q", accept(MediaType.APPLICATION_JSON), handler::save)
                .POST("/bulk", accept(MediaType.APPLICATION_JSON), handler::bulk)

                .GET("/api/list/all", handler::byAll)
                .GET("/api/list/{duration}", handler::byDuration)
                .GET("/api/delete/{id}", handler::delete)

                .build();
    }

    @Component
    public static class ErrAttr extends DefaultErrorAttributes {
        @Override
        public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
            return Map.of("result", HttpStatus.BAD_REQUEST);
        }
    }

    @Slf4j
    @Component
    @Order(-2)
    public static class ErrHandler extends AbstractErrorWebExceptionHandler {
        public ErrHandler(ErrAttr attr, ApplicationContext applicationContext, ServerCodecConfigurer serverCodecConfigurer) {
            super(attr, new Resources(), applicationContext);
            super.setMessageReaders(serverCodecConfigurer.getReaders());
            super.setMessageWriters(serverCodecConfigurer.getWriters());
        }

        @Override
        protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
            return RouterFunctions.route(RequestPredicates.all(), request ->
                    ServerResponse
                        .status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(getErrorAttributes(request, ErrorAttributeOptions.defaults())))
            );
        }

        @Override
        protected void logError(ServerRequest request, ServerResponse response, Throwable throwable) {
            log.error(throwable.getMessage(), throwable);
        }
    }
}
