package network.nada;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Keeps people and who knows whom in memory.
 * Connections are mutual, so each one is stored on both sides.
 */
public class PeopleGraph {

    private final Map<Integer, Person> people = new HashMap<>();
    private final Map<Integer, Set<Integer>> contacts = new HashMap<>();
    private int nextId = 1;

    public synchronized Person addPerson(String name) {
        Person person = new Person(nextId++, name);
        people.put(person.id(), person);
        contacts.put(person.id(), new TreeSet<>());
        return person;
    }

    public synchronized Person find(int id) {
        return people.get(id);
    }

    public synchronized void connect(int a, int b) {
        contacts.get(a).add(b);
        contacts.get(b).add(a);
    }

    public synchronized List<Person> contactsOf(int id) {
        List<Person> result = new ArrayList<>();
        for (int contactId : contacts.get(id)) {
            result.add(people.get(contactId));
        }
        return result;
    }
}
