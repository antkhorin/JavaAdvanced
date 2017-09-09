package ru.ifmo.ctddev.khorin.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private List<T> array;
    private Comparator<? super T> comparator;
    private boolean reverse;

    public ArraySet() {
        this(null);
    }

    public ArraySet(Collection<? extends T> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> comparator) {
        this.comparator = comparator;
        if (collection != null) {
            TreeSet<T> set = new TreeSet<>(this.comparator);
            set.addAll(collection);
            array = Collections.unmodifiableList(new ArrayList<>(set));
        } else {
            array = Collections.emptyList();
        }
    }

    private ArraySet(List<T> list, Comparator<? super T> comparator, boolean reverse) {
        array = list;
        this.comparator = comparator;
        this.reverse = reverse;
    }

    /*public static void main(String[] args) {
        ArrayList<Integer> b = new ArrayList<>();
        b.add(1);
        b.add(3);
        b.add(5);
        b.add(7);
        b.add(9);
        b.add(11);
        b.add(111);
        System.out.println(Collections.binarySearch(b, 11));
        ArraySet<Integer> a = new ArraySet<>(b);
        System.out.println(a.contains(1));
    }*/

    @Override
    public T lower(T t) {
        int index = Collections.binarySearch(array, t, comparator);
        if (index == -1 || index == 0) {
            return null;
        } else if (index > 0){
            return array.get(index - 1);
        } else {
            return array.get(-index - 2);
        }
    }

    @Override
    public T floor(T t) {
        int index = Collections.binarySearch(array, t, comparator);
        if (index == -1) {
            return null;
        } else if (index >= 0){
            return array.get(index);
        } else {
            return array.get(-index - 2);
        }
    }

    @Override
    public T ceiling(T t) {
        int index = Collections.binarySearch(array, t, comparator);
        if (index == -size() - 1) {
            return null;
        } else if (index >= 0){
            return array.get(index);
        } else {
            return array.get(-index - 1);
        }
    }

    @Override
    public T higher(T t) {
        int index = Collections.binarySearch(array, t, comparator);
        if (index == -size() - 1 || index == size() - 1) {
            return null;
        } else if (index >= 0){
            return array.get(index + 1);
        } else {
            return array.get(-index - 1);
        }
    }

    @Override
    public int size() {
        return array.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(array, (T)o, comparator) >= 0;
    }

    private Iterator<T> getIterator(boolean order) {
        if (reverse ^ order) {
            return array.iterator();
        } else {
            return new Iterator<T>() {
                private ListIterator<T> iterator = array.listIterator(size() - 1);

                @Override
                public boolean hasNext() {
                    return iterator.hasPrevious();
                }

                @Override
                public T next() {
                    return iterator.previous();
                }
            };
        }
    }

    @Override
    public Iterator<T> iterator() {
        return getIterator(true);
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(array, Collections.reverseOrder(comparator), !reverse);
    }

    @Override
    public Iterator<T> descendingIterator() {
        return getIterator(false);
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int start = Collections.binarySearch(array, fromElement, comparator);
        int end = Collections.binarySearch(array, toElement, comparator);
        if (start >= 0 && start == end && !fromInclusive) {
            return new ArraySet<>(array.subList(start, end), comparator, reverse);
        }
        if (start >= 0 && !fromInclusive) {
            start++;
        }
        if (end >= 0 && toInclusive) {
            end++;
        }
        if (start < 0) {
            start = -start - 1;
        }
        if (end < 0) {
            end = -end - 1;
        }
        try {
            return new ArraySet<>(array.subList(start, end), comparator, reverse);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (isEmpty()) {
            return this;
        } else {
            return subSet(first(), true, toElement, inclusive);
        }
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (isEmpty()) {
            return this;
        } else {
            return subSet(fromElement, inclusive, last(), true);
        }
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return array.get(0);
        }
    }

    @Override
    public T last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return array.get(size() - 1);
        }
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }
}
