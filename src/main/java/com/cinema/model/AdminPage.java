package com.cinema.model;

import java.util.List;

/**
 * One page of an in-memory list (used by M11).
 */
public record AdminPage<T>(List<T> items, int totalItems, int page, int size) {

    public static <T> AdminPage<T> of(List<T> all, int requestedPage, int size) {
        int total = all.size();
        int pages = Math.max(1, (int) Math.ceil(total / (double) size));
        int page = Math.min(Math.max(1, requestedPage), pages);
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        return new AdminPage<>(List.copyOf(all.subList(from, to)), total, page, size);
    }

    public int totalPages() { return Math.max(1, (int) Math.ceil(totalItems / (double) size)); }
    public int fromItem() { return totalItems == 0 ? 0 : (page - 1) * size + 1; }
    public int toItem() { return Math.min(page * size, totalItems); }
}
