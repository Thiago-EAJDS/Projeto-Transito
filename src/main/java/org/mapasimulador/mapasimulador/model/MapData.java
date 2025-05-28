package org.mapasimulador.mapasimulador.model;

import com.google.gson.annotations.Expose;
import java.util.List;
import java.util.ArrayList;

public class MapData {
    @Expose
    private List<Node> nodes;
    @Expose
    private List<Edge> edges;

    public MapData() {
        // Construtor padrão para JSON
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
    }

    public MapData(List<Node> nodes, List<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    public Node getNodeById(String id) {
        return nodes.stream()
                .filter(node -> node.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<Edge> getEdgesFromNode(String nodeId) {
        return edges.stream()
                .filter(edge -> edge.getSource().equals(nodeId))
                .toList();
    }

    public Edge getEdgeBySourceAndTarget(String source, String target) {
        return edges.stream()
                .filter(edge -> edge.getSource().equals(source) && edge.getTarget().equals(target))
                .findFirst()
                .orElse(null);
    }

    // Métodos para calcular limites do mapa
    public double getMinLatitude() {
        return nodes.stream().mapToDouble(Node::getLatitude).min().orElse(0);
    }

    public double getMaxLatitude() {
        return nodes.stream().mapToDouble(Node::getLatitude).max().orElse(0);
    }

    public double getMinLongitude() {
        return nodes.stream().mapToDouble(Node::getLongitude).min().orElse(0);
    }

    public double getMaxLongitude() {
        return nodes.stream().mapToDouble(Node::getLongitude).max().orElse(0);
    }
}