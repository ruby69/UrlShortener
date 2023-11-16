package net.rubyworks.urlshortener.support;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.MDC;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class WebfluxTraceFilter implements WebFilter {
    private static final String MDC_KEY_REQUEST_ID = "requestId";
    private static final String MDC_KEY_REQ_PATH = "reqpath";
    private static final String MDC_KEY_METHOD = "method";
    private static final String MDC_KEY_STATUS = "status";
    private static final String FORMAT_REQ = """
            >> {}{}, {}, {},
            headers={}
            """;
    private static final String FORMAT_RES1 = """
            << {}{}, {}, {}, {},
            headers={}
            """;
    private static final String FORMAT_RES2 = """
            << {}{}, {}, {}, {},
            headers={},
            payload={}
            """;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var traceExchange = new TraceExchange(exchange);
        return chain.filter(traceExchange)
                .doFirst(() -> {
                    var request = exchange.getRequest();

                    MDC.putCloseable(MDC_KEY_REQUEST_ID, request.getId());
                    MDC.putCloseable(MDC_KEY_REQ_PATH, request.getPath().value());
                    MDC.putCloseable(MDC_KEY_METHOD, request.getMethod().name());
                    log.info(FORMAT_REQ,
                            exchange.getLogPrefix(),
                            request.getMethod(),
                            pathQuery(request),
                            clientIp(request),
                            request.getHeaders());
                })
                .doFinally(it -> {
                    var request = exchange.getRequest();
                    var payload = traceExchange.getRequest().getPayload();
                    var response = exchange.getResponse();

                    MDC.putCloseable(MDC_KEY_STATUS, String.valueOf(response.getStatusCode().value()));
                    log.info(StringUtils.hasText(payload) ? FORMAT_RES2 : FORMAT_RES1,
                            exchange.getLogPrefix(),
                            request.getMethod(),
                            pathQuery(request),
                            response.getStatusCode().value(),
                            traceExchange.traceTime(),
                            response.getHeaders(),
                            payload);
                    MDC.clear();
                });
    }

    private String pathQuery(ServerHttpRequest request) {
        var path = request.getPath();
        var query = request.getURI().getRawQuery();
        return StringUtils.hasText(query) ? path + "?" + query : path.toString();
    }

    private final String clientIp(ServerHttpRequest request) {
        return Optional.ofNullable(request)
                .map(ServerHttpRequest::getRemoteAddress)
                .map(InetSocketAddress::getAddress)
                .map(InetAddress::getHostAddress)
                .orElse("");
    }
}

class TraceExchange extends ServerWebExchangeDecorator {
    @Getter private PayloadRequest request;
    private long creationTime;

    TraceExchange(final ServerWebExchange exchange) {
        super(exchange);
        this.request = new PayloadRequest(exchange.getRequest());
        creationTime = System.currentTimeMillis();
    }

    long traceTime() {
        return System.currentTimeMillis() - creationTime;
    }
}

class PayloadRequest extends ServerHttpRequestDecorator {
    private final StringBuilder payload = new StringBuilder();

    public PayloadRequest(final ServerHttpRequest delegate) {
        super(delegate);
    }

    @Override
    public Flux<DataBuffer> getBody() {
        return super.getBody().doOnNext(buffer -> {
            var byteBuffer = buffer.toByteBuffer();
            if (byteBuffer != null && byteBuffer.remaining() > 0) {
                payload.append(StandardCharsets.UTF_8.decode(byteBuffer.asReadOnlyBuffer()).toString());
            }
        });
    }

    public String getPayload() {
        if (payload.length() == 0) {
            return "";
        }

        return payload.toString();
    }
}
