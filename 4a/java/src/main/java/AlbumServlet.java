import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;

@WebServlet("/albums/*")
@MultipartConfig(
        fileSizeThreshold = 1048576, // 1MB
        maxFileSize = 5242880,       // 5MB
        maxRequestSize = 20971520    // 20MB
)
public class AlbumServlet extends HttpServlet {
    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");

        // Handle file upload
        Part imagePart = request.getPart("image");
        if (imagePart == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorMessage("Image file is required")));
            return;
        }

        // Calculate image size
        long imageSize = imagePart.getSize();

        // Extract album details from the form-data
        String artist = request.getParameter("profile[artist]");
        String title = request.getParameter("profile[title]");
        String year = request.getParameter("profile[year]");

        // Validate required fields
        if (artist == null || title == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorMessage("Artist and Title are required")));
            return;
        }

        // Simulate album storage with a static ID
        JsonObject responseBody = new JsonObject();
        responseBody.addProperty("albumID", "12345");
        responseBody.addProperty("imageSize", imageSize + " bytes");

        response.getWriter().write(gson.toJson(responseBody));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");

        // Extract album ID from the request path
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorMessage("Album ID required")));
            return;
        }

        String albumID = pathInfo.substring(1);

        // Return fixed album data for testing
        if (!albumID.equals("12345")) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(gson.toJson(new ErrorMessage("Album not found")));
            return;
        }

        AlbumInfo album = new AlbumInfo("Sex Pistols", "Never Mind The Bollocks!", "1977");
        response.getWriter().write(gson.toJson(album));
    }

    // Helper classes for JSON responses
    static class ErrorMessage {
        private final String msg;

        public ErrorMessage(String msg) {
            this.msg = msg;
        }
    }

    static class AlbumInfo {
        private final String artist;
        private final String title;
        private final String year;

        public AlbumInfo(String artist, String title, String year) {
            this.artist = artist;
            this.title = title;
            this.year = year;
        }
    }
}