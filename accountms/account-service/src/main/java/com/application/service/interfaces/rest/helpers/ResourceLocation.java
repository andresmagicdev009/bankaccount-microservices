package com.application.service.interfaces.rest.helpers;

import java.net.URI;

import org.springframework.web.server.ServerWebExchange;

/**
 * Location header of the 201 responses.
 *
 * It is built on the path of the request so it includes the base-path (/api/v1)
 * without repeating it in every controller.
 */
public final class ResourceLocation {

    private ResourceLocation() {
    }

    public static URI of(ServerWebExchange exchange, String id) {
        return URI.create(exchange.getRequest().getPath().value() + "/" + id);
    }
}
