package network.nada;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PeopleGraphTest {

    @Test
    void assignsIdsInOrder() {
        PeopleGraph graph = new PeopleGraph();
        assertEquals(1, graph.addPerson("Asha").id());
        assertEquals(2, graph.addPerson("Rohan").id());
    }

    @Test
    void connectionIsMutualAndCanBeRepeated() {
        PeopleGraph graph = new PeopleGraph();
        Person asha = graph.addPerson("Asha");
        Person rohan = graph.addPerson("Rohan");

        graph.connect(asha.id(), rohan.id());
        graph.connect(asha.id(), rohan.id());

        assertEquals(List.of(rohan), graph.contactsOf(asha.id()));
        assertEquals(List.of(asha), graph.contactsOf(rohan.id()));
    }

    @Test
    void newPersonHasNoContacts() {
        PeopleGraph graph = new PeopleGraph();
        Person asha = graph.addPerson("Asha");
        assertTrue(graph.contactsOf(asha.id()).isEmpty());
    }
}
