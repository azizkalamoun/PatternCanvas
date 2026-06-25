package model;

public class ShapeFactory {
    public static Shape createShape(String type, double... coordinates) {
        return ShapeType.fromString(type).create(coordinates);
    }
}
