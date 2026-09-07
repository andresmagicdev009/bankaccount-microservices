package com.application.service.interfaces.rest.helpers;

import java.net.URI;

import org.springframework.web.server.ServerWebExchange;

/**
 * Cabecera Location de los 201.
 *
 * Se construye sobre la ruta de la peticion para que incluya el base-path
 * (/api/v1) sin repetirlo en cada controller.
 */
public final class ResourceLocation {

    private ResourceLocation() {
    }

    public static URI of(ServerWebExchange exchange, String id) {
        return URI.create(exchange.getRequest().getPath().value() + "/" + id);
    }
}
