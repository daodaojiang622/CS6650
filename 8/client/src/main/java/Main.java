import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.StructType;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Main {
  private static final int GROUP_SIZE = 10;
  private static final int NUMBER_OF_GROUPS = 10;
  private static final int DELAY = 2;
  // IP address of the java servlet server
  private static final String IPAddr = "localhost:9090/server_1_0_SNAPSHOT_war";
  private static final String FILE_PATH = "src/main/resources/image.jpeg";

  public static void main(String[] args) {
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(1000);
    connectionManager.setDefaultMaxPerRoute(1000);

    CloseableHttpClient client = HttpClients.custom().setConnectionManager(connectionManager)
            .setRetryStrategy(new DefaultHttpRequestRetryStrategy(5, TimeValue.ofSeconds(2))).build();

    try {
      SparkSession spark =
              SparkSession.builder().appName("data").config("spark.master", "local").getOrCreate();
      spark.sparkContext().setLogLevel("ERROR");
      StructType schema =
              new StructType().add("start", "long").add("requestType", "string").add("latency", "long")
                      .add("code", "integer");
      List<Row> list = Collections.synchronizedList(new ArrayList<>());
      File file = new File(FILE_PATH);
//      ClientGet clientGet = new ClientGet(IPAddr, client, list);
      ClientPost clientPost = new ClientPost(IPAddr, client, list, file);

      int totalThreads = GROUP_SIZE * NUMBER_OF_GROUPS;

      CountDownLatch completed2 = new CountDownLatch(totalThreads);
      long start = System.currentTimeMillis();
      for (int j = 0; j < NUMBER_OF_GROUPS; j++) {
        for (int i = 0; i < GROUP_SIZE; i++) {
          Runnable thread = () -> {
            for (int k = 0; k < 1; k++) {
//              clientGet.run();
              clientPost.run();
            }
            completed2.countDown();
            //System.out.println("Thread completed");
          };
          new Thread(thread).start();
          //System.out.println("Thread started");
        }
        if (j != NUMBER_OF_GROUPS - 1) {
          Thread.sleep(DELAY * 1000);
        }
      }
      completed2.await();

      long end = System.currentTimeMillis();
      Dataset<Row> data = spark.createDataFrame(list, schema);
//      data.show();
//      Dataset<Row> getMean = data.filter("requestType == 'GET'").groupBy().avg("latency");
      Dataset<Row> postMean = data.groupBy().avg("latency");
//      Dataset<Row> getMin = data.filter("requestType == 'GET'").groupBy().min("latency");
      Dataset<Row> postMin = data.groupBy().min("latency");
//      Dataset<Row> getMax = data.filter("requestType == 'GET'").groupBy().max("latency");
      Dataset<Row> postMax = data.groupBy().max("latency");
//      double[] get5099 = data.filter("requestType == 'GET'").stat()
//              .approxQuantile("latency", new double[] {0.5, 0.99}, 0);
      double[] post5099 = data.stat()
              .approxQuantile("latency", new double[] {0.5, 0.99}, 0);
//      System.out.println("GET Mean Latency: " + getMean.first().getDouble(0));
      System.out.println("[RUN CONFIG]: " + GROUP_SIZE + " " + NUMBER_OF_GROUPS + " "+ DELAY);
      System.out.println("POST Mean Latency: " + postMean.first().getDouble(0));
//      System.out.println("GET Min Latency: " + getMin.first().getLong(0));
      System.out.println("POST Min Latency: " + postMin.first().getLong(0));
//      System.out.println("GET Max Latency: " + getMax.first().getLong(0));
      System.out.println("POST Max Latency: " + postMax.first().getLong(0));
//      System.out.println("GET 50th Percentile: " + get5099[0]);
      System.out.println("POST 50th Percentile: " + post5099[0]);
//      System.out.println("GET 99th Percentile: " + get5099[1]);
      System.out.println("POST 99th Percentile: " + post5099[1]);

      // overwrite csv automatically
      data.write()
          .format("csv")
          .mode("overwrite")
          .save(GROUP_SIZE + "_" + NUMBER_OF_GROUPS + "_" + DELAY +".csv");
      spark.stop();

      System.out.println("Time taken: " + (end - start) / 1000 + "s");
      System.out.println(
              "Throughput: "
              + ((totalThreads * 2000)) / ((end - start) / 1000)
              + " requests/s");

      int postSuccesses = ClientPost.getSuccessCount();
      int postFails = ClientPost.getFailCount();
//      int getSuccesses = ClientGet.getSuccessCount();
//      int getFails = ClientGet.getFailCount();
//      System.out.println("Number of successful GET requests: " + getSuccesses);
//      System.out.println("Number of failed GET requests: " + getFails);
      System.out.println("Number of successful requests: " + postSuccesses);
      System.out.println("Number of failed requests: " + postFails);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
