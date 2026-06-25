package model;

import javafx.scene.canvas.GraphicsContext;

public class RectangleShape implements Shape {
    private final double x1, y1, x2, y2;

    public RectangleShape(double x1, double y1, double x2, double y2) {
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.strokeRect(x1, y1, x2 - x1, y2 - y1);
    }

    @Override
    public String getType() { 
        return "Rectangle"; 
    }
    
    @Override
    public double getX() { 
        return x1; 
    }
    
    @Override
    public double getY() { 
        return y1; 
    }
    
    @Override
    public double getW() { 
        return x2 - x1; 
    }
    
    @Override
    public double getH() { 
        return y2 - y1; 
    }
    
    @Override
    public String serialize() {
        return "Rectangle," + x1 + "," + y1 + "," + x2 + "," + y2;
    }
}