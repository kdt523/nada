# Nada – People & connections API

A small HTTP API that stores people and who they know, and finds how two people
are connected (up to three hops). Data is kept in memory, so it resets when the
server restarts.

Built with Java 17, the JDK's built-in `HttpServer` and Gson. Tests use JUnit 5.

## Running the server

You only need **JDK 17 or newer**. Maven is not required, the wrapper downloads it.

```bash
./mvnw compile exec:java
```

On Windows use `mvnw.cmd compile exec:java`.

The server starts on **port 8080**: `http://localhost:8080`

### Quick try

```bash
curl -X POST localhost:8080/people -d '{"name": "Asha"}'
curl -X POST localhost:8080/people -d '{"name": "Rohan"}'
curl -X POST localhost:8080/knows  -d '{"a": 1, "b": 2}'
curl localhost:8080/people/1/contacts
curl "localhost:8080/path?from=1&to=2"
```

## Endpoints

| Method | Path | Success |
|---|---|---|
| POST | `/people` | `201` with `{"id": 1, "name": "Asha"}` |
| POST | `/knows` | `204`, repeating a connection is fine |
| GET | `/people/{id}/contacts` | `200` with a list (can be empty) |
| GET | `/path?from=1&to=5` | `200` with the shortest path of 3 hops or fewer, or `{"connected": false}` |

Errors come back as `{"error": "..."}`:

- `400` for invalid JSON, a missing field, an empty or blank name, `a` equal to `b`,
  or a missing / non-numeric `from` or `to`
- `404` for an unknown person, e.g. `{"error": "no person 9"}`

## Running the tests

```bash
./mvnw test
```

- `PathFinderTest` covers the path search, including both sides of the three-hop
  limit: in a chain A-B-C-D, A to D is 3 hops; in A-B-C-D-E, A to E is not connected.
- `PeopleApiTest` starts the server on a free port and checks every endpoint,
  the error cases and a Hindi name round trip.
- `PeopleGraphTest` covers the in-memory store.

## Project layout

```
src/main/java/network/nada/
  Main.java          starts the server
  PeopleApi.java     routing, validation and JSON responses
  PeopleGraph.java   people and their contacts, in memory
  PathFinder.java    breadth-first search capped at 3 hops
  Person.java        id + name
  ApiException.java  error with an HTTP status
```

## What I would do next with more time

- Store data in a database (or Neo4j) so it survives a restart.
- Search from both ends at once (bidirectional BFS) to keep path lookups fast
  on a large network.
- Endpoints to update or delete people and remove connections.
- Pagination for people with a lot of contacts.
- Make the port configurable and add basic request logging.

## AI tools used

I used **Claude Code** to help break down the task, scaffold the project and
write parts of the code and tests. I reviewed and tested everything myself.
