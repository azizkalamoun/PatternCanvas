package command;

import java.util.List;
import model.Shape;

public class DrawShapeCommand implements Command {
    private final List<Shape> shapes;
    private final Shape shape;

    public DrawShapeCommand(List<Shape> shapes, Shape shape) {
        this.shapes = shapes;
        this.shape = shape;
    }

    @Override
    public void execute() {
        shapes.add(shape);
    }

    @Override
    public void undo() {
        shapes.remove(shape);
    }
}
