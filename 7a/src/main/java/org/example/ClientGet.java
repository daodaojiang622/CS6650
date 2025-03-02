package org.example;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Duration;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import io.github.resilience4j.circuitbreaker.*;

public class ClientGet implements Runnable {
  private static AtomicInteger successCount = new AtomicInteger(0);
  private static AtomicInteger failCount = new AtomicInteger(0);

  private String getUrl;
  private CloseableHttpClient client;
  private List<Row> data;
  private static CircuitBreaker circuitBreaker;

  public ClientGet(String serverAddress, CloseableHttpClient client, List<Row> data) {
    this.getUrl = serverAddress + "/album/1";
    this.client = client;
    this.data = data;

    // Initialize circuit breaker for GET requests
    circuitBreaker = CircuitBreaker.of("ClientGetCB",
            CircuitBreakerConfig.custom()
                    .failureRateThreshold(50) // Open circuit if 50% requests fail
                    .waitDurationInOpenState(Duration.ofSeconds(10)) // Time before retrying
                    .slidingWindowSize(10) // Tracks last 10 requests
                    .build());
  }

  @Override
  public void run() {
    HttpGet getMethod = new HttpGet(getUrl);
    long start = System.currentTimeMillis();

    // Execute request with circuit breaker protection
    try {
      CloseableHttpResponse response = CircuitBreaker.decorateCallable(
              circuitBreaker, () -> client.execute(getMethod)
      ).call();

      int statusCode = response.getCode();
      long end = System.currentTimeMillis();
      long latency = end - start;

      if (statusCode >= 200 && statusCode < 300) {
        successCount.incrementAndGet();
      } else {
        failCount.incrementAndGet();
        System.err.println("GET request failed with status: " + statusCode);
      }

      if (response.getEntity() != null) {
        response.getEntity().getContent().close();
      }

      data.add(RowFactory.create(start, "GET", latency, statusCode));

    } catch (IOException e) {
      failCount.incrementAndGet();
      System.err.println("GET request failed: " + e.getMessage());
    } catch (Exception e) {
      System.err.println("Circuit Breaker Open - Skipping GET request");
    }
  }

  public static int getSuccessCount() {
    return successCount.get();
  }

  public static int getFailCount() {
    return failCount.get();
  }
}
