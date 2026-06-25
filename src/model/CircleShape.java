package model;

import javafx.scene.canvas.GraphicsContext;

public class CircleShape implements Shape {
    private final double x, y;
    private final double radius = 40;

    public CircleShape(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.strokeOval(x, y, radius, radius);
    }

    @Override
    public String getType() { 
        return "Circle"; 
    }
    
    @Override
    public double getX() { 
        return x; 
    }
    
    @Override
    public double getY() { 
        return y; 
    }
    
    @Override
    public double getW() { 
        return radius; 
    }
    
    @Override
    public double getH() { 
        return radius; 
    }
    
    @Override
    public String serialize() {
        return "Circle," + x + "," + y + "," + x + "," + y;
    }
}