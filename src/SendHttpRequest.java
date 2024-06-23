import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SendHttpRequest {
    public static int sendRequest(String type, String urlString, String contentType, String filePath, String body)
            throws Exception {

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(type);
        connection.setRequestProperty("Content-Type", contentType);
        connection.setDoOutput(true);

        if (filePath != null) {
            boolean isJsFile = filePath.endsWith(".js");
            if (isJsFile) {
                // Read body from JavaScript file
                File file = new File(filePath);
                String requestBody = FileUtils.readFileToString(file, "UTF-8");
                // Write body to the connection
                try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                    outputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
                }
            }
        }

        if (body != null) {
            // Write body content to the connection
            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        // Get response
        int responseCode = connection.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        connection.disconnect();
        return responseCode;
    }
}
