package com.shopease.model;

import java.util.List;

/** One page of results plus paging metadata (page numbers are 1-based). */
public class Page<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;

    public Page(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
    }

    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }

    public int getTotalPages() {
        return size == 0 ? 1 : (int) Math.max(1, (totalElements + size - 1) / size);
    }

    public boolean isHasPrevious() { return page > 1; }
    public boolean isHasNext() { return page < getTotalPages(); }
    public long getFirstItem() { return totalElements == 0 ? 0 : (long) (page - 1) * size + 1; }
    public long getLastItem() { return Math.min((long) page * size, totalElements); }
}
