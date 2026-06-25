package model;

import java.util.function.Function;

public enum ShapeType {
    CIRCLE("Circle", coords -> new CircleShape(coords[0], coords[1])),
    RECTANGLE("Rectangle", coords -> new RectangleShape(coords[0], coords[1], coords[2], coords[3])),
    LINE("Line", coords -> new LineShape(coords[0], coords[1], coords[2], coords[3]));

    private final String displayName;
    private final Function<double[], Shape> factory;

    ShapeType(String displayName, Function<double[], Shape> factory) {
        this.displayName = displayName;
        this.factory = factory;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Shape create(double... coordinates) {
        return factory.apply(coordinates);
    }

    public static ShapeType fromString(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Shape type cannot be null");
        }
        for (ShapeType shapeType : ShapeType.values()) {
            if (shapeType.displayName.equalsIgnoreCase(type)) {
                return shapeType;
            }
        }
        throw new IllegalArgumentException("Unknown shape type: " + type);
    }
}
