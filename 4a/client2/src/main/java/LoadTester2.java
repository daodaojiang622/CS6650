import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class LoadTester2 {

    private static final int INIT_THREADS = 10;
    private static final int INIT_REQUESTS_PER_THREAD = 100;
    private static final int REQUESTS_PER_THREAD = 1000;
    private static final int MAX_RETRIES = 5;

    private static final HttpClient client = HttpClient.newHttpClient();
    private static String baseUrl;

    private static final List<RequestRecord> requestRecords = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: java LoadTester <threadGroupSize> <numThreadGroups> <delay> <IPAddr>");
            System.exit(1);
        }

        int threadGroupSize = Integer.parseInt(args[0]);
        int numThreadGroups = Integer.parseInt(args[1]);
        int delay = Integer.parseInt(args[2]);
        baseUrl = args[3];

        ExecutorService executor = Executors.newFixedThreadPool(100); // Create a large thread pool

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
                    e.printStackTrace();
                }
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Instant endTime = Instant.now();
        long wallTime = endTime.getEpochSecond() - startTime.getEpochSecond();
        long totalRequests = (INIT_THREADS * INIT_REQUESTS_PER_THREAD) + (threadGroupSize * REQUESTS_PER_THREAD * numThreadGroups);
        double throughput = (double) totalRequests / wallTime;

        System.out.println("\nLoadTest2 complete.");
        System.out.println("Test specifics: " + threadGroupSize + " " + numThreadGroups + " " + delay + " " + baseUrl);
        System.out.println("Wall Time: " + wallTime + " seconds");
        System.out.println("Throughput: " + throughput + " requests/sec");

        writeResultsToCSV();
        calculateStatistics();
    }

    private static void runLoadTest(ExecutorService executor, int threadCount, int requestsPerThread) {
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    sendPostRequest();
                    sendGetRequest();
                }
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS); // Wait for completion
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendPostRequest() {
        String postUrl = baseUrl + "/albums/12345";

        // Define file path for an image (update this to an actual existing image path)
        Path imagePath = Paths.get("../../asset/test.jpeg"); // Ensure this file exists
        byte[] imageBytes;
        try {
            imageBytes = Files.readAllBytes(imagePath);
//            System.out.println("Image file read successfully. Image size: " + imageBytes.length);
        } catch (IOException e) {
            System.err.println("Failed to read image file: " + e.getMessage());
            return;
        }

        // Generate a unique boundary string for multipart format
        String boundary = "----JavaBoundary" + UUID.randomUUID();
        String CRLF = "\r\n";

        // Multipart form body
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"profile[artist]\"").append(CRLF).append(CRLF);
        sb.append("Test Artist").append(CRLF);

        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"profile[title]\"").append(CRLF).append(CRLF);
        sb.append("Test Album").append(CRLF);

        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"profile[year]\"").append(CRLF).append(CRLF);
        sb.append("2023").append(CRLF);

        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"image\"; filename=\"test.jpeg\"").append(CRLF);
        sb.append("Content-Type: image/jpeg").append(CRLF).append(CRLF);

        byte[] multipartHeader = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] multipartFooter = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        // Combine multipart body
        byte[] requestBody = new byte[multipartHeader.length + imageBytes.length + multipartFooter.length];
        System.arraycopy(multipartHeader, 0, requestBody, 0, multipartHeader.length);
        System.arraycopy(imageBytes, 0, requestBody, multipartHeader.length, imageBytes.length);
        System.arraycopy(multipartFooter, 0, requestBody, multipartHeader.length + imageBytes.length, multipartFooter.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(postUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

        sendWithRetries(request, "POST");
    }

    private static void sendGetRequest() {
        String getUrl = baseUrl + "/albums/12345";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl))
                .GET()
                .build();

        sendWithRetries(request, "GET");
    }

    private static void sendWithRetries(HttpRequest request, String requestType) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            Instant startTime = Instant.now();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                // take a timestamp after receiving the 200/201response
                Instant endTime = Instant.now();
                // calculate the latency
                long latency = Duration.between(startTime, endTime).toMillis();

                // add the request record to the list
                requestRecords.add(new RequestRecord(startTime, requestType, latency, response.statusCode()));

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return;
                } else if (response.statusCode() >= 400) {
                    System.err.println("Error response (" + response.statusCode() + "), retrying... (" + (i + 1) + "/" + MAX_RETRIES + ")");
                    System.err.println("Error message: " + response.body());
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                System.err.println("Request failed, retrying... (" + (i + 1) + "/" + MAX_RETRIES + ")");
                System.err.println("Error message: " + e.getMessage());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {}
            }
        }

        System.err.println("Request failed after " + MAX_RETRIES + " retries.");
    }

    private static void writeResultsToCSV() {
        try (FileWriter writer = new FileWriter("request_records.csv")) {
            writer.append("Start Time,Request Type,Latency,Response Code\n");
            for (RequestRecord record : requestRecords) {
                writer.append(record.toString()).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void calculateStatistics() {
        List<Long> postLatencies = new ArrayList<>();
        List<Long> getLatencies = new ArrayList<>();

        for (RequestRecord record : requestRecords) {
            if (record.getRequestType().equals("POST")) {
                postLatencies.add(record.getLatency());
            } else if (record.getRequestType().equals("GET")) {
                getLatencies.add(record.getLatency());
            }
        }

        System.out.println("POST Request Statistics:");
        calculateAndPrintStatistics(postLatencies);

        System.out.println("GET Request Statistics:");
        calculateAndPrintStatistics(getLatencies);
    }

    private static void calculateAndPrintStatistics(List<Long> latencies) {
        if (latencies.isEmpty()) {
            System.out.println("No data available.");
            return;
        }

        Collections.sort(latencies);

        long sum = 0;
        for (long latency : latencies) {
            sum += latency;
        }

        double mean = (double) sum / latencies.size();
        long median = latencies.get(latencies.size() / 2);
        long p99 = latencies.get((int) (latencies.size() * 0.99));

        System.out.println("Mean Response Time: " + mean + " ms");
        System.out.println("Median Response Time: " + median + " ms");
        System.out.println("99th Percentile Response Time: " + p99 + " ms");
    }

    private static class RequestRecord {
        private final Instant startTime;
        private final String requestType;
        private final long latency;
        private final int responseCode;

        public RequestRecord(Instant startTime, String requestType, long latency, int responseCode) {
            this.startTime = startTime;
            this.requestType = requestType;
            this.latency = latency;
            this.responseCode = responseCode;
        }

        public String getRequestType() {
            return requestType;
        }

        public long getLatency() {
            return latency;
        }

        @Override
        public String toString() {
            return startTime + "," + requestType + "," + latency + "," + responseCode;
        }
    }
}