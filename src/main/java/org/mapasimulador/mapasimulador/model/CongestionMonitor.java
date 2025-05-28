package org.mapasimulador.mapasimulador.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class CongestionMonitor {
    private TrafficLightCongestion[] trafficLights;
    private int trafficLightCount;
    private static final int MAX_TRAFFIC_LIGHTS = 100; // Limite máximo de semáforos (não afeta aqui)

    private DoubleProperty overallCongestionLevel;
    private IntegerProperty totalCars;

    // Constantes para cálculo de congestionamento
    private static final int MAX_CARS_PER_LIGHT = 20; // Máximo de carros considerado para 100%
    private static final double CONGESTION_THRESHOLD_LOW = 30.0; // Verde
    private static final double CONGESTION_THRESHOLD_MEDIUM = 60.0; // Amarelo
    // Acima de 60% = Vermelho (Alto congestionamento)

    public CongestionMonitor() {
        this.trafficLights = new TrafficLightCongestion[MAX_TRAFFIC_LIGHTS];
        this.trafficLightCount = 0;
        this.overallCongestionLevel = new SimpleDoubleProperty(0.0);
        this.totalCars = new SimpleIntegerProperty(0);
    }

    // Classe interna para dados de congestionamento por semáforo
    public static class TrafficLightCongestion {
        private String lightId;
        private int carsWaiting;
        private double congestionPercentage;
        private TrafficLight.State currentState;
        private double x, y; // Coordenadas para visualização

        public TrafficLightCongestion(String lightId, double x, double y) {
            this.lightId = lightId;
            this.carsWaiting = 0;
            this.congestionPercentage = 0.0;
            this.currentState = TrafficLight.State.GREEN;
            this.x = x;
            this.y = y;
        }

        public void updateCongestion(int cars, TrafficLight.State state) {
            this.carsWaiting = cars;
            this.currentState = state;
            this.congestionPercentage = Math.min(100.0, (cars * 100.0) / MAX_CARS_PER_LIGHT);
        }

        public String getCongestionLevel() {
            if (congestionPercentage <= CONGESTION_THRESHOLD_LOW) return "BAIXO";
            else if (congestionPercentage <= CONGESTION_THRESHOLD_MEDIUM) return "MEDIO";
            else return "ALTO";
        }

        public String getCongestionColor() {
            if (congestionPercentage <= CONGESTION_THRESHOLD_LOW) return "#4CAF50"; // Verde
            else if (congestionPercentage <= CONGESTION_THRESHOLD_MEDIUM) return "#FF9800"; // Laranja
            else return "#F44336"; // Vermelho
        }

        // Getters
        public String getLightId() { return lightId; }
        public int getCarsWaiting() { return carsWaiting; }
        public double getCongestionPercentage() { return congestionPercentage; }
        public TrafficLight.State getCurrentState() { return currentState; }
        public double getX() { return x; }
        public double getY() { return y; }
    }

    // Adicionar semáforo ao monitoramento
    public boolean addTrafficLight(String lightId, double x, double y) {
        if (trafficLightCount >= MAX_TRAFFIC_LIGHTS) {
            return false; // Array cheio
        }

        trafficLights[trafficLightCount] = new TrafficLightCongestion(lightId, x, y);
        trafficLightCount++;
        return true;
    }

    // Encontrar semáforo por ID
    private TrafficLightCongestion findTrafficLight(String lightId) {
        for (int i = 0; i < trafficLightCount; i++) {
            if (trafficLights[i].getLightId().equals(lightId)) {
                return trafficLights[i];
            }
        }
        return null;
    }

    // Atualizar dados de um semáforo
    public void updateTrafficLight(String lightId, int carsWaiting, TrafficLight.State state) {
        TrafficLightCongestion light = findTrafficLight(lightId);
        if (light != null) {
            light.updateCongestion(carsWaiting, state);
            calculateOverallCongestion();
        }
    }

    // Calcular congestionamento geral
    private void calculateOverallCongestion() {
        if (trafficLightCount == 0) {
            overallCongestionLevel.set(0.0);
            totalCars.set(0);
            return;
        }

        double totalCongestion = 0.0;
        int totalCarsCount = 0;

        for (int i = 0; i < trafficLightCount; i++) {
            totalCongestion += trafficLights[i].getCongestionPercentage();
            totalCarsCount += trafficLights[i].getCarsWaiting();
        }

        overallCongestionLevel.set(totalCongestion / trafficLightCount);
        totalCars.set(totalCarsCount);
    }

    // Obter dados de congestionamento para visualização
    public CongestionData getCongestionData() {
        return new CongestionData(
                overallCongestionLevel.get(),
                totalCars.get(),
                trafficLights,
                trafficLightCount
        );
    }

    // Classe para transportar dados de congestionamento
    public static class CongestionData {
        private double overallCongestion;
        private int totalCars;
        private TrafficLightCongestion[] lights;
        private int lightCount;

        public CongestionData(double overallCongestion, int totalCars,
                              TrafficLightCongestion[] lights, int lightCount) {
            this.overallCongestion = overallCongestion;
            this.totalCars = totalCars;
            this.lights = lights;
            this.lightCount = lightCount;
        }

        public double getOverallCongestion() { return overallCongestion; }
        public int getTotalCars() { return totalCars; }
        public TrafficLightCongestion[] getLights() { return lights; }
        public int getLightCount() { return lightCount; }

        public String getOverallCongestionLevel() {
            if (overallCongestion <= CONGESTION_THRESHOLD_LOW) return "BAIXO";
            else if (overallCongestion <= CONGESTION_THRESHOLD_MEDIUM) return "MEDIO";
            else return "ALTO";
        }

        public String getOverallCongestionColor() {
            if (overallCongestion <= CONGESTION_THRESHOLD_LOW) return "#4CAF50";
            else if (overallCongestion <= CONGESTION_THRESHOLD_MEDIUM) return "#FF9800";
            else return "#F44336";
        }
    }

    // Properties para binding com UI
    public DoubleProperty overallCongestionProperty() {
        return overallCongestionLevel;
    }

    public IntegerProperty totalCarsProperty() {
        return totalCars;
    }

    // Resetar todos os dados
    public void reset() {
        for (int i = 0; i < trafficLightCount; i++) {
            trafficLights[i] = null;
        }
        trafficLightCount = 0;
        overallCongestionLevel.set(0.0);
        totalCars.set(0);
    }

    // Estatísticas adicionais
    public int getActiveLights() {
        return trafficLightCount;
    }

    public int getHighCongestionLights() {
        int count = 0;
        for (int i = 0; i < trafficLightCount; i++) {
            if (trafficLights[i].getCongestionPercentage() > CONGESTION_THRESHOLD_MEDIUM) {
                count++;
            }
        }
        return count;
    }
}