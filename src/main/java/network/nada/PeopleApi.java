package network.nada;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Receives every request and passes it to the matching endpoint.
 */
public class PeopleApi implements HttpHandler {

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final PeopleGraph graph;
    private final PathFinder pathFinder;

    public PeopleApi(PeopleGraph graph) {
        this.graph = graph;
        this.pathFinder = new PathFinder(graph);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (ApiException e) {
            sendJson(exchange, e.status(), Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, Map.of("error", "internal server error"));
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        // "/people/1/contacts" splits into ["", "people", "1", "contacts"]
        String[] parts = exchange.getRequestURI().getPath().split("/");

        if (parts.length == 2 && parts[1].equals("people")) {
            requireMethod(method, "POST");
            createPerson(exchange);
        } else if (parts.length == 2 && parts[1].equals("knows")) {
            requireMethod(method, "POST");
            connect(exchange);
        } else if (parts.length == 4 && parts[1].equals("people") && parts[3].equals("contacts")) {
            requireMethod(method, "GET");
            contacts(exchange, parts[2]);
        } else if (parts.length == 2 && parts[1].equals("path")) {
            requireMethod(method, "GET");
            path(exchange);
        } else {
            throw new ApiException(404, "not found");
        }
    }

    private void createPerson(HttpExchange exchange) throws IOException {
        JsonObject body = readBody(exchange);
        JsonElement name = body.get("name");
        if (name == null || !name.isJsonPrimitive() || !name.getAsJsonPrimitive().isString()) {
            throw new ApiException(400, "name is required");
        }
        if (name.getAsString().isBlank()) {
            throw new ApiException(400, "name must not be blank");
        }
        Person person = graph.addPerson(name.getAsString());
        sendJson(exchange, 201, person);
    }

    private void connect(HttpExchange exchange) throws IOException {
        JsonObject body = readBody(exchange);
        int a = readId(body, "a");
        int b = readId(body, "b");
        if (a == b) {
            throw new ApiException(400, "a and b must be different people");
        }
        requirePerson(a);
        requirePerson(b);
        graph.connect(a, b);
        exchange.sendResponseHeaders(204, -1);
    }

    private void contacts(HttpExchange exchange, String idText) throws IOException {
        int id = parseId(idText, "id");
        requirePerson(id);
        sendJson(exchange, 200, graph.contactsOf(id));
    }

    private void path(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        int from = parseId(query.get("from"), "from");
        int to = parseId(query.get("to"), "to");
        requirePerson(from);
        requirePerson(to);

        List<Person> path = pathFinder.shortestPath(from, to);

        Map<String, Object> result = new LinkedHashMap<>();
        if (path == null) {
            result.put("connected", false);
        } else {
            result.put("connected", true);
            result.put("hops", path.size() - 1);
            result.put("path", path);
        }
        sendJson(exchange, 200, result);
    }

    private void requireMethod(String actual, String expected) {
        if (!actual.equals(expected)) {
            throw new ApiException(405, "method not allowed");
        }
    }

    private void requirePerson(int id) {
        if (graph.find(id) == null) {
            throw new ApiException(404, "no person " + id);
        }
    }

    private JsonObject readBody(HttpExchange exchange) {
        JsonElement json;
        try (JsonReader reader = new JsonReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            reader.setStrictness(Strictness.STRICT);
            json = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new ApiException(400, "invalid JSON");
            }
        } catch (JsonParseException | IOException | IllegalStateException e) {
            throw new ApiException(400, "invalid JSON");
        }
        if (!json.isJsonObject()) {
            throw new ApiException(400, "request body must be a JSON object");
        }
        return json.getAsJsonObject();
    }

    private int readId(JsonObject body, String field) {
        JsonElement value = body.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new ApiException(400, field + " is required and must be a number");
        }
        try {
            return new BigDecimal(value.getAsString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new ApiException(400, field + " must be a whole number");
        }
    }

    private int parseId(String text, String name) {
        if (text == null || text.isEmpty()) {
            throw new ApiException(400, name + " is required");
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new ApiException(400, name + " must be a number");
        }
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> query = new HashMap<>();
        if (rawQuery == null) {
            return query;
        }
        for (String pair : rawQuery.split("&")) {
            String[] keyValue = pair.split("=", 2);
            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
            query.put(key, value);
        }
        return query;
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
