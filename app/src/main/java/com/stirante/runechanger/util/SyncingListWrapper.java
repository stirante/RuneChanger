package com.stirante.runechanger.util;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class SyncingListWrapper<E> {

    private final ObservableList<E> backingList;

    public SyncingListWrapper() {
        backingList = FXCollections.observableArrayList();
    }

    public void addListener(ListChangeListener<E> listener) {
        backingList.addListener(listener);
    }

    public ObservableList<E> getBackingList() {
        return backingList;
    }

    public int size() {
        return backingList.size();
    }

    public void sort(Comparator<E> comparator) {
        FxUtils.doOnFxThread(() -> FXCollections.sort(backingList, comparator));
    }

    public void clear() {
        FxUtils.doOnFxThread(backingList::clear);
    }

    public void add(E e) {
        FxUtils.doOnFxThread(() -> backingList.add(e));
    }

    public boolean contains(Object object) {
        return backingList.contains(object);
    }

    public void addAll(Collection<E> elements) {
        FxUtils.doOnFxThread(() -> backingList.addAll(elements));
    }
}
