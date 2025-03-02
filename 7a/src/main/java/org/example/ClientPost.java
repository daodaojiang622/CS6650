package org.example;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import java.time.Duration;

public class ClientPost implements Runnable {
  private static AtomicInteger successCount = new AtomicInteger(0);
  private static AtomicInteger failCount = new AtomicInteger(0);

  private String postUrl;
  private CloseableHttpClient client;
  private List<Row> data;
  private File file;
  private CircuitBreaker circuitBreaker;

  public ClientPost(String serverAddress, CloseableHttpClient client, List<Row> data, File file) {
    this.postUrl = serverAddress + "/album";
    this.client = client;
    this.data = data;
    this.file = file;

    // Configure circuit breaker
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // Open circuit if 50% of requests fail
            .slidingWindowSize(10)    // Check failures over the last 10 requests
            .minimumNumberOfCalls(3)  // Wait for at least 3 calls before making a decision
            .waitDurationInOpenState(Duration.ofSeconds(10)) // Wait 10 sec before retrying
            .automaticTransitionFromOpenToHalfOpenEnabled(true) // Auto-recovery attempt
            .build();

    this.circuitBreaker = CircuitBreaker.of("postBreaker", config);
  }

  @Override
  public void run() {
    if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
      System.err.println("Circuit Breaker is OPEN, skipping POST request");
      return;
    }

    long start = System.currentTimeMillis();

    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.setMode(HttpMultipartMode.STRICT);
    builder.addBinaryBody("image", file, ContentType.IMAGE_JPEG, "image.jpeg");

    String jsonProfile = "{\"artist\":\"AgustD\",\"title\":\"D-Day\",\"year\":\"2023\"}";
    builder.addTextBody("profile", jsonProfile, ContentType.APPLICATION_JSON);

    HttpEntity entity = builder.build();
    HttpPost postMethod = new HttpPost(postUrl);
    postMethod.setEntity(entity);

    try {
      circuitBreaker.acquirePermission(); // Check circuit breaker

      try (CloseableHttpResponse response = client.execute(postMethod)) {
        int statusCode = response.getCode();
        long end = System.currentTimeMillis();
        long latency = end - start;

        if (statusCode >= 200 && statusCode < 300) {
          successCount.incrementAndGet();
          circuitBreaker.onSuccess(latency, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
          failCount.incrementAndGet();
          circuitBreaker.onError(latency, java.util.concurrent.TimeUnit.MILLISECONDS, new IOException("POST failed with status " + statusCode));
          System.err.println("POST request failed with status: " + statusCode);
        }

        if (response.getEntity() != null) {
          EntityUtils.consume(response.getEntity());
        }

        data.add(RowFactory.create(start, "POST", latency, statusCode));
      }
    } catch (IOException e) {
      failCount.incrementAndGet();
      circuitBreaker.onError(0, java.util.concurrent.TimeUnit.MILLISECONDS, e);
      System.err.println("POST request failed: " + e.getMessage());
    }
  }

  public static int getSuccessCount() {
    return successCount.get();
  }

  public static int getFailCount() {
    return failCount.get();
  }
}
