package com.hospital.common;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;

public final class PageQueryUtils {
    private PageQueryUtils() {}

    public static <T> PageResponse<T> toPage(List<T> source, int page, int size, String sortBy, String sortDir) {
        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 1);
        List<T> sorted = sort(source, sortBy, sortDir);
        int from = Math.min((safePage - 1) * safeSize, sorted.size());
        int to = Math.min(from + safeSize, sorted.size());
        return new PageResponse<>(sorted.subList(from, to), sorted.size(), safePage, safeSize);
    }

    private static <T> List<T> sort(List<T> source, String sortBy, String sortDir) {
        if (source == null || source.isEmpty() || sortBy == null || sortBy.isBlank()) return source == null ? List.of() : source;
        Comparator<T> comparator = Comparator.comparing(item -> comparableValue(item, sortBy), Comparator.nullsLast(Comparator.naturalOrder()));
        if ("desc".equalsIgnoreCase(sortDir)) comparator = comparator.reversed();
        return source.stream().sorted(comparator).toList();
    }

    @SuppressWarnings("unchecked")
    private static Comparable<Object> comparableValue(Object item, String sortBy) {
        try {
            Method method = item.getClass().getMethod(sortBy);
            Object value = method.invoke(item);
            return (value instanceof Comparable<?> c) ? (Comparable<Object>) c : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
