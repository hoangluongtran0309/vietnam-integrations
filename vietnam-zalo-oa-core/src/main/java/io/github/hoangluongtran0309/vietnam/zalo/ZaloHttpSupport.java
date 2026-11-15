package io.github.hoangluongtran0309.vietnam.zalo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class ZaloHttpSupport {

    static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BigDecimal.class, new PlainBigDecimalTypeAdapter())
            .create();

    private ZaloHttpSupport() {
    }

    static JsonObject send(HttpClient client, HttpRequest request, String operation) {
        final HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ZaloApiException(operation, "request interrupted; outcome may be unknown and was not retried", e);
        } catch (IOException e) {
            throw new ZaloApiException(operation, "I/O error; outcome may be unknown and was not retried", e);
        }

        JsonObject json;
        try {
            JsonElement parsed = com.google.gson.JsonParser.parseString(response.body());
            if (!parsed.isJsonObject()) {
                throw new JsonParseException("response is not an object");
            }
            json = parsed.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new ZaloApiException(operation, response.statusCode(), null, "invalid JSON response");
        }

        Integer code = integer(json, "error");
        if (response.statusCode() < 200 || response.statusCode() >= 300 || (code != null && code != 0)) {
            throw new ZaloApiException(operation, response.statusCode(), code, string(json, "message"));
        }
        return json;
    }

    static HttpRequest.Builder request(java.net.URI uri, Duration timeout) {
        return HttpRequest.newBuilder(uri).timeout(timeout);
    }

    static String string(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    static Integer integer(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        try {
            return value.getAsInt();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static final class PlainBigDecimalTypeAdapter extends TypeAdapter<BigDecimal> {

        @Override
        public void write(JsonWriter out, BigDecimal value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.jsonValue(value.toPlainString());
            }
        }

        @Override
        public BigDecimal read(JsonReader in) throws IOException {
            return new BigDecimal(in.nextString());
        }
    }
}
