package main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import view.DrawingView;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        DrawingView root = new DrawingView();
        Scene scene = new Scene(root, 850, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Mini Projet MASI - JavaFX");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}