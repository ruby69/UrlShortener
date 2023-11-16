package net.rubyworks.urlshortener;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;

import net.rubyworks.urlshortener.support.ErrAttr;
import net.rubyworks.urlshortener.support.ErrHandler;
import net.rubyworks.urlshortener.support.WebfluxTraceFilter;
import net.rubyworks.urlshortener.web.ShortenHandler;

@Configuration
public class WebConfig {

    @Bean
    WebfluxTraceFilter traceFilter() {
        return new WebfluxTraceFilter();
    }

    @Bean
    ErrAttr errAttr() {
        return new ErrAttr();
    }

    @Bean
    @Order(-2)
    ErrHandler errHandler(ErrAttr attr, ApplicationContext applicationContext, ServerCodecConfigurer serverCodecConfigurer) {
        return new ErrHandler(attr, applicationContext, serverCodecConfigurer);
    }

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
}
