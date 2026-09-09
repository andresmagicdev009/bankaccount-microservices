package com.application.service.application.shared;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.application.service.domain.shared.exception.InvalidPageSizeException;

/**
 * Shared pagination utility.
 *
 * It exists so the three services validate page/size the same way, with the
 * limits declared by the contract (page >= 0, size between 1 and 100, default
 * 20).
 */
public final class PageRequestFactory {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageRequestFactory() {
    }

    /**
     * A negative page is corrected to 0 (no harm done), but a size out of range
     * is rejected with a 400: swallowing it would return a page other than the
     * one requested.
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
