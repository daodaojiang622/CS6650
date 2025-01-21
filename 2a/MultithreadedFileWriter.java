import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MultithreadedFileWriter {

    public static void main(String[] args) throws InterruptedException, IOException {
        int numberOfThreads = 500;
        int stringsPerThread = 1000;
        String fileName = "output.txt";

        // Approach 1: Write directly to file in each thread
        long startTime1 = System.currentTimeMillis();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            for (int i = 0; i < numberOfThreads; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < stringsPerThread; j++) {
                            long timestamp = System.currentTimeMillis();
                            long threadId = Thread.currentThread().getId();
                            writer.write(timestamp + ", " + threadId + ", " + j + "\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }
        long endTime1 = System.currentTimeMillis();
        System.out.println("Approach 1 (Direct Write): Time taken = " + (endTime1 - startTime1) + " ms");

        // Approach 2: Write all strings from each thread before terminating
        long startTime2 = System.currentTimeMillis();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            for (int i = 0; i < numberOfThreads; i++) {
                executor.submit(() -> {
                    List<String> localBuffer = new ArrayList<>();
                    for (int j = 0; j < stringsPerThread; j++) {
                        long timestamp = System.currentTimeMillis();
                        long threadId = Thread.currentThread().getId();
                        localBuffer.add(timestamp + ", " + threadId + ", " + j);
                    }
                    synchronized (writer) {
                        try {
                            for (String line : localBuffer) {
                                writer.write(line + "\n");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }
        long endTime2 = System.currentTimeMillis();
        System.out.println("Approach 2 (Batch Write): Time taken = " + (endTime2 - startTime2) + " ms");

        // Approach 3: Collect all strings and write in main thread
        long startTime3 = System.currentTimeMillis();
        List<String> globalBuffer = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < stringsPerThread; j++) {
                    long timestamp = System.currentTimeMillis();
                    long threadId = Thread.currentThread().getId();
                    synchronized (globalBuffer) {
                        globalBuffer.add(timestamp + ", " + threadId + ", " + j);
                    }
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            for (String line : globalBuffer) {
                writer.write(line + "\n");
            }
        }
        long endTime3 = System.currentTimeMillis();
        System.out.println("Approach 3 (Main Thread Write): Time taken = " + (endTime3 - startTime3) + " ms");
    }
}
