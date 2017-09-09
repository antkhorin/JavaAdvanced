package ru.ifmo.ctddev.khorin.crawler;

import info.kgeorgiy.java.advanced.crawler.*;
import info.kgeorgiy.java.advanced.implementor.InterfaceImplementorTest;
import javafx.util.Pair;
import javafx.util.converter.IntegerStringConverter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {

    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final int perHost;
    private final Set<String> visited;
    private final Map<String, Set<String>> links;
    private final Map<String, IOException> errors;
    private final Map<String, Integer> threadsOnHost;

    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
        visited = new ConcurrentSkipListSet<>();
        links = new ConcurrentHashMap<>();
        errors = new ConcurrentHashMap<>();
        threadsOnHost = new HashMap<>();
    }

    public Result download(final String url, final int depth) {
        final Phaser phaser = new Phaser(1);
        final Set<String> visited = new ConcurrentSkipListSet<>();
        final Set<String> result = new ConcurrentSkipListSet<>();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();
        final Map<String, Queue<Pair<String, Integer>>> tasksOnHost = new ConcurrentHashMap<>();
        visited.add(url);
        downloadSite(url, depth, visited, result, errors, tasksOnHost, phaser, false);
        phaser.arriveAndAwaitAdvance();
        return new Result(new ArrayList<>(result), errors);
    }

    private void downloadSite(final String url, final int depth, final Set<String> visited, final Set<String> result, final Map<String, IOException> errors, final Map<String, Queue<Pair<String, Integer>>> tasksOnHost, final Phaser phaser, final boolean c) {
        phaser.register();
        if (depth < 1) {
            phaser.arrive();
            return;
        }
        try {
            if (this.visited.contains(url)) {
                if (errors.containsKey(url)) {
                    errors.put(url, this.errors.get(url));
                } else {
                    result.add(url);
                    if (depth > 1) {
                        links.get(url).stream().filter(visited::add).forEach(e -> downloadSite(e, depth - 1, visited, result, errors, tasksOnHost, phaser, false));
                    }
                }
            } else {
                boolean b = true;
                String host = URLUtils.getHost(url);
                threadsOnHost.putIfAbsent(host, 0);
                if (!c) {
                    synchronized (threadsOnHost) {
                        if (threadsOnHost.get(host) < perHost) {
                            threadsOnHost.put(host, threadsOnHost.get(host) + 1);
                        } else {
                            b = false;
                        }
                    }
                }
                if (b) {
                    phaser.register();
                    downloaders.submit(() -> {
                        try {
                            Document doc = downloader.download(url);
                            result.add(url);
                            if (depth > 1) {
                                phaser.register();
                                extractors.submit(() -> {
                                    try {
                                        List<String> links = doc.extractLinks();
                                        this.links.put(url, new HashSet<>(links));
                                        links.stream().filter(visited::add).forEach(s -> downloadSite(s, depth - 1, visited, result, errors, tasksOnHost, phaser, false));
                                    } catch (IOException e) {
                                        this.errors.put(url, e);
                                        errors.put(url, e);
                                    } finally {
                                        this.visited.add(url);
                                        phaser.arrive();
                                    }
                                });
                            }
                        } catch (IOException e) {
                            this.errors.put(url, e);
                            errors.put(url, e);
                        } finally {
                            if (depth == 1) {
                                this.visited.add(url);
                            }
                            Pair<String, Integer> task = tasksOnHost.get(host).poll();
                            if (task != null) {
                                downloadSite(task.getKey(), task.getValue(), visited, result, errors, tasksOnHost, phaser, true);
                            }
                            synchronized (threadsOnHost) {
                                threadsOnHost.put(host, threadsOnHost.get(host) - 1);
                            }
                            phaser.arrive();
                        }
                    });
                } else {
                }
            }

        } catch (IOException e) {
            errors.put(url, e);
        } finally {
            phaser.arrive();
        }
    }

    public void close() {
        downloaders.shutdownNow();
        extractors.shutdownNow();
    }

    public static void main(String[] args) {
        WebCrawler crawler;
        Downloader downloader = null;
        try {
            downloader = new CachingDownloader();
        } catch (IOException e) {
            System.out.println("Error");
        }
        String url = null;
        int downloaders = 10;
        int extractors = 10;
        int perHost = 1;
        if (args.length  == 0 || args.length > 4) {
            System.out.println("Wrong args length");
        } else {
            try {
                if (args.length >= 1) {
                    url = args[0];
                }
                if (args.length >= 2) {
                    downloaders = Integer.parseInt(args[1]);
                }
                if (args.length >= 3) {
                    extractors = Integer.parseInt(args[2]);
                }
                if (args.length == 4) {
                    perHost = Integer.parseInt(args[3]);
                }
            } catch (NumberFormatException e) {
                System.out.println("Wrong args, use correct numbers");
            }
            crawler = new WebCrawler(downloader, downloaders, extractors, perHost);
            Result result = crawler.download(url, 2);
            result.getDownloaded().forEach(System.out::println);
            for (int i = 5; i > 0; i--) {
                try {
                    Thread.sleep(1000);
                    System.out.println(i + "...");
                } catch (InterruptedException ignored) {

                }
            }
            result = crawler.download(url, 2);
            result.getDownloaded().forEach(System.out::println);
            crawler.close();
        }
    }
}
