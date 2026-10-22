package com.cinema.model;

import java.util.ArrayList;
import java.util.List;

/** Simple page of results for server-side pagination. {@code page} is 1-based. */
public class PageResult<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final int totalItems;

    public PageResult(List<T> items, int page, int size, int totalItems) {
        this.items = items == null ? List.of() : items;
        this.page = Math.max(1, page);
        this.size = Math.max(1, size);
        this.totalItems = Math.max(0, totalItems);
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public int getTotalItems() { return totalItems; }

    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil(totalItems / (double) size));
    }

    public int getFrom() { return totalItems == 0 ? 0 : (page - 1) * size + 1; }

    public int getTo() { return Math.min(totalItems, page * size); }

    public boolean isEmpty() { return items.isEmpty(); }

    public boolean hasPrevious() { return page > 1; }

    public boolean hasNext() { return page < getTotalPages(); }

    /** Page numbers to show, with 0 meaning "...". */
    public List<Integer> getPageNumbers() {
        int total = getTotalPages();
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= total; i++) {
            if (i == 1 || i == total || Math.abs(i - page) <= 1) {
                numbers.add(i);
            } else if (!numbers.isEmpty() && numbers.get(numbers.size() - 1) != 0) {
                numbers.add(0);
            }
        }
        return numbers;
    }
}
