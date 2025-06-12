package org.mapasimulador.mapasimulador.model;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.Random;

public class TrafficLight {
    public enum State {
        GREEN, RED
    }
    
    // Estados para as duas direções do cruzamento
    public enum Direction {
        HORIZONTAL, VERTICAL
    }
    
    private State currentState; // Estado para compatibilidade com código existente
    private Direction activeDirection; // Qual direção está com sinal verde
    private Timeline timeline;
    private static final Random random = new Random();
    
    // Tempos em segundos
    private static final double MIN_GREEN_TIME = 6.0;
    private static final double MAX_GREEN_TIME = 6.0;
    private static final double MIN_RED_TIME = 3.0;
    private static final double MAX_RED_TIME = 3.0;
    
    public TrafficLight() {
        // Direção inicial aleatória
        this.activeDirection = random.nextBoolean() ? Direction.HORIZONTAL : Direction.VERTICAL;
        this.currentState = State.GREEN; // A direção ativa sempre começa verde
        startCycle();
    }
    
    private void startCycle() {
        if (timeline != null) {
            timeline.stop();
        }
        
        double duration = getCurrentStateDuration();
        timeline = new Timeline(new KeyFrame(Duration.seconds(duration), e -> {
            switchState();
            startCycle(); // Reinicia o ciclo
        }));
        timeline.play();
    }
    
    private double getCurrentStateDuration() {
        if (currentState == State.GREEN) {
            return MIN_GREEN_TIME + random.nextDouble() * (MAX_GREEN_TIME - MIN_GREEN_TIME);
        } else {
            return MIN_RED_TIME + random.nextDouble() * (MAX_RED_TIME - MIN_RED_TIME);
        }
    }
    
    private void switchState() {
        if (currentState == State.GREEN) {
            // Se estava verde, muda para vermelho (período de transição)
            currentState = State.RED;
        } else {
            // Se estava vermelho, muda para verde e troca a direção ativa
            currentState = State.GREEN;
            activeDirection = (activeDirection == Direction.HORIZONTAL) ? 
                             Direction.VERTICAL : Direction.HORIZONTAL;
        }
    }
    
    // Métodos existentes mantidos para compatibilidade
    public State getCurrentState() {
        return currentState;
    }
    
    public boolean isGreen() {
        return currentState == State.GREEN;
    }
    
    public boolean isRed() {
        return currentState == State.RED;
    }
    
    // Novos métodos para verificar estado específico de cada direção
    public boolean isHorizontalGreen() {
        return currentState == State.GREEN && activeDirection == Direction.HORIZONTAL;
    }
    
    public boolean isHorizontalRed() {
        return currentState == State.RED || activeDirection == Direction.VERTICAL;
    }
    
    public boolean isVerticalGreen() {
        return currentState == State.GREEN && activeDirection == Direction.VERTICAL;
    }
    
    public boolean isVerticalRed() {
        return currentState == State.RED || activeDirection == Direction.HORIZONTAL;
    }
    
    public Direction getActiveDirection() {
        return activeDirection;
    }
    
    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
    }
    
    // Método para sincronizar semáforos (opcional PS: NÃO FOI USADO NO FIM DAS CONTAS)
    // public void setState(State state) {
    //   this.currentState = state;
    // }
}