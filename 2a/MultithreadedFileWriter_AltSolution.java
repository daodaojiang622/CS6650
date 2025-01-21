import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.*;

public class MultithreadedFileWriter_AltSolution {

    public static void main(String[] args) throws InterruptedException, IOException {
        int numberOfThreads = 500;
        int stringsPerThread = 10000;
        String fileName = "output.txt";

        // Shared priority queue for ordered writes
        BlockingQueue<String> queue = new PriorityBlockingQueue<>();

        // Record start time for the entire approach
        long startTime = System.currentTimeMillis();

        // Consumer thread to write data to file
        Thread writerThread = new Thread(() -> {
            long consumerStartTime = System.currentTimeMillis();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                while (true) {
                    String line = queue.take(); // Blocking call
                    if (line.equals("POISON_PILL")) break; // End condition
                    writer.write(line + "\n");
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            long consumerEndTime = System.currentTimeMillis();
            System.out.println("Consumer Thread: Time taken = " + (consumerEndTime - consumerStartTime) + " ms");
        });

        writerThread.start(); // Start the consumer thread

        // Producer threads to generate strings
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        long producerStartTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < stringsPerThread; j++) {
                    long timestamp = System.currentTimeMillis();
                    long threadId = Thread.currentThread().getId();
                    try {
                        queue.put(timestamp + ", " + threadId + ", " + j); // Add to queue
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        long producerEndTime = System.currentTimeMillis();
        System.out.println("Producer Threads: Time taken = " + (producerEndTime - producerStartTime) + " ms");

        // Add poison pill to signal writer thread to terminate
        queue.put("POISON_PILL");
        writerThread.join();

        // Record end time for the entire approach
        long endTime = System.currentTimeMillis();
        System.out.println("Total Time (Producer + Consumer): Time taken = " + (endTime - startTime) + " ms");
    }
}
