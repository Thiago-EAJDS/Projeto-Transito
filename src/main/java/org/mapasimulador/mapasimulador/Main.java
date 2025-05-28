package org.mapasimulador.mapasimulador;

import org.mapasimulador.mapasimulador.view.MapView;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            MapView mapView = new MapView(primaryStage);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        System.out.println("Aplicação sendo encerrada...");
    }

    public static void main(String[] args) {
        launch(args);
    }
}