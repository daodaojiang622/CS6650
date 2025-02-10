import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class HttpClient {

    private static final String URL = "http://35.93.48.66:8080/3b_war/hello/";

    public static void main(String[] args) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(URL);
            try (CloseableHttpResponse response = client.execute(request)) {
                System.out.println("Response Code: " + response.getCode());

                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
