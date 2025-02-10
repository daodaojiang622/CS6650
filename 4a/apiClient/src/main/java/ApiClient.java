import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

public class ApiClient {
  private final String serverUrl;
  private final CloseableHttpAsyncClient asyncClient;

  public ApiClient(String serverUrl) {
    this.serverUrl = serverUrl;

    PoolingAsyncClientConnectionManager connectionManager = new PoolingAsyncClientConnectionManager();
    connectionManager.setMaxTotal(1000);
    connectionManager.setDefaultMaxPerRoute(1000);


    this.asyncClient = HttpAsyncClients.custom()
        .setConnectionManager(connectionManager)
        .build();
    this.asyncClient.start();
  }

  public Future<SimpleHttpResponse> sendPostRequest(File imageFile, String profileJson) {
    String url = serverUrl + "/albums";
    SimpleHttpRequest post = SimpleRequestBuilder.post(url)
        .setBody(profileJson, ContentType.APPLICATION_JSON)
        .build();

    return asyncClient.execute(post, null);
  }

  public Future<SimpleHttpResponse> sendGetRequest(String albumID) {
    String url = serverUrl + "/albums/" + albumID;
    SimpleHttpRequest get = SimpleRequestBuilder.get(url).build();

    return asyncClient.execute(get, null);
  }
}