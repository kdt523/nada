package network.nada;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class PathFinder {

    static final int MAX_HOPS = 3;

    private final PeopleGraph graph;

    public PathFinder(PeopleGraph graph) {
        this.graph = graph;
    }

    /**
     * Breadth-first search from one person to another.
     * Returns the people along the shortest path (both ends included),
     * or null if there is no path within MAX_HOPS.
     */
    public List<Person> shortestPath(int from, int to) {
        Map<Integer, Integer> cameFrom = new HashMap<>();
        Map<Integer, Integer> hops = new HashMap<>();
        Queue<Integer> queue = new ArrayDeque<>();

        hops.put(from, 0);
        queue.add(from);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            if (current == to) {
                return buildPath(cameFrom, to);
            }
            if (hops.get(current) == MAX_HOPS) {
                continue;
            }
            for (Person contact : graph.contactsOf(current)) {
                if (!hops.containsKey(contact.id())) {
                    hops.put(contact.id(), hops.get(current) + 1);
                    cameFrom.put(contact.id(), current);
                    queue.add(contact.id());
                }
            }
        }
        return null;
    }

    private List<Person> buildPath(Map<Integer, Integer> cameFrom, int to) {
        List<Person> path = new ArrayList<>();
        Integer step = to;
        while (step != null) {
            path.add(graph.find(step));
            step = cameFrom.get(step);
        }
        Collections.reverse(path);
        return path;
    }
}
