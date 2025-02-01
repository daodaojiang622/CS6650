import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;

public class MultiThreadedHttpClient {

    private static final String SERVER_URL = "http://34.211.226.45:8080/3b_war/hello/";
    private static final int NUM_THREADS = 1000; // Number of concurrent requests

    // Counter to track the number of completed requests
    private static class RequestCounter {
        private int count = 0;

        // Synchronized method to increment the counter (thread-safe)
        public synchronized void inc() {
            count++;
        }

        public int getVal() {
            return count;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final RequestCounter counter = new RequestCounter();
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        long startTime = System.currentTimeMillis(); // Start time

        // Create and start threads to send requests
        for (int i = 0; i < NUM_THREADS; i++) {
            Thread thread = new Thread(() -> {
                sendRequest(); // Send the request
                counter.inc(); // Increment counter when request completes
                latch.countDown(); // Signal that this thread has finished
            });
            thread.start();
        }

        latch.await(); // Wait for all threads to finish

        long endTime = System.currentTimeMillis(); // End time
        System.out.println("\nTotal requests sent: " + counter.getVal());
        System.out.println("Total time taken: " + (endTime - startTime) + " ms\n");

        // Print the expected value
        System.out.println("Expected Value: " + NUM_THREADS + "\nActual Value: " + counter.getVal());
    }

    private static void sendRequest() {
        // Send a GET request to the server
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(SERVER_URL);
            // Execute the request and get the response
            try (CloseableHttpResponse response = client.execute(request)) {
                System.out.println("Thread: " + Thread.currentThread().getId() + " - Response Code: " + response.getCode());

                // Read and print the response content
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Thread: " + Thread.currentThread().getId() + " - Response: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}