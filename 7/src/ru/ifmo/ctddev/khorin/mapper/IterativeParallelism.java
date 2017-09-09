package ru.ifmo.ctddev.khorin.mapper;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Class for perform some parallel operations on list
 *
 * @author Khorin Anton
 */
public class IterativeParallelism implements ListIP {

    private ParallelMapper mapper;

    public IterativeParallelism() {}

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    private class FunctionApplier<T, U> implements Runnable {
        List<? extends T> list;
        Function<? super List<? extends T>,? extends U> function;
        U result;

        /**
         * Ordinary constructor for FunctionApplier
         *
         * @param function function which is applying to list
         * @param list sublist of list with data for execution in this one thread
         */
        FunctionApplier(Function<List<? extends T>, U> function, List<? extends T> list) {
            this.function = function;
            this.list = list;
        }

        /**
         * Run this
         */
        public void run() {
            result = function.apply(list);
        }

        /**
         * Get result of execution this runnable
         * @return result of execution this runnable
         */
        U getResult() {
            return result;
        }
    }

    private <T, U> List<U> makeThreads(int i, List<? extends T> list, Function<List<? extends T>, U> function) throws InterruptedException {
        i = Math.min(i, list.size());
        i = Math.min(i, 8);
        double step = ((double) list.size()) / i;
        List<List<? extends T>> parts = new ArrayList<>(i);
        for (int j = 0; j < i; j++) {
            parts.add(list.subList((int)(step * j), j == i - 1 ? list.size() : (int)(step * (j + 1))));
        }
        if (mapper != null) {
            return mapper.map(function, parts);
        } else {
            List<FunctionApplier<T, U>> appliers = parts.stream().map(element -> new FunctionApplier<>(function, element)).collect(Collectors.toList());
            List<Thread> threads = appliers.stream().map(Thread::new).collect(Collectors.toList());
            threads.forEach(Thread::start);
            for (Thread thread : threads) {
                thread.join();
            }
            return appliers.stream().map(FunctionApplier::getResult).collect(Collectors.toList());
        }
    }

    /**
     * Find maximum in given list, using given comparator,
     * using not more than i threads
     *
     * @param i amount of threads which we can use to find maximum
     * @param list list with data for find maximum
     * @param comparator comparator for compare elements from list
     * @param <T> type of elements in list
     * @return maximum in list
     * @throws InterruptedException if someone interrupt our threads
     * @throws NoSuchElementException if given list is empty
     * @throws NullPointerException if given comparator or list is null
     */
    public <T> T maximum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException, NoSuchElementException, NullPointerException {
        return makeThreads(i, list, element -> element.stream().max(comparator).get()).stream().max(comparator).get();
    }

    /**
     * Find minimum in given list, using given comparator,
     * using not more than i threads
     *
     * @param i amount of threads which we can use to find minimum
     * @param list list with data for find minimum
     * @param comparator comparator for compare elements from list
     * @param <T> type of elements in list
     * @return minimum in list
     * @throws InterruptedException if someone interrupt our threads
     * @throws NoSuchElementException if given list is empty
     * @throws NullPointerException if given comparator or list is null
     */
    public <T> T minimum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException, NoSuchElementException, NullPointerException {
        return maximum(i, list, comparator.reversed());
    }

    /**
     * Check that all elements in given list satisfy given predicate
     *
     * @param i amount of threads which we can use
     * @param list list with data
     * @param predicate predicate for check
     * @param <T> type of elements in list
     * @return true if all elements in given list satisfy given predicate, else false
     * @throws InterruptedException if someone interrupt our threads
     * @throws NullPointerException if given predicate or list is null
     */
    public <T> boolean all(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException, NullPointerException {
        return makeThreads(i, list, element -> element.stream().allMatch(predicate)).stream().allMatch(Predicate.isEqual(true));
    }

    /**
     * Check that any elements in given list satisfy given predicate
     *
     * @param i amount of threads which we can use
     * @param list list with data
     * @param predicate predicate for check
     * @param <T> type of elements in list
     * @return true if any elements in given list satisfy given predicate, else false
     * @throws InterruptedException if someone interrupt our threads
     * @throws NullPointerException if given predicate or list is null
     */
    public <T> boolean any(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException, NullPointerException {
        return !all(i, list, predicate.negate());
    }

    /**
     * Make one string with all elements from list in string representation
     *
     * @param i amount of threads which we can use
     * @param list list with data
     * @return string with all elements from list in string representation
     * @throws InterruptedException if someone interrupt our threads
     * @throws NullPointerException if given list is null
     */
    public String join(int i, List<?> list) throws InterruptedException, NullPointerException {
        StringBuilder result = new StringBuilder();
        makeThreads(i, list, element -> {
            StringBuilder s = new StringBuilder();
            element.stream().map(Object::toString).forEach(s::append);
            return s.toString();
        }).forEach(result::append);
        return result.toString();
    }

    /**
     *  Make new list with elements from given list which satisfies given predicate
     *
     * @param i amount of threads which we can use
     * @param list list with data
     * @param predicate predicate for check
     * @param <T> type of elements in list
     * @return list with elements from given list which satisfies given predicate
     * @throws InterruptedException if someone interrupt our threads
     * @throws NullPointerException if given predicate or list is null
     */
    public <T> List<T> filter(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException, NullPointerException {
        List<T> result = new LinkedList<>();
        makeThreads(i, list, element -> element.stream().filter(predicate).collect(Collectors.toList())).forEach(result::addAll);
        return result;
    }
    /**
     *  Make new list with elements from given list which applied given function
     *
     * @param i amount of threads which we can use
     * @param list list with data
     * @param function function for apply
     * @param <T> type of elements in list
     * @param <U> type of elements that function returns
     * @return list with elements from given list which applied given function
     * @throws InterruptedException if someone interrupt our threads
     * @throws NullPointerException if given predicate or list is null
     */
    public <T, U> List<U> map(int i, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException, NullPointerException {
        List<U> result = new LinkedList<>();
        makeThreads(i, list, element -> element.stream().map(function::apply).collect(Collectors.toList())).forEach(result::addAll);
        return result;
    }
}
