package model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ColorDecorator implements Shape {
    private final Shape decoratedShape;
    private final Color color;

    public ColorDecorator(Shape shape, Color color) {
        this.decoratedShape = shape;
        this.color = color;
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setStroke(color);
        decoratedShape.draw(gc);
        gc.setStroke(Color.BLACK);
    }

    @Override
    public String getType() {
        return decoratedShape.getType();
    }

    @Override
    public double getX() {
        return decoratedShape.getX();
    }

    @Override
    public double getY() {
        return decoratedShape.getY();
    }

    @Override
    public double getW() {
        return decoratedShape.getW();
    }

    @Override
    public double getH() {
        return decoratedShape.getH();
    }

    @Override
    public String serialize() {
        return decoratedShape.serialize();
    }
    
    public Color getColor() {
        return this.color;
    }
}