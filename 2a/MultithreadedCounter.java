import java.util.ArrayList;
import java.util.List;

public class MultithreadedCounter {
    // Shared synchronized counter
    private static int counter = 0;

    // Synchronized method to increment the counter
    private synchronized static void increment() {
        counter++;
    }

    public static void main(String[] args) {
        int threadCounts[] = {1, 100, 1000, 10000}; // Different thread counts to test

        for (int threadCount : threadCounts) {
            System.out.println("Running with " + threadCount + " threads...");

            // Reset counter for each run
            counter = 0;

            // Record start time
            long startTime = System.currentTimeMillis();

            // List to hold threads
            List<Thread> threads = new ArrayList<>();

            // Create and start threads
            for (int i = 0; i < threadCount; i++) {
                Thread thread = new Thread(() -> {
                    for (int j = 0; j < 10; j++) {
                        increment();
                    }
                });
                threads.add(thread);
                thread.start();
            }

            // Wait for all threads to finish
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Record end time
            long endTime = System.currentTimeMillis();

            // Print results
            System.out.println("Final counter value: " + counter);
            System.out.println("Time taken: " + (endTime - startTime) + " ms\n");
        }
    }
}
