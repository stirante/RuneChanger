package com.stirante.runechanger.utils;

import java.util.*;

public class ClassSet<T> extends AbstractSet<T> {

    private final Comparator<T> comparator;
    private final List<T> actualList = new ArrayList<>();

    public ClassSet(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    @Override
    public Iterator<T> iterator() {
        return actualList.iterator();
    }

    @Override
    public int size() {
        return actualList.size();
    }

    @Override
    public boolean add(T t) {
        if (contains(t)) {
            return false;
        }
        actualList.add(t);
        actualList.sort(comparator);
        return true;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        for (T t : actualList) {
            if (t.getClass() == o.getClass()) {
                return true;
            }
        }
        return false;
    }
}
