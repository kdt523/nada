package network.nada;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PeopleApiTest {

    private HttpServer server;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void startServer() throws Exception {
        server = Main.startServer(0);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(url(path))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(url(path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI url(String path) {
        return URI.create("http://localhost:" + server.getAddress().getPort() + path);
    }

    @Test
    void createsPerson() throws Exception {
        HttpResponse<String> response = post("/people", "{\"name\": \"Asha\"}");

        assertEquals(201, response.statusCode());
        assertEquals("{\"id\":1,\"name\":\"Asha\"}", response.body());
    }

    @Test
    void keepsHindiNameExactly() throws Exception {
        HttpResponse<String> response = post("/people", "{\"name\": \"आशा\"}");

        assertEquals(201, response.statusCode());
        assertEquals("{\"id\":1,\"name\":\"आशा\"}", response.body());
    }

    @Test
    void rejectsBadPersonRequests() throws Exception {
        assertEquals(400, post("/people", "not json").statusCode());
        assertEquals(400, post("/people", "").statusCode());
        assertEquals(400, post("/people", "{}").statusCode());
        assertEquals(400, post("/people", "{\"name\": \"   \"}").statusCode());
        assertEquals(400, post("/people", "{\"name\": 5}").statusCode());
    }

    @Test
    void connectsAndListsContacts() throws Exception {
        post("/people", "{\"name\": \"Asha\"}");
        post("/people", "{\"name\": \"Rohan\"}");

        assertEquals(204, post("/knows", "{\"a\": 1, \"b\": 2}").statusCode());
        assertEquals(204, post("/knows", "{\"a\": 1, \"b\": 2}").statusCode());

        HttpResponse<String> response = get("/people/1/contacts");
        assertEquals(200, response.statusCode());
        assertEquals("[{\"id\":2,\"name\":\"Rohan\"}]", response.body());
    }

    @Test
    void contactsCanBeEmpty() throws Exception {
        post("/people", "{\"name\": \"Asha\"}");

        assertEquals("[]", get("/people/1/contacts").body());
    }

    @Test
    void rejectsBadConnections() throws Exception {
        post("/people", "{\"name\": \"Asha\"}");

        assertEquals(400, post("/knows", "{\"a\": 1, \"b\": 1}").statusCode());
        assertEquals(400, post("/knows", "{\"a\": 1}").statusCode());
        assertEquals(400, post("/knows", "{bad").statusCode());

        HttpResponse<String> response = post("/knows", "{\"a\": 1, \"b\": 9}");
        assertEquals(404, response.statusCode());
        assertEquals("{\"error\":\"no person 9\"}", response.body());
    }

    @Test
    void unknownPersonIs404() throws Exception {
        HttpResponse<String> response = get("/people/9/contacts");

        assertEquals(404, response.statusCode());
        assertEquals("{\"error\":\"no person 9\"}", response.body());
    }

    @Test
    void pathRespectsThreeHopLimit() throws Exception {
        // A - B - C - D - E
        for (String name : new String[] {"A", "B", "C", "D", "E"}) {
            post("/people", "{\"name\": \"" + name + "\"}");
        }
        for (int id = 1; id < 5; id++) {
            post("/knows", "{\"a\": " + id + ", \"b\": " + (id + 1) + "}");
        }

        HttpResponse<String> toD = get("/path?from=1&to=4");
        assertEquals(200, toD.statusCode());
        assertEquals("{\"connected\":true,\"hops\":3,\"path\":["
                + "{\"id\":1,\"name\":\"A\"},{\"id\":2,\"name\":\"B\"},"
                + "{\"id\":3,\"name\":\"C\"},{\"id\":4,\"name\":\"D\"}]}", toD.body());

        HttpResponse<String> toE = get("/path?from=1&to=5");
        assertEquals(200, toE.statusCode());
        assertEquals("{\"connected\":false}", toE.body());
    }

    @Test
    void pathToSelfIsZeroHops() throws Exception {
        post("/people", "{\"name\": \"Asha\"}");

        assertEquals("{\"connected\":true,\"hops\":0,\"path\":[{\"id\":1,\"name\":\"Asha\"}]}",
                get("/path?from=1&to=1").body());
    }

    @Test
    void rejectsBadPathParameters() throws Exception {
        post("/people", "{\"name\": \"Asha\"}");

        assertEquals(400, get("/path?from=1").statusCode());
        assertEquals(400, get("/path").statusCode());
        assertEquals(400, get("/path?from=abc&to=1").statusCode());
        assertEquals(404, get("/path?from=1&to=9").statusCode());
    }
}
