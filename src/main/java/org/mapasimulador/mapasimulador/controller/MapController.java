package org.mapasimulador.mapasimulador.controller;

import org.mapasimulador.mapasimulador.model.*;
import com.google.gson.Gson;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javafx.animation.Timeline;

public class MapController {
    private MapData mapData;
    private List<Car> cars;
    private Timeline carSpawnTimer;
    private Timeline carMovementTimer;
    private Timeline carRemovalTimer;
    private static final Random random = new Random();
    private static final int MAX_CARS = 5000;
    private static final double CAR_SPAWN_RATE = 50.0;
    private static final double CAR_REMOVAL_DELAY = 0.35;
    private boolean simulationRunning = false;

    private long simulationStartTime;
    private Timeline displayUpdateTimer;

    // Contador total de carros que chegaram ao destino (acumulativo)
    private int totalArrivedCarsCount = 0;
    private int currentArrivedCarsCount = 0;

    // Constantes para detecção de colisão
    private static final double MIN_CAR_DISTANCE = 0.2;
    private static final double TRAFFIC_LIGHT_STOP_DISTANCE = 0.2;

    // Sistema de monitoramento de congestionamento
    private CongestionMonitor congestionMonitor;

    // Calcula rotas dos carros com Dijkstra
    private RoutePlanner routePlanner;
    private static final int ROUTE_ATTEMPTS = 3; // tentativas de achar um destino alcançável

    public MapController() {
        this.cars = new ArrayList<>();
        // Inicializar monitor de congestionamento
        this.congestionMonitor = new CongestionMonitor();

        if (!loadMapData()) {
            System.err.println("Falha ao carregar dados do mapa.");
        }
    }

    // Classe para monitoramento de congestionamento
    public static class CongestionMonitor {
        private final List<TrafficLightCongestion> trafficLights = new ArrayList<>();
        private final Map<String, TrafficLightCongestion> trafficLightsById = new HashMap<>();

        private double overallCongestionLevel;
        private int totalCars;

        // Constantes para cálculo de congestionamento
        private static final int MAX_CARS_PER_LIGHT = 20;
        private static final double CONGESTION_THRESHOLD_LOW = 30.0;
        private static final double CONGESTION_THRESHOLD_MEDIUM = 60.0;

        // Único ponto de classificação do congestionamento (nível e cor)
        static String levelOf(double percentage) {
            if (percentage <= CONGESTION_THRESHOLD_LOW) return "BAIXO";
            else if (percentage <= CONGESTION_THRESHOLD_MEDIUM) return "MEDIO";
            else return "ALTO";
        }

        static String colorOf(double percentage) {
            if (percentage <= CONGESTION_THRESHOLD_LOW) return "#4CAF50";
            else if (percentage <= CONGESTION_THRESHOLD_MEDIUM) return "#FF9800";
            else return "#F44336";
        }

        public CongestionMonitor() {
            this.overallCongestionLevel = 0.0;
            this.totalCars = 0;
        }

        // Classe interna para dados de congestionamento por semáforo
        public static class TrafficLightCongestion {
            private String lightId;
            private int carsWaiting;
            private double congestionPercentage;
            private TrafficLight.State currentState;
            private double x, y;

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

            public String getCongestionLevel() { return levelOf(congestionPercentage); }

            public String getCongestionColor() { return colorOf(congestionPercentage); }

            // Getters
            public String getLightId() { return lightId; }
            public int getCarsWaiting() { return carsWaiting; }
            public double getCongestionPercentage() { return congestionPercentage; }
            public TrafficLight.State getCurrentState() { return currentState; }
            public double getX() { return x; }
            public double getY() { return y; }
        }

        public void addTrafficLight(String lightId, double x, double y) {
            TrafficLightCongestion light = new TrafficLightCongestion(lightId, x, y);
            trafficLights.add(light);
            trafficLightsById.put(lightId, light);
        }

        private TrafficLightCongestion findTrafficLight(String lightId) {
            return trafficLightsById.get(lightId);
        }

        public void updateTrafficLight(String lightId, int carsWaiting, TrafficLight.State state) {
            TrafficLightCongestion light = findTrafficLight(lightId);
            if (light != null) {
                light.updateCongestion(carsWaiting, state);
                calculateOverallCongestion();
            }
        }

        private void calculateOverallCongestion() {
            if (trafficLights.isEmpty()) {
                overallCongestionLevel = 0.0;
                totalCars = 0;
                return;
            }

            double totalCongestion = 0.0;
            int totalCarsCount = 0;

            for (TrafficLightCongestion light : trafficLights) {
                totalCongestion += light.getCongestionPercentage();
                totalCarsCount += light.getCarsWaiting();
            }

            overallCongestionLevel = totalCongestion / trafficLights.size();
            totalCars = totalCarsCount;
        }

        // Getter para dados de congestionamento
        public CongestionData getCongestionData() {
            return new CongestionData(overallCongestionLevel, totalCars, trafficLights);
        }

        // Classe para transportar dados
        public static class CongestionData {
            private double overallCongestion;
            private int totalCars;
            private List<TrafficLightCongestion> lights;

            public CongestionData(double overallCongestion, int totalCars,
                                  List<TrafficLightCongestion> lights) {
                this.overallCongestion = overallCongestion;
                this.totalCars = totalCars;
                this.lights = lights;
            }

            public double getOverallCongestion() { return overallCongestion; }
            public int getTotalCars() { return totalCars; }
            public List<TrafficLightCongestion> getLights() { return lights; }

            public String getOverallCongestionLevel() { return levelOf(overallCongestion); }

            public String getOverallCongestionColor() { return colorOf(overallCongestion); }
        }

        public int getActiveLights() {
            return trafficLights.size();
        }

        public int getHighCongestionLights() {
            int count = 0;
            for (TrafficLightCongestion light : trafficLights) {
                if (light.getCongestionPercentage() > CONGESTION_THRESHOLD_MEDIUM) {
                    count++;
                }
            }
            return count;
        }

        public void reset() {
            trafficLights.clear();
            trafficLightsById.clear();
            overallCongestionLevel = 0.0;
            totalCars = 0;
        }
    }

    private boolean loadMapData() {
        try {
            Gson gson = new com.google.gson.GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();

            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("JoqueiTeresinaPiauíBrazil.json");
            if (inputStream == null) {
                throw new IOException("Arquivo JSON não encontrado!");
            }

            InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
            mapData = gson.fromJson(reader, MapData.class);
            reader.close();

            if (mapData == null || mapData.getNodes() == null || mapData.getEdges() == null) {
                throw new IOException("Dados do mapa inválidos ou incompletos!");
            }

            // Inicializar semáforos para todos os nós e iniciar seus ciclos
            for (Node node : mapData.getNodes()) {
                if (node.getTrafficLight() == null) {
                    node.setTrafficLight(new TrafficLight());
                }
                node.getTrafficLight().start();
            }

            routePlanner = new RoutePlanner(mapData);

            // Registrar semáforos no monitor de congestionamento
            initializeCongestionMonitoring();

            System.out.println("Mapa carregado com " + mapData.getNodes().size() + " nós e " + mapData.getEdges().size() + " arestas");
            return true;

        } catch (IOException e) {
            System.err.println("Erro ao carregar dados do mapa: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Erro inesperado ao carregar dados do mapa: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Método para inicializar monitoramento de congestionamento
    private void initializeCongestionMonitoring() {
        if (mapData == null || mapData.getNodes() == null) return;

        for (Node node : mapData.getNodes()) {
            double x = convertLonToScreen(node.getLongitude());
            double y = convertLatToScreen(node.getLatitude());
            congestionMonitor.addTrafficLight(node.getId(), x, y);
        }

        System.out.println("Sistema de monitoramento de congestionamento inicializado com " +
                congestionMonitor.getActiveLights() + " semáforos");
    }

    // Método para atualizar congestionamento em tempo real
    private void updateCongestionData() {
        if (mapData == null || mapData.getNodes() == null) return;

        // Array para contar carros por nó
        int[] carsPerNode = new int[mapData.getNodes().size()];

        // Mapear IDs de nós para índices
        for (int i = 0; i < mapData.getNodes().size(); i++) {
            String nodeId = mapData.getNodes().get(i).getId();

            // Contar carros neste nó
            int carsAtNode = 0;
            for (Car car : cars) {
                if (car.getCurrentNodeId().equals(nodeId)) {
                    carsAtNode++;
                }
            }

            // Atualizar monitor de congestionamento
            TrafficLight.State lightState = mapData.getNodes().get(i).getTrafficLight().getCurrentState();
            congestionMonitor.updateTrafficLight(nodeId, carsAtNode, lightState);
        }
    }

    private void startDisplayUpdate() {
        displayUpdateTimer = new Timeline(new KeyFrame(Duration.seconds(0.1), e -> {
            // Atualizar dados de congestionamento
            updateCongestionData();
        }));
        displayUpdateTimer.setCycleCount(Timeline.INDEFINITE);
        displayUpdateTimer.play();
    }

    public void startSimulation() {
        simulationStartTime = System.currentTimeMillis();
        startDisplayUpdate();

        if (!isMapLoaded()) {
            System.err.println("Mapa não foi carregado. Não é possível iniciar a simulação.");
            return;
        }

        if (simulationRunning) {
            System.out.println("Simulação já está rodando.");
            return;
        }

        simulationRunning = true;
        totalArrivedCarsCount = 0;
        currentArrivedCarsCount = 0;

        // Resetar monitor de congestionamento
        congestionMonitor.reset();
        initializeCongestionMonitoring();

        // stopSimulation() para os semáforos; reinicia-los para que voltem a ciclar
        for (Node node : mapData.getNodes()) {
            node.getTrafficLight().start();
        }

        startCarSpawning();
        startCarMovement();
        startCarRemoval();
        System.out.println("Simulação iniciada com sistema de monitoramento de congestionamento!");
    }

    public String getSimulationTimeString() {
        if (!simulationRunning) return "00:00";

        long elapsedMs = System.currentTimeMillis() - simulationStartTime;
        long seconds = elapsedMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    public void stopSimulation() {
        simulationRunning = false;
        stop();
        System.out.println("Simulação parada!");

        if (displayUpdateTimer != null) {
            displayUpdateTimer.stop();
        }
    }

    public void resetSimulation() {
        stopSimulation();
        cars.clear();
        totalArrivedCarsCount = 0;
        currentArrivedCarsCount = 0;

        // Resetar monitor de congestionamento
        congestionMonitor.reset();
        initializeCongestionMonitoring();

        System.out.println("Simulação resetada!");
    }

    private void startCarSpawning() {
        carSpawnTimer = new Timeline(new KeyFrame(Duration.seconds(1.0 / CAR_SPAWN_RATE), e -> spawnCar()));
        carSpawnTimer.setCycleCount(Timeline.INDEFINITE);
        carSpawnTimer.play();
    }

    private void startCarMovement() {
        carMovementTimer = new Timeline(new KeyFrame(Duration.seconds(0.1), e -> {
            updateCarMovements();
            updateArrivedCarsCount();
        }));
        carMovementTimer.setCycleCount(Timeline.INDEFINITE);
        carMovementTimer.play();
    }

    private void startCarRemoval() {
        carRemovalTimer = new Timeline(new KeyFrame(Duration.seconds(CAR_REMOVAL_DELAY), e -> removeArrivedCars()));
        carRemovalTimer.setCycleCount(Timeline.INDEFINITE);
        carRemovalTimer.play();
    }

    private void removeArrivedCars() {
        List<Car> carsToRemove = new ArrayList<>();

        for (Car car : cars) {
            if (car.hasArrived()) {
                if (!car.isMoving() && !car.isWaitingAtTrafficLight()) {
                    carsToRemove.add(car);
                    System.out.println("Removendo carro que chegou ao destino no nó: " + car.getCurrentNodeId());
                }
            }
        }

        for (Car car : carsToRemove) {
            car.stop();
            cars.remove(car);
            totalArrivedCarsCount++;
        }

        if (!carsToRemove.isEmpty()) {
            System.out.println("Removidos " + carsToRemove.size() + " carros. Total de carros que chegaram: " + totalArrivedCarsCount);
            System.out.println("Carros ativos no mapa: " + cars.size());
        }
    }

    private void spawnCar() {
        if (mapData == null || mapData.getNodes() == null || mapData.getNodes().isEmpty()) {
            System.err.println("MapData não disponível para spawn de carros");
            return;
        }

        if (cars.size() >= MAX_CARS) {
            System.out.println("Máximo de carros ativos atingido: " + MAX_CARS + " (Total chegaram: " + totalArrivedCarsCount + ")");
            return;
        }

        Node spawnNode = null;
        int attempts = 0;
        while (spawnNode == null && attempts < 20) {
            Node candidateNode = mapData.getNodes().get(random.nextInt(mapData.getNodes().size()));
            List<Edge> outgoingEdges = mapData.getEdgesFromNode(candidateNode.getId());

            if (!outgoingEdges.isEmpty()) {
                if (mapData != null) {
                    double nodeX = convertLonToScreen(candidateNode.getLongitude());
                    double nodeY = convertLatToScreen(candidateNode.getLatitude());

                    if (!isAreaCongested(nodeX, nodeY)) {
                        spawnNode = candidateNode;
                    }
                }
            }
            attempts++;
        }

        if (spawnNode == null) {
            System.err.println("Não foi possível encontrar um nó válido para spawn após " + attempts + " tentativas");
            return;
        }

        createCar(spawnNode.getId());
    }

    public void createCar(String startNodeId) {
        if (mapData == null) {
            System.err.println("ERRO: mapData é null - não é possível criar carro");
            return;
        }

        Car car = new Car(startNodeId);
        Node startNode = mapData.getNodeById(startNodeId);

        if (startNode != null) {
            car.setInitialPosition(startNode,
                    mapData.getMinLatitude(),
                    mapData.getMaxLatitude(),
                    mapData.getMinLongitude(),
                    mapData.getMaxLongitude(),
                    mapData);
        } else {
            System.err.println("ERRO: startNode não encontrado para ID: " + startNodeId);
            return;
        }

        cars.add(car);
        System.out.println("Carro " + (totalArrivedCarsCount + cars.size()) + " gerado no nó: " + startNodeId +
                " na posição (" + String.format("%.2f", car.getX()) + ", " + String.format("%.2f", car.getY()) + ")");
    }

    private double convertLonToScreen(double longitude) {
        double minLon = mapData.getMinLongitude();
        double maxLon = mapData.getMaxLongitude();
        double lonRange = maxLon - minLon;
        double latRange = mapData.getMaxLatitude() - mapData.getMinLatitude();

        double scaleX = 800.0 / lonRange;
        double scaleY = 600.0 / latRange;
        double scale = Math.min(scaleX, scaleY);

        double mapWidthUsed = lonRange * scale;
        double offsetX = (800.0 - mapWidthUsed) / 2 + 10;

        return offsetX + ((longitude - minLon) / lonRange) * mapWidthUsed;
    }

    private double convertLatToScreen(double latitude) {
        double minLat = mapData.getMinLatitude();
        double maxLat = mapData.getMaxLatitude();
        double latRange = maxLat - minLat;
        double lonRange = mapData.getMaxLongitude() - mapData.getMinLongitude();

        double scaleX = 800.0 / lonRange;
        double scaleY = 600.0 / latRange;
        double scale = Math.min(scaleX, scaleY);

        double mapHeightUsed = latRange * scale;
        double offsetY = (600.0 - mapHeightUsed) / 2 + 10;

        return offsetY + mapHeightUsed - ((latitude - minLat) / latRange) * mapHeightUsed;
    }

    private boolean isAreaCongested(double x, double y) {
        int carsInArea = 0;
        double checkRadius = 2.0;

        for (Car car : cars) {
            double distance = Math.sqrt(Math.pow(car.getX() - x, 2) + Math.pow(car.getY() - y, 2));
            if (distance <= checkRadius) {
                carsInArea++;
            }
        }

        return carsInArea >= 3;
    }

    private void updateCarMovements() {
        if (mapData == null) {
            System.err.println("MapData não disponível para movimento de carros");
            return;
        }

        List<Car> carsToRemove = new ArrayList<>();

        for (Car car : cars) {
            if (car.hasArrived()) {
                continue;
            }

            if (!car.isBusy()) {
                Node currentNode = mapData.getNodeById(car.getCurrentNodeId());
                if (currentNode == null) {
                    System.err.println("Nó atual não encontrado para o carro: " + car.getCurrentNodeId());
                    carsToRemove.add(car);
                    continue;
                }

                Edge selectedEdge = chooseNextEdge(car, currentNode);
                if (selectedEdge == null) {
                    System.out.println("Carro chegou ao destino final (nó sem saída): " + currentNode.getId());
                    continue;
                }

                Node targetNode = mapData.getNodeById(selectedEdge.getTarget());

                if (targetNode != null) {
                    if (canCarMoveTo(car, currentNode, targetNode)) {
                        System.out.println("Carro tentando mover de " + currentNode.getId() + " para " + targetNode.getId());
                        car.moveTo(targetNode, currentNode, selectedEdge, mapData);
                        // Só avança a rota se o carro realmente começou (ou aguarda) esse trecho
                        if (car.isBusy()) {
                            car.advanceRoute();
                        }
                    } else {
                        System.out.println("Carro aguardando - caminho bloqueado para " + targetNode.getId());
                    }
                } else {
                    System.err.println("Nó de destino não encontrado: " + selectedEdge.getTarget());
                    carsToRemove.add(car);
                }
            }
        }

        for (Car car : carsToRemove) {
            car.stop();
            cars.remove(car);
        }
    }

    // Próximo trecho do carro: o da rota Dijkstra; sem rota válida, um trecho aleatório
    private Edge chooseNextEdge(Car car, Node currentNode) {
        Edge planned = car.peekNextEdge();
        if (planned == null || !planned.getSource().equals(currentNode.getId())) {
            planRoute(car, currentNode);
            planned = car.peekNextEdge();
        }
        if (planned != null) {
            return planned;
        }

        List<Edge> outgoingEdges = mapData.getEdgesFromNode(currentNode.getId());
        return outgoingEdges.isEmpty() ? null : outgoingEdges.get(random.nextInt(outgoingEdges.size()));
    }

    // Sorteia destinos alcançáveis e guarda o caminho mínimo até o primeiro que funcionar
    private void planRoute(Car car, Node currentNode) {
        car.setRoute(List.of());
        List<Node> nodes = mapData.getNodes();

        for (int attempt = 0; attempt < ROUTE_ATTEMPTS; attempt++) {
            Node destination = nodes.get(random.nextInt(nodes.size()));
            if (destination.getId().equals(currentNode.getId())) {
                continue;
            }

            List<Edge> path = routePlanner.shortestPath(currentNode.getId(), destination.getId());
            if (!path.isEmpty()) {
                car.setRoute(path);
                return;
            }
        }
    }

    private boolean canCarMoveTo(Car car, Node currentNode, Node targetNode) {
        if (targetNode.getTrafficLight().isRedFor(TrafficLight.approachOf(currentNode, targetNode))) {
            return false;
        }

        double targetX = convertLonToScreen(targetNode.getLongitude());
        double targetY = convertLatToScreen(targetNode.getLatitude());

        for (Car otherCar : cars) {
            if (otherCar == car) continue;

            double distance = Math.sqrt(Math.pow(otherCar.getX() - targetX, 2) + Math.pow(otherCar.getY() - targetY, 2));
            if (distance < MIN_CAR_DISTANCE) {
                return false;
            }

            if (otherCar.getCurrentNodeId().equals(targetNode.getId()) && !otherCar.isMoving()) {
                return false;
            }
        }

        return true;
    }

    public boolean checkCollisionForCar(Car car, double checkX, double checkY) {
        for (Car otherCar : cars) {
            if (otherCar == car) continue;

            double distance = Math.sqrt(Math.pow(otherCar.getX() - checkX, 2) + Math.pow(otherCar.getY() - checkY, 2));
            if (distance < MIN_CAR_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    private void updateArrivedCarsCount() {
        int currentCount = 0;
        for (Car car : cars) {
            if (car.hasArrived()) {
                currentCount++;
            }
        }
        currentArrivedCarsCount = currentCount;
    }

    public void stop() {
        if (carSpawnTimer != null) {
            carSpawnTimer.stop();
        }
        if (carMovementTimer != null) {
            carMovementTimer.stop();
        }
        if (carRemovalTimer != null) {
            carRemovalTimer.stop();
        }

        for (Car car : cars) {
            car.stop();
        }

        if (mapData != null && mapData.getNodes() != null) {
            for (Node node : mapData.getNodes()) {
                if (node.getTrafficLight() != null) {
                    node.getTrafficLight().stop();
                }
            }
        }
    }

    // Getters para sistema de congestionamento
    public CongestionMonitor getCongestionMonitor() {
        return congestionMonitor;
    }

    public CongestionMonitor.CongestionData getCongestionData() {
        return congestionMonitor.getCongestionData();
    }

    // Getters existentes
    public int getArrivedCarsCount() {
        return totalArrivedCarsCount;
    }

    public int getCurrentArrivedCarsCount() {
        return currentArrivedCarsCount;
    }

    public int getTotalCarsProcessed() {
        return totalArrivedCarsCount + cars.size();
    }

    public MapData getMapData() {
        return mapData;
    }

    public List<Car> getCars() {
        return cars;
    }

    public boolean isMapLoaded() {
        return mapData != null && mapData.getNodes() != null && !mapData.getNodes().isEmpty();
    }

    public boolean isSimulationRunning() {
        return simulationRunning;
    }
}