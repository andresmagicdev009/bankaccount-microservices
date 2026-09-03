package com.application.service.application.shared;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.application.service.domain.shared.exception.InvalidPageSizeException;

/**
 * PASO 5.1 - Utilidad compartida de paginacion.
 *
 * Existe para que los tres servicios validen page/size igual, con los mismos
 * limites que declara el contrato (page >= 0, size entre 1 y 100, default 20).
 */
public final class PageRequestFactory {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageRequestFactory() {
    }

    /**
     * Un page negativo se corrige a 0 (no hay dano), pero un size fuera de rango
     * se rechaza con 400: silenciarlo devolveria una pagina distinta a la pedida.
     */
    public static Pageable of(Integer page, Integer size, Sort sort) {
        int pageNumber = (page == null || page < 0) ? 0 : page;

        if (size != null && (size < 1 || size > MAX_SIZE)) {
            throw new InvalidPageSizeException(size);
        }
        int pageSize = (size == null) ? DEFAULT_SIZE : size;

        return PageRequest.of(pageNumber, pageSize, sort == null ? Sort.unsorted() : sort);
    }
}
