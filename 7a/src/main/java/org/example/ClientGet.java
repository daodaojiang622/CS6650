package org.example;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;

public class ClientGet implements Runnable {
  private static AtomicInteger successCount = new AtomicInteger(0);
  private static AtomicInteger failCount = new AtomicInteger(0);

  private String getUrl;
  private CloseableHttpClient client;
  private List<Row> data;

  public ClientGet(String serverAddress, CloseableHttpClient client, List<Row> data) {
    this.getUrl = serverAddress + "/album/1"; // Ensure localhost:9090 is set
    this.client = client;
    this.data = data;
  }

  @Override
  public void run() {
    HttpGet getMethod = new HttpGet(getUrl);
    long start = System.currentTimeMillis();

    try (CloseableHttpResponse response = client.execute(getMethod)) {
      int statusCode = response.getCode();
      long end = System.currentTimeMillis();
      long latency = end - start;

      if (statusCode >= 200 && statusCode < 300) {
        successCount.incrementAndGet();
      } else {
        failCount.incrementAndGet();
        System.err.println("GET request failed with status: " + statusCode);
      }

      // Read the response body (optional for debugging)
      if (response.getEntity() != null) {
        response.getEntity().getContent().close(); // Properly close response entity
      }

      data.add(RowFactory.create(start, "GET", latency, statusCode));

    } catch (IOException e) {
      failCount.incrementAndGet();
      System.err.println("GET request failed: " + e.getMessage());
    }
  }

  public static int getSuccessCount() {
    return successCount.get();
  }

  public static int getFailCount() {
    return failCount.get();
  }
}
