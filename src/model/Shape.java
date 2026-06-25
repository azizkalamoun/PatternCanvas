package model;

import javafx.scene.canvas.GraphicsContext;

public interface Shape {
    void draw(GraphicsContext gc);
    String getType();
    double getX();
    double getY();
    double getW();
    double getH();
    String serialize();
}