package ru.ifmo.ctddev.khorin.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class for perform parallel action on list
 */
public class ParallelMapperImpl implements ParallelMapper {

    private final List<Thread> threads;
    private final QueueOfTasks queueOfTasks = new QueueOfTasks();
    private Boolean close = false;


    /**
     * Constructor of ParallelMapper which make threads and run them
     *
     * @param countOfThreads number how much threads we need to make
     */
    public ParallelMapperImpl(int countOfThreads)
    {
        threads = new ArrayList<>(countOfThreads);
        for (int i = 0; i < countOfThreads; i++) {
            threads.add(new Thread(new FunctionApplier(queueOfTasks)));
            threads.get(i).start();
        }
    }

    /**
     * Apply given function to given list of args and return list with result of applying function
     *
     * @param f function fo apply
     * @param args list with args
     * @param <T> type of element in list
     * @param <R> type of result which function returns
     * @return list with result of applying function
     * @throws InterruptedException when someone interrupt out threads
     */
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        if (close) {
            throw new IllegalStateException();
        }
        final List<R> result = new ArrayList<>(args.size());
        final List<Task<T,? extends R>> tasks = args.stream().map(element -> new Task<>(element, f)).collect(Collectors.toList());
        tasks.forEach(queueOfTasks::add);
        for (Task<T,? extends R> task : tasks) {
            result.add(task.getResult());
        }
        return result;
    }

    /**
     * Close all threads which maked in constructor
     *
     * @throws InterruptedException when someone interrupt our threads
     */
    public void close() throws InterruptedException {
        close = true;
        threads.forEach(Thread::interrupt);
        for (Thread thread : threads) {
            thread.join();
        }
        queueOfTasks.clear();
        threads.clear();
    }

    private class Task<T, R> {
        private final T element;
        private final Function<? super T, ? extends R> function;
        private R result;
        private boolean readyResult;

        /**
         * Ordinary constructor for Task
         *
         * @param element element for applying function
         * @param function function for applying
         */
        public Task(final T element, final Function<? super T, ? extends R> function) {
            this.element = element;
            this.function = function;
        }

        /**
         * Apply given function
         */
        public synchronized void run() {
            result = function.apply(element);
            readyResult = true;
            notify();
        }

        /**
         * Wait result of applying function and return result
         *
         * @return result of applying function
         * @throws InterruptedException when someone interrupt our threads
         */
        public synchronized R getResult() throws InterruptedException {
            while (!readyResult) {
                wait();
            }
            return result;
        }
    }

    private class QueueOfTasks {
        final Queue<Task<?, ?>> queue = new LinkedList<>();

        /**
         * Add task in queue
         *
         * @param task task for add in queue
         */
        public synchronized void add(final Task<?, ?> task) {
            queue.add(task);
            notify();
        }

        /**
         * Return task from queue
         *
         * @return task
         * @throws InterruptedException when someone interrupt our threads
         */
        public synchronized Task<?, ?> get() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }
            return queue.poll();
        }

        public synchronized void clear() {
            queue.clear();
        }
    }

    private class FunctionApplier implements Runnable {
        private final QueueOfTasks queueOfTasks;

        /**
         * Ordinary constructor of functionApplyer
         * @param queueOfTasks queue
         */
        public FunctionApplier(final QueueOfTasks queueOfTasks) {
            this.queueOfTasks = queueOfTasks;
        }

        /**
         * Get task from queue and run it until interrupted
         */
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    queueOfTasks.get().run();
                }
            } catch (InterruptedException ignored) {

            } finally {
                Thread.currentThread().interrupt();
            }
        }
    }
}
