package org.mapasimulador.mapasimulador.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutePlannerTest {

    private static Edge edge(String source, String target, double length) {
        return new Edge(source + "->" + target, source, target, length, 0, false, 50);
    }

    private static MapData mapOf(List<String> nodeIds, Edge... edges) {
        List<Node> nodes = new ArrayList<>();
        for (String id : nodeIds) {
            nodes.add(new Node(id, 0, 0));
        }
        return new MapData(nodes, Arrays.asList(edges));
    }

    // A->B direto custa 10; A->C->B custa 2
    private static MapData mapWithShortcut() {
        return mapOf(List.of("A", "B", "C"),
                edge("A", "B", 10), edge("A", "C", 1), edge("C", "B", 1));
    }

    @Test
    void escolheOCaminhoDeMenorCusto() {
        List<Edge> path = new RoutePlanner(mapWithShortcut()).shortestPath("A", "B");

        assertEquals(2, path.size());
        assertEquals("A", path.get(0).getSource());
        assertEquals("C", path.get(0).getTarget());
        assertEquals("C", path.get(1).getSource());
        assertEquals("B", path.get(1).getTarget());
    }

    @Test
    void percorreCadeiaDeTresTrechos() {
        MapData map = mapOf(List.of("A", "B", "C", "D"),
                edge("A", "B", 1), edge("B", "C", 1), edge("C", "D", 1), edge("A", "D", 10));

        List<Edge> path = new RoutePlanner(map).shortestPath("A", "D");

        assertEquals(3, path.size());
        assertEquals("D", path.get(2).getTarget());
    }

    @Test
    void respeitaADirecaoDosTrechos() {
        MapData map = mapOf(List.of("A", "B"), edge("A", "B", 5));

        assertTrue(new RoutePlanner(map).shortestPath("B", "A").isEmpty());
    }

    @Test
    void origemIgualAoDestinoNaoGeraRota() {
        MapData map = mapOf(List.of("A", "B"), edge("A", "B", 5));

        assertTrue(new RoutePlanner(map).shortestPath("A", "A").isEmpty());
    }

    @Test
    void arvoreDeCaminhosMinimosApontaParaOPaiDeCadaNo() {
        Map<String, Edge> tree = new RoutePlanner(mapWithShortcut()).shortestPathTree("A");

        assertEquals("C", tree.get("B").getSource());
        assertEquals("A", tree.get("C").getSource());
        assertFalse(tree.containsKey("A"));
    }
}
