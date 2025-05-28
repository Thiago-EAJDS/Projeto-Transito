package org.mapasimulador.mapasimulador.model;

import com.google.gson.annotations.Expose;

public class Node {
    @Expose
    private String id;
    @Expose
    private double latitude;
    @Expose
    private double longitude;

    // TrafficLight não será serializado/deserializado pelo JSON
    // Será criado programaticamente
    private transient TrafficLight trafficLight;

    public Node() {
        // Construtor padrão para JSON
    }

    public Node(String id, double latitude, double longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public TrafficLight getTrafficLight() {
        return trafficLight;
    }

    public void setTrafficLight(TrafficLight trafficLight) {
        this.trafficLight = trafficLight;
    }

    // Métodos para converter coordenadas geográficas para coordenadas de tela
    public double getScreenX(double minLongitude, double maxLongitude, double screenWidth) {
        return ((longitude - minLongitude) / (maxLongitude - minLongitude)) * screenWidth;
    }

    public double getScreenY(double minLatitude, double maxLatitude, double screenHeight) {
        // Invertemos Y porque as coordenadas de tela têm origem no topo
        return screenHeight - ((latitude - minLatitude) / (maxLatitude - minLatitude)) * screenHeight;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id='" + id + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}