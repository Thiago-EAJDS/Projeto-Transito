package org.mapasimulador.mapasimulador.view;

import org.mapasimulador.mapasimulador.controller.MapController;
import org.mapasimulador.mapasimulador.model.*;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.animation.FillTransition;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.Map;

public class MapView {
    private Stage stage;
    private Scene scene;
    private Group mapGroup;
    private Group carsGroup;
    private Group trafficLightsGroup;
    private Label simulationTimer;
    private MapController controller;
    private Timeline updateTimer;
    private Map<String, Circle> trafficLightCircles;
    private Label carCountLabel; // Contador de carros
    private Label movingCarsLabel; // Contador de carros que movem
    private Label waitingCarsLabel; // Contador de carros que esperam
    private Label arrivedCarsLabel; // Contador de carros que chegaram
    private Label simulationStatusLabel;
    private Button startButton;
    private Button stopButton;
    private Button resetButton;
    private Group congestionGroup;
    private Label congestionLevelLabel;
    private Label highCongestionLightsLabel;

    // Ciclo de Dia e Noite
    private boolean isDarkMode = false;
    private Timeline dayNightCycle;
    private Group cityLightsGroup;

    // Variáveis para controlar o offset do mapa
    private double mapOffsetX = 10;
    private double mapOffsetY = 10;

    private static final double SCENE_WIDTH = 1000;
    private static final double SCENE_HEIGHT = 700;
    private static final double MAP_WIDTH = 800;
    private static final double MAP_HEIGHT = 600;

    private static final double CONGESTION_BAR_WIDTH = 60.0;
    private static final double CONGESTION_BAR_HEIGHT = 6.0;
    private static final Font CONGESTION_FONT = Font.font("Arial", FontWeight.BOLD, 8);

    public MapView(Stage stage) {
        this.stage = stage;
        this.controller = new MapController();
        this.trafficLightCircles = new HashMap<>();
        setupUI();
        startUpdateTimer();

        // Verificar se o mapa foi carregado
        if (!controller.isMapLoaded()) {
            System.err.println("ERRO: Mapa não foi carregado no MapView!");
        } else {
            System.out.println("MapView inicializada com sucesso!");
            System.out.println("Nós: " + controller.getMapData().getNodes().size());
            System.out.println("Arestas: " + controller.getMapData().getEdges().size());
        }
    }

    private void setupUI() {
        // Grupos para organizar elementos
        mapGroup = new Group();
        carsGroup = new Group();
        trafficLightsGroup = new Group();
        congestionGroup = new Group();

        // Criar painel de controle elegante
        VBox controlPanel = createControlPanel();

        // Criar painel do mapa com bordas elegantes
        VBox mapPanel = createMapPanel();

        // Layout principal com gradiente
        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(controlPanel);
        mainLayout.setCenter(mapPanel);

        // Aplicar gradiente de fundo
        LinearGradient backgroundGradient = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f0f4f8")),
                new Stop(1, Color.web("#d6e8f5"))
        );
        mainLayout.setBackground(new Background(new BackgroundFill(backgroundGradient, null, null)));

        // Configurar cena
        scene = new Scene(mainLayout, SCENE_WIDTH, SCENE_HEIGHT);
        stage.setScene(scene);
        stage.setTitle("🚗 Simulador de Tráfego - Teresina, PI");

        // Desenhar mapa inicial
        drawMap();
    }

    private VBox createControlPanel() {
        VBox controlPanel = new VBox(15);
        controlPanel.setPadding(new Insets(20));
        controlPanel.setAlignment(Pos.CENTER);

        // Gradiente para o painel de controle
        LinearGradient controlGradient = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#2c3e50")),
                new Stop(1, Color.web("#34495e"))
        );
        controlPanel.setBackground(new Background(new BackgroundFill(controlGradient, new CornerRadii(10), null)));

        // Efeito de sombra
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#00000030"));
        shadow.setOffsetY(5);
        shadow.setRadius(10);
        controlPanel.setEffect(shadow);

        // Título
        Label titleLabel = new Label("🚦 CONTROLE DA SIMULAÇÃO");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Container dos botões
        HBox buttonContainer = new HBox(15);
        buttonContainer.setAlignment(Pos.CENTER);

        // Controles da interface com estilo melhorado
        startButton = createStyledButton("▶ Iniciar Simulação", "#27ae60", "#2ecc71");
        stopButton = createStyledButton("⏸ Parar Simulação", "#e74c3c", "#c0392b");
        resetButton = createStyledButton("🔄 Resetar", "#f39c12", "#e67e22");

        // Configurar estado inicial dos botões
        stopButton.setDisable(true);

        startButton.setOnAction(e -> startSimulation());
        stopButton.setOnAction(e -> stopSimulation());
        resetButton.setOnAction(e -> resetSimulation());

        buttonContainer.getChildren().addAll(startButton, stopButton, resetButton);

        // Separador
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #7f8c8d;");

        // Painel de estatísticas
        HBox statsContainer = createStatsPanel();

        controlPanel.getChildren().addAll(titleLabel, buttonContainer, separator, statsContainer);
        return controlPanel;
    }

    private Button createStyledButton(String text, String baseColor, String hoverColor) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + baseColor + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 20; " +
                        "-fx-background-radius: 25; " +
                        "-fx-cursor: hand;"
        );

        // Efeitos hover
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: " + hoverColor + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 20; " +
                        "-fx-background-radius: 25; " +
                        "-fx-cursor: hand; " +
                        "-fx-scale-x: 1.05; " +
                        "-fx-scale-y: 1.05;"
        ));

        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: " + baseColor + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 20; " +
                        "-fx-background-radius: 25; " +
                        "-fx-cursor: hand;"
        ));

        return button;
    }

    private HBox createStatsPanel() {
        HBox statsContainer = new HBox(20); // Reduzido o espaçamento para comportar mais labels
        statsContainer.setAlignment(Pos.CENTER);

        // Labels de estatísticas com estilo - ADICIONADO o contador de carros que chegaram
        carCountLabel = createStatsLabel("🚗 Carros: 0");
        movingCarsLabel = createStatsLabel("🟢 Em movimento: 0");
        waitingCarsLabel = createStatsLabel("🟠 Esperando: 0");
        arrivedCarsLabel = createStatsLabel("🏁 Chegaram: 0");
        simulationStatusLabel = createStatsLabel("⏹ Status: Parada");
        congestionLevelLabel = createStatsLabel("📊 Congestionamento: 0%");
        highCongestionLightsLabel = createStatsLabel("🔴 Alto: 0");

        // Criar o timer antes de adicionar
        simulationTimer = createStatsLabel("⏱ Tempo: 00:00");
        statsContainer.getChildren().addAll(simulationTimer, carCountLabel, movingCarsLabel,
                waitingCarsLabel, arrivedCarsLabel, congestionLevelLabel, highCongestionLightsLabel, simulationStatusLabel);
        return statsContainer;
    }

    private Label createStatsLabel(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-color: #34495e; " +
                        "-fx-padding: 5 10; " +
                        "-fx-background-radius: 15;"
        );
        return label;
    }

    private void addCityLights() {
        cityLightsGroup.getChildren().clear();
        if (controller.getMapData() == null) return;

        for (Node node : controller.getMapData().getNodes()) {
            double x = convertLonToScreen(node.getLongitude(),
                    controller.getMapData().getMinLongitude(),
                    controller.getMapData().getMaxLongitude());
            double y = convertLatToScreen(node.getLatitude(),
                    controller.getMapData().getMinLatitude(),
                    controller.getMapData().getMaxLatitude());

            Circle light = new Circle(x, y, 2);
            light.setFill(Color.web("#f1c40f")); // Amarelo para luzes
            light.setOpacity(0.0); // Iniciar invisível para fade-in
            DropShadow glow = new DropShadow();
            glow.setRadius(6);
            glow.setColor(Color.web("#f1c40f80"));
            light.setEffect(glow);
            cityLightsGroup.getChildren().add(light);

            // Adicionar transição de fade-in para cada luz
            FadeTransition lightFade = new FadeTransition(Duration.seconds(2), light);
            lightFade.setToValue(1.0);
            lightFade.play();
        }
    }

    private void startDayNightCycle(Rectangle mapBackground) {
        dayNightCycle = new Timeline(
                new KeyFrame(Duration.seconds(30), e -> {
                    isDarkMode = !isDarkMode;
                    FillTransition backgroundTransition = new FillTransition(Duration.seconds(2), mapBackground);
                    if (isDarkMode) {
                        // Modo noturno
                        backgroundTransition.setToValue(Color.web("#2c3e50"));
                        addCityLights();
                    } else {
                        // Modo diurno
                        backgroundTransition.setToValue(Color.web("#ecf0f1"));
                        // Transição de fade-out para luzes
                        FadeTransition lightsFade = new FadeTransition(Duration.seconds(2), cityLightsGroup);
                        lightsFade.setToValue(0.0);
                        lightsFade.setOnFinished(event -> cityLightsGroup.getChildren().clear());
                        lightsFade.play();
                    }
                    backgroundTransition.play();
                })
        );
        dayNightCycle.setCycleCount(Timeline.INDEFINITE);
        dayNightCycle.play();
    }

    private VBox createMapPanel() {
        VBox mapPanel = new VBox();
        mapPanel.setPadding(new Insets(10));
        mapPanel.setAlignment(Pos.CENTER);

        StackPane mapContainer = new StackPane();
        mapContainer.setPrefSize(MAP_WIDTH + 20, MAP_HEIGHT + 20);
        mapContainer.setMaxSize(MAP_WIDTH + 20, MAP_HEIGHT + 20);

        Rectangle mapBackground = new Rectangle(MAP_WIDTH + 20, MAP_HEIGHT + 20);
        mapBackground.setFill(Color.web("#ecf0f1")); // Cor inicial (dia)
        mapBackground.setStroke(Color.web("#bdc3c7"));
        mapBackground.setStrokeWidth(2);
        mapBackground.setArcWidth(15);
        mapBackground.setArcHeight(15);

        DropShadow mapShadow = new DropShadow();
        mapShadow.setColor(Color.web("#00000020"));
        mapShadow.setOffsetY(5);
        mapShadow.setRadius(15);
        mapBackground.setEffect(mapShadow);

        // Inicializar grupo para luzes da cidade
        cityLightsGroup = new Group();

        mapGroup.getChildren().addAll(trafficLightsGroup, carsGroup, cityLightsGroup, congestionGroup);

        mapContainer.getChildren().addAll(mapBackground, mapGroup);
        mapPanel.getChildren().add(mapContainer);

        // Iniciar ciclo de dia e noite
        startDayNightCycle(mapBackground);

        return mapPanel;
    }

    private void renderCongestionOverlay() {
        congestionGroup.getChildren().clear();

        if (!controller.isSimulationRunning()) return;

        MapController.CongestionMonitor.CongestionData data = controller.getCongestionData();
        MapController.CongestionMonitor.TrafficLightCongestion[] lights = data.getLights();

        for (int i = 0; i < data.getLightCount(); i++) {
            MapController.CongestionMonitor.TrafficLightCongestion light = lights[i];

            if (light.getCarsWaiting() > 0) {
                renderCongestionBar(light);
                renderCarCount(light);
            }
        }
    }

    private void renderCongestionBar(MapController.CongestionMonitor.TrafficLightCongestion light) {
        double x = light.getX();
        double y = light.getY() - 20; // Posicionar acima do semáforo

        // Fundo da barra (cinza)
        Rectangle background = new Rectangle(x - CONGESTION_BAR_WIDTH/2, y, CONGESTION_BAR_WIDTH, CONGESTION_BAR_HEIGHT);
        background.setFill(Color.LIGHTGRAY);
        background.setStroke(Color.BLACK);
        background.setStrokeWidth(0.5);

        // Barra de progresso (colorida)
        double fillWidth = (light.getCongestionPercentage() / 100.0) * CONGESTION_BAR_WIDTH;
        Rectangle progressBar = new Rectangle(x - CONGESTION_BAR_WIDTH/2, y, fillWidth, CONGESTION_BAR_HEIGHT);
        progressBar.setFill(Color.web(light.getCongestionColor()));

        congestionGroup.getChildren().addAll(background, progressBar);
    }

    private void renderCarCount(MapController.CongestionMonitor.TrafficLightCongestion light) {
        double x = light.getX();
        double y = light.getY() + 15; // Posicionar abaixo do semáforo

        Label carCountLabel = new Label("🚗" + light.getCarsWaiting());
        carCountLabel.setFont(CONGESTION_FONT);
        carCountLabel.setTextFill(Color.DARKBLUE);
        carCountLabel.setLayoutX(x - 10);
        carCountLabel.setLayoutY(y);

        congestionGroup.getChildren().add(carCountLabel);
    }

    private void drawMap() {
        if (controller.getMapData() == null) {
            System.err.println("MapData é null - não é possível desenhar o mapa");
            return;
        }

        MapData mapData = controller.getMapData();

        // Calcular limites do mapa
        double minLat = mapData.getMinLatitude();
        double maxLat = mapData.getMaxLatitude();
        double minLon = mapData.getMinLongitude();
        double maxLon = mapData.getMaxLongitude();

        System.out.println("Desenhando mapa:");
        System.out.println("Limites do mapa:");
        System.out.println("Lat: " + minLat + " a " + maxLat);
        System.out.println("Lon: " + minLon + " a " + maxLon);

        // CORREÇÃO: Calcular o offset necessário para centralizar o mapa
        double latRange = maxLat - minLat;
        double lonRange = maxLon - minLon;

        // Calcular escala para ambos os eixos
        double scaleX = MAP_WIDTH / lonRange;
        double scaleY = MAP_HEIGHT / latRange;

        // Usar a menor escala para manter proporções
        double scale = Math.min(scaleX, scaleY);

        // Calcular offset para centralizar
        double mapWidthUsed = lonRange * scale;
        double mapHeightUsed = latRange * scale;

        mapOffsetX = (MAP_WIDTH - mapWidthUsed) / 2 + 10; // +10 para a borda
        mapOffsetY = (MAP_HEIGHT - mapHeightUsed) / 2 + 10; // +10 para a borda

        System.out.println("Offset calculado: X=" + mapOffsetX + ", Y=" + mapOffsetY);
        System.out.println("Escala: " + scale);

        // Desenhar arestas (ruas) com estilo melhorado
        System.out.println("Desenhando " + mapData.getEdges().size() + " arestas...");
        for (Edge edge : mapData.getEdges()) {
            Node sourceNode = mapData.getNodeById(edge.getSource());
            Node targetNode = mapData.getNodeById(edge.getTarget());

            if (sourceNode != null && targetNode != null) {
                // Usar o novo método de conversão com offset e escala
                double x1 = convertLonToScreen(sourceNode.getLongitude(), minLon, maxLon);
                double y1 = convertLatToScreen(sourceNode.getLatitude(), minLat, maxLat);
                double x2 = convertLonToScreen(targetNode.getLongitude(), minLon, maxLon);
                double y2 = convertLatToScreen(targetNode.getLatitude(), minLat, maxLat);

                Line road = new Line(x1, y1, x2, y2);
                road.setStroke(Color.web("#7f8c8d"));
                road.setStrokeWidth(2);

                mapGroup.getChildren().add(road);
            }
        }

        // Desenhar nós e semáforos com estilo melhorado
        System.out.println("Desenhando " + mapData.getNodes().size() + " nós...");
        for (Node node : mapData.getNodes()) {
            // Usar o novo método de conversão com offset e escala
            double x = convertLonToScreen(node.getLongitude(), minLon, maxLon);
            double y = convertLatToScreen(node.getLatitude(), minLat, maxLat);

            // Círculo do nó
            Circle nodeCircle = new Circle(x, y, 3);
            nodeCircle.setFill(Color.web("#2c3e50"));
            nodeCircle.setStroke(Color.web("#34495e"));
            mapGroup.getChildren().add(nodeCircle);

            // Semáforo com estilo melhorado
            Circle trafficLight = new Circle(x, y, 5);
            trafficLight.setStroke(Color.web("#2c3e50"));
            trafficLight.setStrokeWidth(2);
            updateTrafficLightColor(trafficLight, node.getTrafficLight());

            // Efeito de brilho para semáforos
            DropShadow lightGlow = new DropShadow();
            lightGlow.setRadius(8);
            lightGlow.setColor(node.getTrafficLight().isGreen() ? Color.web("#27ae60") : Color.web("#e74c3c"));
            trafficLight.setEffect(lightGlow);

            trafficLightsGroup.getChildren().add(trafficLight);
            trafficLightCircles.put(node.getId(), trafficLight);
        }

        System.out.println("Mapa desenhado com sucesso!");
    }

    // Conversão de coordenadas com offset e escala corrigidos
    private double convertLonToScreen(double longitude, double minLon, double maxLon) {
        double lonRange = maxLon - minLon;
        double latRange = controller.getMapData().getMaxLatitude() - controller.getMapData().getMinLatitude();

        double scaleX = MAP_WIDTH / lonRange;
        double scaleY = MAP_HEIGHT / latRange;
        double scale = Math.min(scaleX, scaleY); // Escala que preserva proporção

        double mapWidthUsed = lonRange * scale;
        double offsetX = (MAP_WIDTH - mapWidthUsed) / 2 + 10; // Centralizar

        return offsetX + ((longitude - minLon) * scale);
    }

    private double convertLatToScreen(double latitude, double minLat, double maxLat) {
        double lonRange = controller.getMapData().getMaxLongitude() - controller.getMapData().getMinLongitude();
        double latRange = maxLat - minLat;

        double scaleX = MAP_WIDTH / lonRange;
        double scaleY = MAP_HEIGHT / latRange;
        double scale = Math.min(scaleX, scaleY); // Escala que preserva proporção

        double mapHeightUsed = latRange * scale;
        double offsetY = (MAP_HEIGHT - mapHeightUsed) / 2 + 10; // Centralizar

        return offsetY + ((maxLat - latitude) * scale);
    }

    private void updateTrafficLightColor(Circle trafficLight, TrafficLight light) {
        if (light.isGreen()) {
            trafficLight.setFill(Color.web("#27ae60"));
            // Atualizar efeito de brilho
            DropShadow greenGlow = new DropShadow();
            greenGlow.setRadius(8);
            greenGlow.setColor(Color.web("#27ae60"));
            trafficLight.setEffect(greenGlow);
        } else {
            trafficLight.setFill(Color.web("#e74c3c"));
            // Atualizar efeito de brilho
            DropShadow redGlow = new DropShadow();
            redGlow.setRadius(8);
            redGlow.setColor(Color.web("#e74c3c"));
            trafficLight.setEffect(redGlow);
        }
    }

    private void startUpdateTimer() {
        updateTimer = new Timeline(new KeyFrame(Duration.millis(100), e -> updateDisplay()));
        updateTimer.setCycleCount(Timeline.INDEFINITE);
        updateTimer.play();
    }

    private void updateDisplay() {
        // Sempre atualizar semáforos
        if (controller.getMapData() != null) {
            for (Node node : controller.getMapData().getNodes()) {
                Circle trafficLightCircle = trafficLightCircles.get(node.getId());
                if (trafficLightCircle != null) {
                    updateTrafficLightColor(trafficLightCircle, node.getTrafficLight());
                }
            }
        }

        // Sempre atualizar carros na tela
        carsGroup.getChildren().clear();
        for (Car car : controller.getCars()) {
            carsGroup.getChildren().add(car.getCarShape());
        }

        // Atualizar estatísticas
        updateStatistics();
        renderCongestionOverlay();
    }

    private void updateStatistics() {
        int totalCars = controller.getCars().size();
        int movingCars = 0;
        int waitingCars = 0;
        int arrivedCars = 0;

        for (Car car : controller.getCars()) {
            if (car.isMoving()) {
                movingCars++;
            } else if (car.isWaitingAtTrafficLight()) {
                waitingCars++;
            }
        }

        // Obter contador de carros que chegaram do controller
        if (controller != null) {
            arrivedCars = controller.getArrivedCarsCount(); // Este método precisa ser implementado no MapController
        }

        carCountLabel.setText("🚗 Carros: " + totalCars);
        movingCarsLabel.setText("🟢 Em movimento: " + movingCars);
        waitingCarsLabel.setText("🟠 Esperando: " + waitingCars);
        arrivedCarsLabel.setText("🏁 Chegaram: " + arrivedCars);

        MapController.CongestionMonitor.CongestionData congestionData = controller.getCongestionData();
        congestionLevelLabel.setText(String.format("📊 Congestionamento: %.1f%% (%s)",
                congestionData.getOverallCongestion(), congestionData.getOverallCongestionLevel()));
        highCongestionLightsLabel.setText("🔴 Alto: " + controller.getCongestionMonitor().getHighCongestionLights());

        String congestionStyle = "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-background-color: " + congestionData.getOverallCongestionColor() + "; " +
                "-fx-padding: 5 10; -fx-background-radius: 15;";
        congestionLevelLabel.setStyle(congestionStyle);

        if (controller.isSimulationRunning()) {
            simulationStatusLabel.setText("▶ Status: Rodando");
            simulationStatusLabel.setStyle(
                    "-fx-text-fill: white; " +
                            "-fx-font-size: 12px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-color: #27ae60; " +
                            "-fx-padding: 5 10; " +
                            "-fx-background-radius: 15;"
            );
        } else {
            simulationStatusLabel.setText("⏹ Status: Parada");
            simulationStatusLabel.setStyle(
                    "-fx-text-fill: white; " +
                            "-fx-font-size: 12px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-color: #e74c3c; " +
                            "-fx-padding: 5 10; " +
                            "-fx-background-radius: 15;"
            );
        }
        if (controller.isSimulationRunning()) {
            simulationTimer.setText("⏱ Tempo: " + controller.getSimulationTimeString());
        } else {
            simulationTimer.setText("⏱ Tempo: 00:00");
        }
    }

    private void startSimulation() {
        if (!controller.isMapLoaded()) {
            System.err.println("ERRO: Não é possível iniciar simulação - mapa não carregado!");
            return;
        }

        if (controller.isSimulationRunning()) {
            System.out.println("Simulação já está rodando!");
            return;
        }

        System.out.println("=== INICIANDO SIMULAÇÃO ===");

        // Chamar o método do controller
        controller.startSimulation();

        // Atualizar interface
        startButton.setDisable(true);
        stopButton.setDisable(false);

        System.out.println("Simulação iniciada com sucesso!");
    }

    private void stopSimulation() {
        System.out.println("=== PARANDO SIMULAÇÃO ===");

        // Parar a simulação no controller
        controller.stopSimulation();

        // Limpar carros da tela
        carsGroup.getChildren().clear();

        // Atualizar interface
        startButton.setDisable(false);
        stopButton.setDisable(true);

        System.out.println("Simulação parada!");
    }

    private void resetSimulation() {
        System.out.println("=== RESETANDO SIMULAÇÃO ===");

        // Parar simulação atual
        stopSimulation();

        // Limpar carros da tela
        carsGroup.getChildren().clear();

        // Reinicializar controller
        controller = new MapController();

        // Redesenhar mapa
        mapGroup.getChildren().clear();
        trafficLightsGroup.getChildren().clear();
        trafficLightCircles.clear();

        // Readicionar grupos
        mapGroup.getChildren().addAll(trafficLightsGroup, carsGroup);
        drawMap();

        System.out.println("Simulação resetada!");
    }

    // MÉTODOS PÚBLICOS para permitir que o Carro (Car) acesse as conversões de coordenadas
    public double getScreenX(double longitude) {
        if (controller.getMapData() != null) {
            return convertLonToScreen(longitude,
                    controller.getMapData().getMinLongitude(),
                    controller.getMapData().getMaxLongitude());
        }
        return 0;
    }

    public double getScreenY(double latitude) {
        if (controller.getMapData() != null) {
            return convertLatToScreen(latitude,
                    controller.getMapData().getMinLatitude(),
                    controller.getMapData().getMaxLatitude());
        }
        return 0;
    }

    public void show() {
        stage.show();
    }

    public void close() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        if (dayNightCycle != null) {
            dayNightCycle.stop();
        }
        controller.stopSimulation();
        stage.close();
    }
}