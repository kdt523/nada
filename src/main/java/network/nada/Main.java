package network.nada;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        startServer(PORT);
        System.out.println("Server running on http://localhost:" + PORT);
    }

    // Tests pass port 0 so the system picks a free port.
    static HttpServer startServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new PeopleApi(new PeopleGraph()));
        server.start();
        return server;
    }
}
