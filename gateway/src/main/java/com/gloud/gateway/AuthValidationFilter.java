package com.gloud.gateway;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthValidationFilter implements GlobalFilter, Ordered {

    private final WebClient.Builder webClientBuilder;

    public AuthValidationFilter(@Qualifier("loadBalancedWebClientBuilder") WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        if (path.matches(".*/auth.*")
                || path.matches(".*/swagger.*")
                || path.matches(".*/health.*")
                || path.matches(".*/oauth2.*")
        ) {
            return chain.filter(exchange); // 우회
        }

        return webClientBuilder.build()
                .get()
                .uri("http://service-auth/auth/validate")
                .cookies(cookies -> {
                    exchange.getRequest().getCookies().forEach((name, values) -> {
                        values.forEach(cookie -> cookies.add(name, cookie.getValue()));
                    });
                })
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.UNAUTHORIZED)) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    } else if (response.statusCode().isError()) {
                        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}