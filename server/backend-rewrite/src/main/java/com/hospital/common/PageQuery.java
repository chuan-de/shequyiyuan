package com.hospital.common;

public record PageQuery(
    int page,
    int size,
    String sortBy,
    String sortDir
) {
    public static PageQuery of(Integer page, Integer size, String sortBy, String sortDir) {
        return new PageQuery(page == null || page < 1 ? 1 : page, size == null || size < 1 ? 10 : Math.min(size, 100), sortBy == null ? "id" : sortBy, sortDir == null ? "asc" : sortDir);
    }
}
