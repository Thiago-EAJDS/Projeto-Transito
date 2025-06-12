package org.mapasimulador.mapasimulador.model;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.util.Random;

public class Car {
    private String currentNodeId;
    private double x;
    private double y;
    private boolean moving;
    private boolean waitingAtTrafficLight;
    private Timeline movementTimeline;
    private Timeline trafficLightCheckTimer;
    private double speed; // pixels por segundo
    private Node targetNode;
    private Node currentNode;
    private Edge currentEdge;
    private MapData mapData;
    private boolean hasPassedTrafficLight; // Flag para indicar se já passou do semáforo
    private boolean hasArrived; // Flag para indicar se chegou ao destino
    private int movementCount; // Contador de movimentos
    private int maxMovements; // Máximo de movimentos antes de considerar chegada
    private static final Random random = new Random();

    // Constantes para detecção de colisão
    private static final double CAR_SIZE = 6.0; // Tamanho do carro para colisão
    private static final double SAFE_DISTANCE = 6.5; // Distância segura entre carros

    public Car(String startNodeId) {
        this.currentNodeId = startNodeId;
        this.moving = false;
        this.waitingAtTrafficLight = false;
        this.hasPassedTrafficLight = false;
        this.hasArrived = false;
        this.speed = 100.0; // velocidade padrão
        this.movementCount = 0; // Inicializar contador
        this.maxMovements = 5 + random.nextInt(10); // Entre 5 e 14 movimentos aleatórios
        System.out.println("Carro criado com destino após " + maxMovements + " movimentos");
    }

    public void moveTo(Node targetNode, Node currentNode, Edge edge, MapData mapData) {
        if (moving || waitingAtTrafficLight || hasArrived) { // MODIFICADO: Não mover se já chegou
            if (hasArrived) {
                System.out.println("Carro já chegou ao destino, ignorando comando de movimento");
            } else {
                System.out.println("Carro já está se movendo ou esperando no semáforo, ignorando novo comando");
            }
            return;
        }

        this.targetNode = targetNode;
        this.currentNode = currentNode;
        this.currentEdge = edge;
        this.mapData = mapData;
        this.hasPassedTrafficLight = false; // Reset da flag ao iniciar novo movimento

        // Verificar semáforo no nó de destino antes de começar a se mover
        if (targetNode.getTrafficLight().isRed()) {
            System.out.println("Carro esperando semáforo verde no nó: " + targetNode.getId());
            waitAtTrafficLight();
            return;
        }

        startMovement();
    }

    private void waitAtTrafficLight() {
        waitingAtTrafficLight = true;

        // Timer para verificar o semáforo periodicamente
        trafficLightCheckTimer = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> {
            if (targetNode.getTrafficLight().isGreen()) {
                System.out.println("Semáforo ficou verde! Carro começando movimento para: " + targetNode.getId());
                stopTrafficLightCheck();
                waitingAtTrafficLight = false;
                startMovement();
            }
        }));
        trafficLightCheckTimer.setCycleCount(Timeline.INDEFINITE);
        trafficLightCheckTimer.play();
    }

    private void stopTrafficLightCheck() {
        if (trafficLightCheckTimer != null) {
            trafficLightCheckTimer.stop();
            trafficLightCheckTimer = null;
        }
    }

    private void startMovement() {
        // Verificar colisão com outros carros antes de se mover
        if (checkCollisionWithOtherCars()) {
            System.out.println("Carro aguardando - rota bloqueada por outro carro");
            // Tentar novamente após um tempo
            Timeline retryTimer = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> {
                if (!moving && !waitingAtTrafficLight && !hasArrived) {
                    moveTo(targetNode, currentNode, currentEdge, mapData);
                }
            }));
            retryTimer.play();
            return;
        }

        // Calcular coordenadas de destino usando os limites corretos do mapa
        double minLat = mapData.getMinLatitude();
        double maxLat = mapData.getMaxLatitude();
        double minLon = mapData.getMinLongitude();
        double maxLon = mapData.getMaxLongitude();

        double targetX = convertLonToScreen(targetNode.getLongitude(), minLon, maxLon);
        double targetY = convertLatToScreen(targetNode.getLatitude(), minLat, maxLat);

        // Calcular distância em pixels
        double deltaX = targetX - x;
        double deltaY = targetY - y;
        double pixelDistance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        System.out.println("Iniciando movimento: origem(" + String.format("%.2f", x) + ", " + String.format("%.2f", y) +
                ") -> destino(" + String.format("%.2f", targetX) + ", " + String.format("%.2f", targetY) +
                ") distância: " + String.format("%.2f", pixelDistance) + " pixels");

        // Se a distância for muito pequena, mover instantaneamente
        if (pixelDistance < 0.5) {
            x = targetX;
            y = targetY;
            currentNodeId = targetNode.getId();
            hasPassedTrafficLight = true; // Marca que passou do semáforo
            incrementMovementAndCheckArrival(); // MODIFICADO: Verificar chegada
            System.out.println("Movimento instantâneo - distância muito pequena");
            return;
        }

        // Calcular tempo baseado na velocidade e distância da aresta
        double realDistance = currentEdge.getLength(); // metros
        double speedMs = Math.max(speed / 3.6, currentEdge.getMaxspeed() / 2.0); // converter km/h para m/s
        double travelTime = realDistance / speedMs; // tempo em segundos

        // Ajustar duração da animação
        double animationDuration = Math.max(0.5, Math.min(travelTime / 1.5, 2.0)); // Reduz o divisor e os limites

        System.out.println("Duração da animação: " + String.format("%.2f", animationDuration) + " segundos");

        moving = true;

        // Criar animação de movimento suave com verificação contínua
        movementTimeline = new Timeline();

        final double startX = x;
        final double startY = y;
        final int steps = (int)(animationDuration * 40); // 120 FPS para suavidade

        for (int i = 0; i <= steps; i++) {
            final double progress = (double) i / steps;
            final double currentX = startX + (deltaX * progress);
            final double currentY = startY + (deltaY * progress);

            KeyFrame keyFrame = new KeyFrame(
                    Duration.seconds(i * animationDuration / steps),
                    e -> {
                        // Só verificar semáforo se ainda não passou da metade do caminho
                        // e se ainda não passou do semáforo anteriormente
                        if (progress >= 0.8 && progress < 0.95 && !hasPassedTrafficLight && targetNode.getTrafficLight().isRed()) {
                            System.out.println("Semáforo ficou vermelho antes de chegar - parando");
                            stopMovement();
                            // Reposicionar no ponto atual
                            x = currentX;
                            y = currentY;
                            waitAtTrafficLight();
                            return;
                        }

                        // Marcar que passou do semáforo quando chegar a 60% do caminho
                        if (progress >= 0.95) {
                            hasPassedTrafficLight = true;
                        }

                        // Verificar colisão durante o movimento
                        if (checkCollisionAtPosition(currentX, currentY)) {
                            System.out.println("Colisão detectada durante movimento - parando");
                            stopMovement();
                            // Reposicionar no ponto atual
                            x = currentX;
                            y = currentY;
                            // Tentar reposicionar ou aguardar
                            Timeline retryTimer = new Timeline(new KeyFrame(Duration.seconds(1.0), retryEvent -> {
                                if (!moving && !waitingAtTrafficLight && !hasArrived) {
                                    moveTo(targetNode, currentNode, currentEdge, mapData);
                                }
                            }));
                            retryTimer.play();
                            return;
                        }

                        x = currentX;
                        y = currentY;
                    }
            );
            movementTimeline.getKeyFrames().add(keyFrame);
        }

        // Quando a animação terminar
        movementTimeline.setOnFinished(e -> {
            x = targetX;
            y = targetY;
            currentNodeId = targetNode.getId();
            moving = false;
            hasPassedTrafficLight = true; // Garantir que marcou como passou
            incrementMovementAndCheckArrival(); // Verificar chegada ao destino
            System.out.println("Carro chegou ao nó: " + targetNode.getId() + " na posição (" +
                    String.format("%.2f", x) + ", " + String.format("%.2f", y) + ") - Movimento " +
                    movementCount + "/" + maxMovements);
        });

        movementTimeline.play();
    }

    // Método para incrementar movimento e verificar se chegou ao destino
    private void incrementMovementAndCheckArrival() {
        movementCount++;

        // Verificar se chegou ao destino baseado no número de movimentos
        if (movementCount >= maxMovements) {
            markAsArrived();
        }
    }

    // Para conversão de coordenadas (similares aos do MapView)
    private double convertLonToScreen(double longitude, double minLon, double maxLon) {
        double lonRange = maxLon - minLon;
        double latRange = mapData.getMaxLatitude() - mapData.getMinLatitude();

        // Calcular escala mantendo proporções
        double scaleX = 800.0 / lonRange; // MAP_WIDTH = 800
        double scaleY = 600.0 / latRange; // MAP_HEIGHT = 600
        double scale = Math.min(scaleX, scaleY);

        double mapWidthUsed = lonRange * scale;
        double offsetX = (800.0 - mapWidthUsed) / 2 + 10; // +10 para borda

        return offsetX + ((longitude - minLon) / lonRange) * mapWidthUsed;
    }

    private double convertLatToScreen(double latitude, double minLat, double maxLat) {
        double latRange = maxLat - minLat;
        double lonRange = mapData.getMaxLongitude() - mapData.getMinLongitude();

        // Calcular escala mantendo proporções
        double scaleX = 800.0 / lonRange; // MAP_WIDTH = 800
        double scaleY = 600.0 / latRange; // MAP_HEIGHT = 600
        double scale = Math.min(scaleX, scaleY);

        double mapHeightUsed = latRange * scale;
        double offsetY = (600.0 - mapHeightUsed) / 2 + 10; // +10 para borda

        // IMPORTANTE: Inverter o Y porque em coordenadas de tela Y cresce para baixo
        return offsetY + mapHeightUsed - ((latitude - minLat) / latRange) * mapHeightUsed;
    }

    // MODIFICADO: Método para marcar quando o carro chegou ao destino
    private void markAsArrived() {
        if (!hasArrived) {
            hasArrived = true;
            System.out.println("🎯 Carro chegou ao destino final: " + currentNodeId +
                    " após " + movementCount + " movimentos");
            // Aqui você pode adicionar lógica adicional, como notificar o controller
        }
    }

    private boolean checkCollisionWithOtherCars() {
        if (mapData == null) return false;

        // Esta verificação seria implementada pelo MapController
        // Por enquanto, retorna false - será implementada no MapController
        return false;
    }

    private boolean checkCollisionAtPosition(double checkX, double checkY) {
        if (mapData == null) return false;

        // Esta verificação seria implementada pelo MapController
        // Por enquanto, retorna false - será implementada no MapController
        return false;
    }

    public void stopMovement() {
        if (movementTimeline != null) {
            movementTimeline.stop();
            movementTimeline = null;
        }
        moving = false;
    }

    public void stop() {
        stopMovement();
        stopTrafficLightCheck();
        waitingAtTrafficLight = false;
        hasPassedTrafficLight = false;
        System.out.println("Carro completamente parado");
    }

    // Método para verificar se o carro está próximo de uma posição
    public boolean isNearPosition(double checkX, double checkY, double distance) {
        double deltaX = x - checkX;
        double deltaY = y - checkY;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY) <= distance;
    }

    // Para definir posição inicial usando coordenadas geográficas
    public void setInitialPosition(Node startNode, double minLat, double maxLat, double minLon, double maxLon, MapData mapData) {
        this.mapData = mapData; // Definir mapData antes de usar
        this.x = convertLonToScreen(startNode.getLongitude(), minLon, maxLon);
        this.y = convertLatToScreen(startNode.getLatitude(), minLat, maxLat);
        this.currentNodeId = startNode.getId();
        this.movementCount = 0; // Reset contador ao definir posição inicial
        System.out.println("Posição inicial do carro definida: (" + String.format("%.2f", x) + ", " + String.format("%.2f", y) +
                ") no nó " + startNode.getId() + " - Destino após " + maxMovements + " movimentos");
    }

    // Getters e Setters
    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        System.out.println("Posição do carro definida: (" + String.format("%.2f", x) + ", " + String.format("%.2f", y) + ")");
    }

    public boolean isMoving() {
        return moving;
    }

    public boolean isWaitingAtTrafficLight() {
        return waitingAtTrafficLight;
    }

    public boolean isBusy() {
        return moving || waitingAtTrafficLight;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public boolean hasPassedTrafficLight() {
        return hasPassedTrafficLight;
    }

    // Getter para verificar se o carro chegou ao destino
    public boolean hasArrived() {
        return hasArrived;
    }

    // Método para resetar o estado de chegada
    public void resetArrivalStatus() {
        hasArrived = false;
        movementCount = 0;
        maxMovements = 6 + random.nextInt(10); // Novo destino aleatório
        System.out.println("Status de chegada resetado - novo destino após " + maxMovements + " movimentos");
    }

    // Getters para informações de movimento
    public int getMovementCount() {
        return movementCount;
    }

    public int getMaxMovements() {
        return maxMovements;
    }

    // Método para obter forma visual do carro com cores mais claras
    public javafx.scene.shape.Shape getCarShape() {
        // Criar um retângulo para representar o carro
        javafx.scene.shape.Rectangle carShape = new javafx.scene.shape.Rectangle(x - 3, y - 3, 6, 6);

        // Cor baseada no estado do carro
        if (hasArrived) {
            carShape.setFill(javafx.scene.paint.Color.LIGHTBLUE); // Azul claro quando chegou ao destino
            carShape.setStroke(javafx.scene.paint.Color.BLUE);
            carShape.setStrokeWidth(2);
        } else if (waitingAtTrafficLight) {
            carShape.setFill(javafx.scene.paint.Color.DARKORANGE); // Laranja quando esperando semáforo
            carShape.setStroke(javafx.scene.paint.Color.ORANGE);
            carShape.setStrokeWidth(1);
        } else if (moving) {
            carShape.setFill(javafx.scene.paint.Color.LIGHTGREEN); // Verde claro quando se movendo
            carShape.setStroke(javafx.scene.paint.Color.GREEN);
            carShape.setStrokeWidth(1);
        } else {
            carShape.setFill(javafx.scene.paint.Color.LIGHTCORAL); // Vermelho claro quando parado
            carShape.setStroke(javafx.scene.paint.Color.LIGHTGREEN);
            carShape.setStrokeWidth(1);
        }

        return carShape;
    }

    @Override
    public String toString() {
        return "Car{" +
                "currentNodeId='" + currentNodeId + '\'' +
                ", x=" + String.format("%.2f", x) +
                ", y=" + String.format("%.2f", y) +
                ", moving=" + moving +
                ", waitingAtTrafficLight=" + waitingAtTrafficLight +
                ", hasPassedTrafficLight=" + hasPassedTrafficLight +
                ", hasArrived=" + hasArrived +
                ", movementCount=" + movementCount +
                ", maxMovements=" + maxMovements +
                '}';
    }
}