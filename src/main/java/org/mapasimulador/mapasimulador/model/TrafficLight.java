package org.mapasimulador.mapasimulador.model;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.Random;

public class TrafficLight {
    public enum State {
        GREEN, YELLOW, RED
    }

    private State currentState;
    private Timeline timeline;
    private static final Random random = new Random();

    // Tempos de cada fase em segundos (ciclo: verde -> amarelo -> vermelho)
    private static final double GREEN_TIME = 6.0;
    private static final double YELLOW_TIME = 2.0;
    private static final double RED_TIME = 4.0;

    public TrafficLight() {
        // Estado inicial aleatório (defasagem entre cruzamentos)
        State[] states = State.values();
        this.currentState = states[random.nextInt(states.length)];
    }

    // Método para iniciar o ciclo do semáforo
    public void startAsMaster() {
        startCycle();
    }

    private void startCycle() {
        if (timeline != null) {
            timeline.stop();
        }

        double duration = getCurrentStateDuration();
        timeline = new Timeline(new KeyFrame(Duration.seconds(duration), e -> {
            switchState();
            startCycle(); // Reinicia o ciclo com a duração da nova fase
        }));
        timeline.play();
    }

    private double getCurrentStateDuration() {
        switch (currentState) {
            case GREEN:  return GREEN_TIME;
            case YELLOW: return YELLOW_TIME;
            default:     return RED_TIME;
        }
    }

    private void switchState() {
        switch (currentState) {
            case GREEN:  currentState = State.YELLOW; break;
            case YELLOW: currentState = State.RED;    break;
            default:     currentState = State.GREEN;  break;
        }
    }

    public State getCurrentState() {
        return currentState;
    }

    public boolean isGreen() {
        return currentState == State.GREEN;
    }

    public boolean isYellow() {
        return currentState == State.YELLOW;
    }

    // Amarelo conta como parada para carros que ainda não iniciaram o movimento
    public boolean isRed() {
        return currentState != State.GREEN;
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}
