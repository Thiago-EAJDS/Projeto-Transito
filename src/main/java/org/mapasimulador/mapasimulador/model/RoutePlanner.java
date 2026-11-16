package org.mapasimulador.mapasimulador.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Calcula caminhos mínimos sobre o grafo de vias com o algoritmo de Dijkstra.
 *
 * Uma execução a partir de um nó produz uma árvore de caminhos mínimos: cada nó
 * alcançado guarda o trecho que o liga ao seu pai nessa árvore. Os caminhos até
 * qualquer destino são reconstruídos percorrendo essa árvore de volta até a origem.
 * A fila de prioridade é um heap binário.
 */
public class RoutePlanner {
    private final Map<String, List<Edge>> outgoingEdges = new HashMap<>();

    public RoutePlanner(MapData mapData) {
        for (Edge edge : mapData.getEdges()) {
            outgoingEdges.computeIfAbsent(edge.getSource(), id -> new ArrayList<>()).add(edge);
        }
    }

    /**
     * Árvore de caminhos mínimos a partir de {@code sourceId}.
     * Chave: id de um nó alcançável. Valor: trecho que chega até ele pelo caminho mínimo.
     * A origem não aparece como chave.
     */
    public Map<String, Edge> shortestPathTree(String sourceId) {
        Map<String, Double> bestDistance = new HashMap<>();
        Map<String, Edge> parentEdge = new HashMap<>();
        Set<String> settled = new HashSet<>();

        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(
                Comparator.comparingDouble((QueueEntry entry) -> entry.distance));
        bestDistance.put(sourceId, 0.0);
        queue.add(new QueueEntry(sourceId, 0.0));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            if (!settled.add(current.nodeId)) {
                continue; // já finalizado com distância menor
            }

            for (Edge edge : outgoingEdges.getOrDefault(current.nodeId, List.of())) {
                double candidate = current.distance + edge.getLength();
                if (candidate < bestDistance.getOrDefault(edge.getTarget(), Double.POSITIVE_INFINITY)) {
                    bestDistance.put(edge.getTarget(), candidate);
                    parentEdge.put(edge.getTarget(), edge);
                    queue.add(new QueueEntry(edge.getTarget(), candidate));
                }
            }
        }

        return parentEdge;
    }

    /**
     * Sequência de trechos do caminho mínimo de {@code sourceId} até {@code targetId}.
     * Retorna lista vazia se o destino for o próprio nó de origem ou se não houver caminho.
     */
    public List<Edge> shortestPath(String sourceId, String targetId) {
        if (sourceId.equals(targetId)) {
            return List.of();
        }

        Map<String, Edge> tree = shortestPathTree(sourceId);
        LinkedList<Edge> path = new LinkedList<>();
        String node = targetId;

        while (!node.equals(sourceId)) {
            Edge edge = tree.get(node);
            if (edge == null) {
                return List.of(); // destino inalcançável
            }
            path.addFirst(edge);
            node = edge.getSource();
        }

        return path;
    }

    private static final class QueueEntry {
        final String nodeId;
        final double distance;

        QueueEntry(String nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }
}
