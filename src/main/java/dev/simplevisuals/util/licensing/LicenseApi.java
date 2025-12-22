package dev.simplevisuals.util.licensing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class LicenseApi {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private LicenseApi() {}

    public static JsonObject redeem(String serverUrl, String code, String uuid, String hwid) throws Exception {
        String url = (serverUrl == null || serverUrl.isBlank()) ? LicenseManager.DEFAULT_SERVER_URL : serverUrl.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        JsonObject req = new JsonObject();
        req.addProperty("code", code == null ? "" : code.trim());
        req.addProperty("uuid", uuid == null ? "" : uuid.trim());
        req.addProperty("hwid", hwid == null ? "" : hwid.trim());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/redeem"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(req.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = resp.body() == null ? "" : resp.body();

        JsonObject obj;
        try {
            obj = JsonParser.parseString(body).getAsJsonObject();
        } catch (Throwable t) {
            String snippet = body;
            if (snippet.length() > 200) snippet = snippet.substring(0, 200);
            throw new IllegalStateException("bad_json_response:" + resp.statusCode() + ":" + snippet);
        }

        if (resp.statusCode() != 200) {
            String err = obj.has("error") ? obj.get("error").getAsString() : ("http_" + resp.statusCode());
            throw new IllegalStateException(err + " (" + resp.statusCode() + ")");
        }

        return obj;
    }

    /**
     * Проверяет статус лицензии на сервере (revoked/expired)
     * @return JsonObject с полями: valid (boolean), error (string, optional), reason (string, optional)
     */
    public static JsonObject checkStatus(String serverUrl, String uuid, String hwid) throws Exception {
        String url = (serverUrl == null || serverUrl.isBlank()) ? LicenseManager.DEFAULT_SERVER_URL : serverUrl.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        JsonObject req = new JsonObject();
        req.addProperty("uuid", uuid == null ? "" : uuid.trim());
        req.addProperty("hwid", hwid == null ? "" : hwid.trim());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/check_status"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(req.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = resp.body() == null ? "" : resp.body();

        JsonObject obj;
        try {
            obj = JsonParser.parseString(body).getAsJsonObject();
        } catch (Throwable t) {
            throw new IllegalStateException("bad_json_response:" + resp.statusCode());
        }

        return obj;
    }
}
