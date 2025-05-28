package org.mapasimulador.mapasimulador.model;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.Random;

public class TrafficLight {
    public enum State {
        GREEN, RED
    }

    private State currentState;
    private Timeline timeline;
    private static final Random random = new Random();

    // Tempos em segundos
    private static final double MIN_GREEN_TIME = 6.0;
    private static final double MAX_GREEN_TIME = 6.0;
    private static final double MIN_RED_TIME = 3.0;
    private static final double MAX_RED_TIME = 3.0;

    public TrafficLight() {
        // Estado inicial aleatório
        this.currentState = random.nextBoolean() ? State.GREEN : State.RED;
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
        currentState = (currentState == State.GREEN) ? State.RED : State.GREEN;
    }

    public State getCurrentState() {
        return currentState;
    }

    public boolean isGreen() {
        return currentState == State.GREEN;
    }

    public boolean isRed() {
        return currentState == State.RED;
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