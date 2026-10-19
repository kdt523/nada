package network.nada;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PathFinderTest {

    // Links the names in a line: first - second - third ...
    private PeopleGraph chain(String... names) {
        PeopleGraph graph = new PeopleGraph();
        for (String name : names) {
            graph.addPerson(name);
        }
        for (int id = 1; id < names.length; id++) {
            graph.connect(id, id + 1);
        }
        return graph;
    }

    @Test
    void threeHopsAwayIsConnected() {
        PeopleGraph graph = chain("A", "B", "C", "D");

        List<Person> path = new PathFinder(graph).shortestPath(1, 4);

        assertNotNull(path);
        assertEquals(3, path.size() - 1);
        assertEquals(List.of("A", "B", "C", "D"), path.stream().map(Person::name).toList());
    }

    @Test
    void fourHopsAwayIsNotConnected() {
        PeopleGraph graph = chain("A", "B", "C", "D", "E");

        assertNull(new PathFinder(graph).shortestPath(1, 5));
    }

    @Test
    void samePersonIsZeroHops() {
        PeopleGraph graph = chain("A");

        List<Person> path = new PathFinder(graph).shortestPath(1, 1);

        assertEquals(List.of(graph.find(1)), path);
    }

    @Test
    void picksTheShorterRoute() {
        // 1 - 2 - 3 - 4, plus a shortcut 1 - 4
        PeopleGraph graph = chain("A", "B", "C", "D");
        graph.connect(1, 4);

        List<Person> path = new PathFinder(graph).shortestPath(1, 4);

        assertEquals(2, path.size());
    }

    @Test
    void noConnectionAtAll() {
        PeopleGraph graph = new PeopleGraph();
        graph.addPerson("A");
        graph.addPerson("B");

        assertNull(new PathFinder(graph).shortestPath(1, 2));
    }
}
