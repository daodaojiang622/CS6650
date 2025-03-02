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

public class ClientPost implements Runnable {
  private static AtomicInteger successCount = new AtomicInteger(0);
  private static AtomicInteger failCount = new AtomicInteger(0);

  private String postUrl;
  private CloseableHttpClient client;
  private List<Row> data;
  private File file;

  public ClientPost(String serverAddress, CloseableHttpClient client, List<Row> data, File file) {
    this.postUrl = serverAddress + "/album"; // Ensure correct API URL
    this.client = client;
    this.data = data;
    this.file = file;
  }

  @Override
  public void run() {
    long start = System.currentTimeMillis();

    // Build the multipart form request
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.setMode(HttpMultipartMode.STRICT);

    // Attach the image file
    builder.addBinaryBody("image", file, ContentType.IMAGE_JPEG, "image.jpeg");

    // Add the JSON profile data
    String jsonProfile = "{\"artist\":\"AgustD\",\"title\":\"D-Day\",\"year\":\"2023\"}";
    builder.addTextBody("profile", jsonProfile, ContentType.APPLICATION_JSON);

    HttpEntity entity = builder.build();
    HttpPost postMethod = new HttpPost(postUrl);
    postMethod.setEntity(entity);

    try (CloseableHttpResponse response = client.execute(postMethod)) {
      int statusCode = response.getCode();
      long end = System.currentTimeMillis();
      long latency = end - start;

      if (statusCode >= 200 && statusCode < 300) {
        successCount.incrementAndGet();
      } else {
        failCount.incrementAndGet();
        System.err.println("POST request failed with status: " + statusCode);
      }

      // Ensure response is properly closed
      if (response.getEntity() != null) {
        EntityUtils.consume(response.getEntity());
      }

      data.add(RowFactory.create(start, "POST", latency, statusCode));

    } catch (IOException e) {
      failCount.incrementAndGet();
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
