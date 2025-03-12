import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientPost implements Runnable {
  private static AtomicInteger successCount = new AtomicInteger(0);
  private static AtomicInteger failCount = new AtomicInteger(0);
  private String postAlbumUrl;
  private String postReviewUrl;
  private CloseableHttpClient client;
  private List<Row> data;
  private File file;

  public ClientPost(String IPAddr, CloseableHttpClient client, List<Row> data, File file) {
    this.postAlbumUrl = "http://" + IPAddr + "/IGORTON/AlbumStore/1.0.0/albums";
    this.postReviewUrl = "http://" + IPAddr + "/review";  // URL for likes/dislikes
    this.client = client;
    this.data = data;
    this.file = file;
  }

  public void run() {
    try {
      // POST a new album and get its albumId
      int albumId = postNewAlbum();
      if (albumId == -1) return;  // Album creation failed, skip likes/dislikes

      // Post two likes
      postReview(albumId, "like");
      postReview(albumId, "like");

      // Post one dislike
      postReview(albumId, "dislike");

//      System.out.println("Data: " + data);

    } catch (Exception e) {
      System.err.println("[ERROR]: " + e.getMessage());
      e.printStackTrace();
    }
  }

  // POST a new album and return albumId
  private int postNewAlbum() {
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.setMode(HttpMultipartMode.STRICT);

    // Add the image file
    builder.addBinaryBody("image", file, ContentType.IMAGE_JPEG, "image.jpeg");

    // Add profile fields separately (NOT JSON)
    builder.addTextBody("profile[artist]", "AgustD", ContentType.TEXT_PLAIN);
    builder.addTextBody("profile[title]", "D-Day", ContentType.TEXT_PLAIN);
    builder.addTextBody("profile[year]", "2023", ContentType.TEXT_PLAIN);

    HttpEntity entity = builder.build();
    HttpPost postMethod = new HttpPost(postAlbumUrl);
    postMethod.setEntity(entity);

    long albumStartTime = System.currentTimeMillis();

    try (CloseableHttpResponse response = client.execute(postMethod)) {
      int statusCode = response.getCode();
      String responseBody = EntityUtils.toString(response.getEntity());

      if (statusCode >= 200 && statusCode < 300) {
        long albumEndTime = System.currentTimeMillis();
        long albumLatency = albumEndTime - albumStartTime;

        data.add(RowFactory.create(albumStartTime, "Album_POST", albumLatency, statusCode));

        successCount.incrementAndGet();
        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
        int albumId = jsonResponse.get("albumId").getAsInt();
        System.out.println("Created Album ID: " + albumId);
        return albumId;
      } else {
        failCount.incrementAndGet();
        System.err.println("[ERROR] POST Album failed: " + statusCode);
        System.err.println("[ERROR] Server Response: " + responseBody);
        return -1;
      }
    } catch (IOException e) {
      failCount.incrementAndGet();
      System.err.println("[ERROR] POST Album error: " + e.getMessage());
      return -1;
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  // POST a like or dislike for an album
  private void postReview(int albumId, String reviewType) {
    String userId = String.valueOf(System.currentTimeMillis());  // Unique user_id
    HttpPost postMethod = new HttpPost(postReviewUrl + "/" + reviewType + "/" + albumId + "?user_id=" + userId);

    long reviewStartTime = System.currentTimeMillis();

    try (CloseableHttpResponse response = client.execute(postMethod)) {
      int statusCode = response.getCode();
      String responseBody = EntityUtils.toString(response.getEntity());

      if (statusCode >= 200 && statusCode < 300) {
        long reviewEndTime = System.currentTimeMillis();
        long reviewLatency = reviewEndTime - reviewStartTime;

        data.add(RowFactory.create(reviewStartTime, "Review_POST", reviewLatency, statusCode));

        successCount.incrementAndGet();
        System.out.println(reviewType.toUpperCase() + " Success for Album ID: " + albumId);
      } else {
        failCount.incrementAndGet();
        System.err.println("[ERROR] POST Review failed: " + statusCode);
        System.err.println("[ERROR] Server Response: " + responseBody);
      }
    } catch (IOException | ParseException e) {
      failCount.incrementAndGet();
      System.err.println("[ERROR] POST Review error: " + e.getMessage());
    }
  }

  public static int getSuccessCount() {
    return successCount.get();
  }

  public static int getFailCount() {
    return failCount.get();
  }
}
