package org.mapasimulador.mapasimulador.model;

import com.google.gson.annotations.Expose;

public class Edge {
    @Expose
    private String id;
    @Expose
    private String source;
    @Expose
    private String target;
    @Expose
    private double length;
    @Expose
    private double travel_time;
    @Expose
    private boolean oneway;
    @Expose
    private double maxspeed;

    public Edge() {
        // Construtor padrão para JSON
    }

    public Edge(String id, String source, String target, double length, double travel_time, boolean oneway, double maxspeed) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.length = length;
        this.travel_time = travel_time;
        this.oneway = oneway;
        this.maxspeed = maxspeed;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getTravel_time() {
        return travel_time;
    }

    public void setTravel_time(double travel_time) {
        this.travel_time = travel_time;
    }

    public boolean isOneway() {
        return oneway;
    }

    public void setOneway(boolean oneway) {
        this.oneway = oneway;
    }

    public double getMaxspeed() {
        return maxspeed;
    }

    public void setMaxspeed(double maxspeed) {
        this.maxspeed = maxspeed;
    }

    // Métodos de conveniência para compatibilidade
    public double getDistance() {
        return length;
    }

    public double getSpeedLimit() {
        return maxspeed;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "id='" + id + '\'' +
                ", source='" + source + '\'' +
                ", target='" + target + '\'' +
                ", length=" + length +
                ", travel_time=" + travel_time +
                ", oneway=" + oneway +
                ", maxspeed=" + maxspeed +
                '}';
    }
}