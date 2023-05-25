package com.company;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WebScraper {
    public static final Logger logger = LogManager.getLogger(WebScraper.class);

    public static void main(String[] args) {
        LinkedBlockingQueue<ScrapeURL> taskQueue = new LinkedBlockingQueue<>();
        taskQueue.add(new ScrapeURL("https://www.touro.edu/"));
        List<Email> allEmailList = new CopyOnWriteArrayList<>();
        Set<String> addedEmails = new CopyOnWriteArraySet<>();
        Set<String> processedLinks = new CopyOnWriteArraySet<>();
        AtomicInteger databaseConnectionCounter = new AtomicInteger();//do baches of 500 so when reaches 20 shutsdown the program

        ExecutorService es = Executors.newFixedThreadPool(16);

        // Create and start multiple worker threads
        for (int i = 0; i < 16; i++) {
            es.execute(() -> {
                while (true) {
                    try {
                        ScrapeURL scrapeURL = taskQueue.take();  // Blocks if the queue is empty
                        String url = scrapeURL.getUrl();
                        logger.info("Processing URL: {}", url);

                        scrapeURL.scrapeLinksAndEmails();


                        // Process emails
                        Set<Email> scrapedEmails = scrapeURL.getEmailList();
                        for (Email email : scrapedEmails) {
                            logger.info("Scraped Email: {}", email.getEmailAddress());
                        }

                        //Add them to main list and get rid of already added emails
                        synchronized (allEmailList) {
                            for (Email email : scrapedEmails) {
                                if (!addedEmails.contains(email.getEmailAddress())) {
                                    allEmailList.add(email);
                                    addedEmails.add(email.getEmailAddress());
                                }
                            }
                        }

                        // Check if got 10,000 emails
                        if (databaseConnectionCounter.get() >= 20) {
                            logger.info("Reached 10,000 emails. Stopping all threads.");
                            es.shutdownNow(); // Stop all threads
                            break;
                        }

                        synchronized (allEmailList) {
                            if (allEmailList.size() >= 500) {
                                logger.info("Reached 500 emails. Sending batch to Database.");
                                SaveToDatabase std = new SaveToDatabase(allEmailList);
                                std.saveEmailBatch();
                                databaseConnectionCounter.getAndIncrement();
                                allEmailList.clear();
                            }
                        }


                        // Get the scraped URLs
                        List<String> scrapedURLs = scrapeURL.getUrlList();

                        // Add new tasks to the queue
                        for (String scrapedURL : scrapedURLs) {
                            if (!processedLinks.contains(scrapedURL)) {
                                taskQueue.add(new ScrapeURL(scrapedURL));
                                processedLinks.add(scrapedURL);
                            }
                        }

                    } catch (InterruptedException e) {
                        // Handle the exception or break the loop
                        break;
                    }
                }
            });
        }


        // Shutdown the executor service once all tasks are completed
        es.shutdown();

//        try {
//            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//        } catch (InterruptedException e) {
//            // Handle the exception
//        }
//              TESTING
//        // Print and log the scraped emails
//        logger.info("Scraped emails:");
//        for (Email e : allEmailList) {
//            System.out.println(e.getEmailAddress());
//            logger.info("Email: {}", e.getEmailAddress());
//        }
//        //Save To database test
//        SaveToDatabase std = new SaveToDatabase(allEmailList);
//        std.saveEmailBatch();
    }
    }





//public class WebScraper {
//    public static final Logger logger = LogManager.getLogger(WebScraper.class);
//
//    private static List<Email> allEmailList = new ArrayList<>();
//
//    public static void main(String[] args) {
//        int numThreads = 16;
//        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
//        LinkedBlockingQueue<String> urlQueue = new LinkedBlockingQueue<>();
//        urlQueue.add("https://www.touro.edu/");
//        // ...
//
//        int maxEmails = 100; // Maximum number of emails to scrape
//
//        while (!urlQueue.isEmpty() && allEmailList.size() < maxEmails) {
//            String url = urlQueue.poll();
//            logger.info("Processing URL: {}", url);
//            ScrapeURL scrapeURL = new ScrapeURL(url);
//            Future<ScrapeURL> future = executorService.submit(scrapeURL, scrapeURL);
//
//            try {
//                ScrapeURL processedURL = future.get();
//                Set<Email> emailList = processedURL.getEmailList();
//                List<String> scrapedUrlList = processedURL.getUrlList();
//
//                // Combine the emailList from the current task with allEmailList
//                synchronized (allEmailList) {
//                    allEmailList.addAll(emailList);
//                }
//
//                // Add the scraped URLs to the urlQueue for further scraping
//                urlQueue.addAll(scrapedUrlList);
//
//                // Check if the maximum number of emails has been reached
//                if (allEmailList.size() >= maxEmails) {
//                    break; // Stop processing further URLs
//                }
//            } catch (InterruptedException | ExecutionException e) {
//                logger.error("An error occurred while processing a task.", e);
//            }
//        }
//
//        // Shutdown the executorService when all tasks are completed
//        executorService.shutdown();
//
//        // Process allEmailList as needed
//        // ...
//
//        logger.info("Scraped emails:");
//        for (Email e : allEmailList) {
//            System.out.println(e.getEmailAddress());
//            logger.info("Email: {}", e.getEmailAddress());
//        }
//    }
//}



