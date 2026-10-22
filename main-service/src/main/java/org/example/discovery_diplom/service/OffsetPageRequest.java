package org.example.discovery_diplom.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Pageable that treats the API's from parameter as an exact row offset. */
final class OffsetPageRequest implements Pageable {
    private final long offset;
    private final int pageSize;
    private final Sort sort;

    OffsetPageRequest(long offset, int pageSize, Sort sort) {
        if (offset < 0 || pageSize < 1) {
            throw new IllegalArgumentException("Offset must be non-negative and page size must be positive");
        }
        this.offset = offset;
        this.pageSize = pageSize;
        this.sort = sort;
    }

    @Override
    public int getPageNumber() {
        return Math.toIntExact(offset / pageSize);
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(offset + pageSize, pageSize, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious()
                ? new OffsetPageRequest(Math.max(0, offset - pageSize), pageSize, sort)
                : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, pageSize, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Page number must not be negative");
        }
        return new OffsetPageRequest((long) pageNumber * pageSize, pageSize, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
