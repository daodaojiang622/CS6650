import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

public class LoadTester {

    private static final int INIT_THREADS = 10;
    private static final int INIT_REQUESTS_PER_THREAD = 100;
    private static final int REQUESTS_PER_THREAD = 1000;
    private static final int MAX_RETRIES = 5;

    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)  // Enable HTTP/2 for multiplexing
            .connectTimeout(Duration.ofSeconds(5))
            .executor(Executors.newVirtualThreadPerTaskExecutor()) // Use Virtual Threads
            .build();

    private static String baseUrl;
    private static final byte[] imageBytes = loadImage(); // Cache image in memory

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: java LoadTester <threadGroupSize> <numThreadGroups> <delay> <IPAddr>");
            System.exit(1);
        }

        int threadGroupSize = Integer.parseInt(args[0]);
        int numThreadGroups = Integer.parseInt(args[1]);
        int delay = Integer.parseInt(args[2]);
        baseUrl = args[3];

        ExecutorService executor = Executors.newFixedThreadPool(200);

        System.out.println("Starting initialization phase...");
        runLoadTest(executor, INIT_THREADS, INIT_REQUESTS_PER_THREAD);

        Instant startTime = Instant.now();
        System.out.println("Initialization phase complete. Starting main test...");

        for (int i = 0; i < numThreadGroups; i++) {
            System.out.println("Starting thread group " + (i + 1) + "/" + numThreadGroups);
            runLoadTest(executor, threadGroupSize, REQUESTS_PER_THREAD);

            if (i < numThreadGroups - 1) {
                try {
                    Thread.sleep(delay * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Instant endTime = Instant.now();
        long wallTime = Duration.between(startTime, endTime).getSeconds();
        long totalRequests = (INIT_THREADS * INIT_REQUESTS_PER_THREAD) + (threadGroupSize * REQUESTS_PER_THREAD * numThreadGroups);
        double throughput = (double) totalRequests / wallTime;

        System.out.println("\nTest complete.");
        System.out.println("Wall Time: " + wallTime + " seconds");
        System.out.println("Throughput: " + throughput + " requests/sec");
    }

    //    private static void runLoadTest(ExecutorService executor, int threadCount, int requestsPerThread) {
//        List<CompletableFuture<Void>> futures = new ArrayList<>();
//
//        for (int i = 0; i < threadCount; i++) {
//            futures.add(CompletableFuture.runAsync(() -> {
//                for (int j = 0; j < requestsPerThread; j++) {
//                    sendPostRequest();
//                    sendGetRequest();
//                }
//            }, executor));
//        }
//
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//    }
    private static void runLoadTest(ExecutorService executor, int threadCount, int requestsPerThread) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                List<CompletableFuture<Void>> requestFutures = new ArrayList<>();
                for (int j = 0; j < requestsPerThread; j++) {
                    requestFutures.add(CompletableFuture.runAsync(LoadTester::sendPostRequest, executor));
                    requestFutures.add(CompletableFuture.runAsync(LoadTester::sendGetRequest, executor));
                }
                CompletableFuture.allOf(requestFutures.toArray(new CompletableFuture[0])).join();
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private static void sendPostRequest() {
        String postUrl = baseUrl + "/albums/12345";

        if (imageBytes == null) {
            System.err.println("Image data is unavailable. Skipping POST request.");
            return;
        }

        String boundary = "----JavaBoundary" + UUID.randomUUID();
        String CRLF = "\r\n";

        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append(CRLF)
                .append("Content-Disposition: form-data; name=\"profile[artist]\"").append(CRLF).append(CRLF)
                .append("Test Artist").append(CRLF)
                .append("--").append(boundary).append(CRLF)
                .append("Content-Disposition: form-data; name=\"profile[title]\"").append(CRLF).append(CRLF)
                .append("Test Album").append(CRLF)
                .append("--").append(boundary).append(CRLF)
                .append("Content-Disposition: form-data; name=\"profile[year]\"").append(CRLF).append(CRLF)
                .append("2023").append(CRLF)
                .append("--").append(boundary).append(CRLF)
                .append("Content-Disposition: form-data; name=\"image\"; filename=\"test.jpg\"").append(CRLF)
                .append("Content-Type: image/jpg").append(CRLF).append(CRLF);

        byte[] multipartHeader = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] multipartFooter = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] requestBody = new byte[multipartHeader.length + imageBytes.length + multipartFooter.length];

        System.arraycopy(multipartHeader, 0, requestBody, 0, multipartHeader.length);
        System.arraycopy(imageBytes, 0, requestBody, multipartHeader.length, imageBytes.length);
        System.arraycopy(multipartFooter, 0, requestBody, multipartHeader.length + imageBytes.length, multipartFooter.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(postUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

        sendWithRetries(request);
    }

    private static void sendGetRequest() {
        String getUrl = baseUrl + "/albums/12345";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl))
                .GET()
                .build();

        sendWithRetries(request);
    }

    private static void sendWithRetries(HttpRequest request) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {  // Use 'attempt' instead of modifying 'i'
            try {
                int retryCount = attempt; // Create a final variable for use in lambda

                CompletableFuture<HttpResponse<String>> responseFuture =
                        client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

                responseFuture.thenAccept(response -> {
//                    System.out.println("Response Body: " + response.body());
                    if (response.statusCode() >= 400) {
                        System.err.println("Error response (" + response.statusCode() + "), retrying... (" + (retryCount + 1) + "/" + MAX_RETRIES + ")");
                        System.err.println("Error message: " + response.body());
                    }
                }).join(); // Wait for async completion

                return; // Exit loop on success
            } catch (Exception e) {
                System.err.println("Request failed, retrying... (" + (attempt + 1) + "/" + MAX_RETRIES + ")");
                try {
                    Thread.sleep(200 * (attempt + 1));
                } catch (InterruptedException ignored) {}
            }
        }

        System.err.println("Request failed after " + MAX_RETRIES + " retries.");
    }

    private static byte[] loadImage() {
        try {
            return Files.readAllBytes(Paths.get("../../asset/test.jpg"));
        } catch (IOException e) {
            System.err.println("Failed to load image file: " + e.getMessage());
            return null; // Handle missing image case
        }
    }
}

