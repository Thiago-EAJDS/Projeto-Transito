package org.mapasimulador.mapasimulador.model;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.Random;

/**
 * Semáforo de um cruzamento com duas direções conflitantes: norte-sul e leste-oeste.
 * As fases se alternam assim:
 * verde NS -> amarelo NS -> tudo vermelho -> verde EW -> amarelo EW -> tudo vermelho.
 * Assim as duas direções nunca ficam verdes ao mesmo tempo.
 */
public class TrafficLight {
    public enum State {
        GREEN, YELLOW, RED
    }

    public enum Approach {
        NORTH_SOUTH, EAST_WEST
    }

    // Duração de cada fase do ciclo, em segundos
    private static final double[] PHASE_DURATIONS = {6.0, 2.0, 1.0, 6.0, 2.0, 1.0};
    private static final int NS_GREEN_PHASE = 0;
    private static final int EW_GREEN_PHASE = 3;
    private static final Random random = new Random();

    private int phase;
    private Timeline timeline;

    public TrafficLight() {
        // Fase inicial aleatória (defasagem entre cruzamentos)
        this.phase = random.nextInt(PHASE_DURATIONS.length);
    }

    /** Eixo de aproximação de um carro que segue de {@code from} até {@code to}. */
    public static Approach approachOf(Node from, Node to) {
        double dLat = Math.abs(to.getLatitude() - from.getLatitude());
        // A longitude é corrigida pelo cosseno da latitude para ter a mesma escala de distância
        double dLon = Math.abs(to.getLongitude() - from.getLongitude())
                * Math.cos(Math.toRadians(from.getLatitude()));
        return dLat >= dLon ? Approach.NORTH_SOUTH : Approach.EAST_WEST;
    }

    public void start() {
        startPhase();
    }

    private void startPhase() {
        if (timeline != null) {
            timeline.stop();
        }

        timeline = new Timeline(new KeyFrame(Duration.seconds(PHASE_DURATIONS[phase]), e -> {
            phase = (phase + 1) % PHASE_DURATIONS.length;
            startPhase();
        }));
        timeline.play();
    }

    public State stateFor(Approach approach) {
        int greenPhase = (approach == Approach.NORTH_SOUTH) ? NS_GREEN_PHASE : EW_GREEN_PHASE;
        if (phase == greenPhase) {
            return State.GREEN;
        }
        if (phase == greenPhase + 1) {
            return State.YELLOW;
        }
        return State.RED;
    }

    // Amarelo conta como parada para carros que ainda não iniciaram o movimento
    public boolean isGreenFor(Approach approach) {
        return stateFor(approach) == State.GREEN;
    }

    public boolean isRedFor(Approach approach) {
        return stateFor(approach) != State.GREEN;
    }

    /** Estado do eixo norte-sul, usado para representar o cruzamento na interface. */
    public State getCurrentState() {
        return stateFor(Approach.NORTH_SOUTH);
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}
