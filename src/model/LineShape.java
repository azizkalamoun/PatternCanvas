package model;

import javafx.scene.canvas.GraphicsContext;

public class LineShape implements Shape {
    private final double x1, y1, x2, y2;

    public LineShape(double x1, double y1, double x2, double y2) {
        this.x1 = x1; 
        this.y1 = y1;
        this.x2 = x2; 
        this.y2 = y2;
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.strokeLine(x1, y1, x2, y2);
    }

    @Override
    public String getType() { 
        return "Line"; 
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
        return "Line," + x1 + "," + y1 + "," + x2 + "," + y2;
    }
}