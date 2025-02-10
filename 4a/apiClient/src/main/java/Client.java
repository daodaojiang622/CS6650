import java.io.File;
import java.util.concurrent.*;

import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;

public class Client {
  public static void main(String[] args) {
    if (args.length < 4) {
      System.out.println("Usage: java Client <threadGroupSize> <numThreadGroups> <delay> <serverURL>");
      return;
    }

    int threadGroupSize = Integer.parseInt(args[0]);
    int numThreadGroups = Integer.parseInt(args[1]);
    int delay = Integer.parseInt(args[2]);
    String serverURL = args[3];

    ApiClient apiClient = new ApiClient(serverURL);
    File testImage = new File("src/main/resources/example.jpg");
    String profileJson = "{\"artist\": \"The Beatles\", \"title\": \"Abbey Road\", \"year\": \"1969\"}";

    ExecutorService executor = Executors.newFixedThreadPool(threadGroupSize * numThreadGroups);
    long startTime = System.currentTimeMillis();
    int totalRequests = 0;

    for (int i = 0; i < 10; i++) {
      executor.execute(() -> {
        for (int j = 0; j < 100; j++) {
          try {
            Future<SimpleHttpResponse> futurePost = apiClient.sendPostRequest(testImage, profileJson);
            Future<SimpleHttpResponse> futureGet = apiClient.sendGetRequest("some-album-id");

            futurePost.get();
            futureGet.get();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
      totalRequests += 100 * 2;
    }

    for (int i = 0; i < numThreadGroups; i++) {
      System.out.println("Starting thread group " + (i + 1));
      for (int j = 0; j < threadGroupSize; j++) {
        executor.execute(() -> {
          for (int k = 0; k < 1000; k++) {
            try {
              Future<SimpleHttpResponse> futurePost = apiClient.sendPostRequest(testImage, profileJson);
              Future<SimpleHttpResponse> futureGet = apiClient.sendGetRequest("some-album-id");

              futurePost.get();
              futureGet.get();
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
        totalRequests += 1000 * 2;
      }

      try {
        Thread.sleep(delay * 1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    executor.shutdown();
    try {
      if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
        System.err.println("Executor did not terminate in the specified time.");
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
    }

    long endTime = System.currentTimeMillis();
    double totalTimeSec = (endTime - startTime) / 1000.0;

    System.out.println("Wall Time: " + totalTimeSec + " seconds");
    System.out.println("Throughput: " + (totalRequests / totalTimeSec) + " requests/sec");
  }
}