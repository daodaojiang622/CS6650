import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientGet implements Runnable {
  private static AtomicInteger successCount = new AtomicInteger(0);
  private static AtomicInteger failCount = new AtomicInteger(0);

  private String getUrl;
  private CloseableHttpClient client;
  private List<Row> data;
  private boolean isReview;
  private ConcurrentLinkedQueue<Integer> albumIds;
  private volatile boolean stop;
  private Random random = new Random();

  public ClientGet(String IPAddr, CloseableHttpClient client, List<Row> data) {
    this.getUrl = "http://" + IPAddr + "/album/1";
    this.client = client;
    this.data = data;
    this.isReview = false;
  }

  public ClientGet(CloseableHttpClient client, List<Row> data, ConcurrentLinkedQueue<Integer> albumIds) {
    this.client = client;
    this.data = data;
    this.albumIds = albumIds;
    this.isReview = true;
    this.stop = false;
  }

  public void stopRunning() {
    this.stop = true;
  }

  public void run() {
    if (isReview) {
      runReview();
    } else {
      runAlbum();
    }
  }

  private void runAlbum() {
    HttpGet getMethod = new HttpGet(getUrl);
    try {
      long start = System.currentTimeMillis();
      CloseableHttpResponse response = client.execute(getMethod);
      int statusCode = response.getCode();
      byte[] responseBody = response.getEntity().getContent().readAllBytes();
      long end = System.currentTimeMillis();
      long latency = end - start;
      data.add(RowFactory.create(start, "Album_GET", latency, statusCode));
      if (statusCode >= 200 && statusCode < 300) {
        successCount.incrementAndGet();
      } else {
        failCount.incrementAndGet();
      }
    } catch (IOException e) {
      failCount.incrementAndGet();
    }
  }

  private void runReview() {
    while (!stop) {
      if (albumIds.isEmpty()) continue;
      int randomAlbumId = albumIds.stream().skip(random.nextInt(albumIds.size())).findFirst().orElse(1);
      String url = "http://52.42.26.168:8080/server-1.0-SNAPSHOT/review/" + randomAlbumId;

      HttpGet get = new HttpGet(url);
      long start = System.currentTimeMillis();

      try (CloseableHttpResponse response = client.execute(get)) {
        int statusCode = response.getCode();
        long latency = System.currentTimeMillis() - start;
        data.add(RowFactory.create(start, "Review_GET", latency, statusCode));
        if (statusCode >= 200 && statusCode < 300) {
          successCount.incrementAndGet();
        } else {
          failCount.incrementAndGet();
        }
      } catch (IOException e) {
        failCount.incrementAndGet();
      }
    }
  }

  public static int getSuccessCount() {
    return successCount.get();
  }

  public static int getFailCount() {
    return failCount.get();
  }
}
