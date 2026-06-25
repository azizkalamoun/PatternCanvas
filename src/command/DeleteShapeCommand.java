package command;

import java.util.List;
import model.Shape;

public class DeleteShapeCommand implements Command {
    private final List<Shape> shapes;
    private final Shape shape;
    private final int index;

    public DeleteShapeCommand(List<Shape> shapes, Shape shape, int index) {
        this.shapes = shapes;
        this.shape = shape;
        this.index = index;
    }

    @Override
    public void execute() {
        shapes.remove(shape);
    }

    @Override
    public void undo() {
        shapes.add(Math.min(index, shapes.size()), shape);
    }
}
