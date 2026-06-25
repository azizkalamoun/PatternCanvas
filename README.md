# PatternCanvas — JavaFX Drawing Application with Design Patterns

## Project Overview

**Masi** is a JavaFX-based vector drawing application that allows users to create, delete, and persist geometric shapes (rectangles, lines, and circles) on a canvas. It supports undo/redo, color selection, an eraser tool, and database persistence with named drawings.

The primary purpose of this project is to serve as a practical, code-first demonstration of **software design patterns** and **object-oriented design principles** within a real JavaFX application. It is intended for software engineering students, instructors, and professional Java developers studying design patterns in a desktop GUI context.

## Objectives

The project demonstrates the following architectural goals through concrete implementation:

| Goal | How It Is Achieved |
|------|-------------------|
| **Separation of Concerns** | Packages are organized by architectural role: `model`, `view`, `controller`, `command`, `logger`, `main`. Each package has a single responsibility. |
| **Extensibility** | New shape types can be added by implementing the `Shape` interface and adding an entry to the `ShapeType` enum. New log destinations implement the `LoggerStrategy` interface. New commands implement the `Command` interface. |
| **Maintainability** | The Controller mediates all interaction between the View and the Model. The View has no business logic. Each command, shape, and logger is a self-contained class. |
| **Reusability** | `ColorDecorator` can wrap any `Shape` without modifying the shape's class. `CommandHistory` is a reusable undo/redo stack. `DBConnection` provides a single shared connection. |
| **Object-Oriented Design** | Polymorphism drives shape drawing (`Shape.draw()`), command execution (`Command.execute()`/`undo()`), and logging (`LoggerStrategy.log()`). Composition is used for decoration (`ColorDecorator` wraps `Shape`). |

## Design Patterns Implemented

### MVC (Model-View-Controller)

**Intent:** Separate the internal representation of data (Model), the user interface (View), and the control logic (Controller) into distinct components so that each can be modified independently.

**Implementation:**
- **Model** — `Shape`, `CircleShape`, `RectangleShape`, `LineShape`, `ShapeType`, `ColorDecorator`, `DBConnection`. These classes hold data and drawing logic but know nothing about the UI.
- **View** — `DrawingView` constructs the JavaFX scene graph (toolbar, canvas, color picker, logger combo box) and wires UI events to the Controller. It has no business logic.
- **Controller** — `DrawingController` holds the shape list (`List<Shape>`), handles mouse events, invokes the command history, and triggers canvas redraws. It mediates all interactions between the View and the Model.

**Relevant Classes:**
- [`src/model/Shape.java`](src/model/Shape.java)
- [`src/model/CircleShape.java`](src/model/CircleShape.java)
- [`src/model/RectangleShape.java`](src/model/RectangleShape.java)
- [`src/model/LineShape.java`](src/model/LineShape.java)
- [`src/view/DrawingView.java`](src/view/DrawingView.java)
- [`src/controller/DrawingController.java`](src/controller/DrawingController.java)

**Benefits:**
- The canvas rendering and shape list management live in the Controller, not the View.
- The UI layout can be redesigned without touching shape logic.
- Shape serialization and database persistence are isolated from JavaFX components.

---

### Strategy

**Intent:** Define a family of interchangeable algorithms, encapsulate each one, and make them interchangeable at runtime.

**Implementation:**
- `LoggerStrategy` (interface) declares `void log(String message)`.
- Three concrete strategies implement it:
  - `ConsoleLogger` — writes to `System.out`.
  - `FileLogger` — appends to a timestamped `.txt` file (`log_yyyyMMdd_HHmmss.txt`).
  - `DatabaseLogger` — inserts into a MySQL `log` table via JDBC.
- The Controller holds a `LoggerStrategy logger` field and exposes `setLogger(LoggerStrategy)`. The View's combo box selects the strategy at runtime (`DrawingView.java:167-173`).

**Relevant Classes:**
- [`src/logger/LoggerStrategy.java`](src/logger/LoggerStrategy.java)
- [`src/logger/ConsoleLogger.java`](src/logger/ConsoleLogger.java)
- [`src/logger/FileLogger.java`](src/logger/FileLogger.java)
- [`src/logger/DatabaseLogger.java`](src/logger/DatabaseLogger.java)

**Benefits:**
- Logging destination can be changed at runtime without modifying the Controller.
- Adding a new logging target (e.g., a remote logging service) requires only a new `LoggerStrategy` implementation.
- The Controller is decoupled from the logging implementation.

---

### Command

**Intent:** Encapsulate a request as an object, thereby allowing parameterization of clients with different requests, queuing, logging, and undoable operations.

**Implementation:**
- `Command` (interface) declares `void execute()` and `void undo()`.
- `DrawShapeCommand` — adds a `Shape` to the list on `execute()`, removes it on `undo()`. Holds a reference to the shape list and the shape.
- `DeleteShapeCommand` — removes a `Shape` from the list on `execute()`, re-inserts it at its original index on `undo()`.
- `CommandHistory` maintains two stacks (`undoStack`, `redoStack`). `executeCommand()` pushes to the undo stack and clears the redo stack. `undo()` pops from undo, calls `undo()`, pushes to redo. `redo()` pops from redo, calls `execute()`, pushes to undo.

**Relevant Classes:**
- [`src/command/Command.java`](src/command/Command.java)
- [`src/command/DrawShapeCommand.java`](src/command/DrawShapeCommand.java)
- [`src/command/DeleteShapeCommand.java`](src/command/DeleteShapeCommand.java)
- [`src/command/CommandHistory.java`](src/command/CommandHistory.java)

**Benefits:**
- Undo/redo is implemented generically: any operation can be a `Command`.
- New operations (e.g., move shape, resize) require only a new `Command` class.
- The Controller does not need to manage undo/redo state manually.

---

### Factory Method

**Intent:** Define an interface or abstract class for creating an object, but let subclasses or implementing classes decide which class to instantiate.

**Implementation:**
- `ShapeFactory.createShape(String type, double... coordinates)` delegates to `ShapeType.fromString(type).create(coordinates)`.
- `ShapeType` is an enum where each constant holds a `Function<double[], Shape>` factory lambda:
  - `CIRCLE` → `coords -> new CircleShape(coords[0], coords[1])`
  - `RECTANGLE` → `coords -> new RectangleShape(coords[0], coords[1], coords[2], coords[3])`
  - `LINE` → `coords -> new LineShape(coords[0], coords[1], coords[2], coords[3])`
- The `ShapeType.fromString()` method resolves a display name to the matching enum constant.

**Relevant Classes:**
- [`src/model/ShapeFactory.java`](src/model/ShapeFactory.java)
- [`src/model/ShapeType.java`](src/model/ShapeType.java)
- [`src/model/Shape.java`](src/model/Shape.java)

**Benefits:**
- Adding a new shape type requires implementing `Shape`, adding an enum constant with a factory lambda, and nothing else in the creation logic changes.
- The Controller creates shapes through `ShapeFactory` without depending on concrete shape classes.
- The factory can validate coordinates before construction.

---

### Decorator

**Intent:** Attach additional responsibilities to an object dynamically without altering its class.

**Implementation:**
- `ColorDecorator` implements `Shape` and wraps a delegate `Shape` (the `decoratedShape` field).
- `ColorDecorator.draw(GraphicsContext)` sets the stroke color on the graphics context, calls `decoratedShape.draw(gc)`, then resets the stroke color to black.
- All other methods (`getType()`, `getX()`, `getY()`, `getW()`, `getH()`, `serialize()`) are delegated directly to the decorated shape.
- In the Controller, shapes are wrapped at creation time: `shape = new ColorDecorator(shape, selectedColor)` (`DrawingController.java:119, 138`).

**Relevant Classes:**
- [`src/model/ColorDecorator.java`](src/model/ColorDecorator.java)
- [`src/model/Shape.java`](src/model/Shape.java)

**Benefits:**
- Shape classes themselves have no color awareness; color is added transparently via decoration.
- Multiple decorators could be composed (e.g., `new ShadowDecorator(new ColorDecorator(shape, color))`) without modifying shape code.
- The decorator preserves the `Shape` interface, so the rest of the system treats decorated and undecorated shapes identically.

---

### Singleton

**Intent:** Ensure a class has only one instance and provide a global point of access to it.

**Implementation:**
- `DBConnection` has a `private static Connection instance` field, a `private` constructor, and a `public static Connection getInstance()` method.
- `getInstance()` lazily initializes the connection and calls `initTables()` on first access. If the connection is closed, it re-creates it.
- The `closeConnection()` static method allows clean shutdown.

**Relevant Classes:**
- [`src/model/DBConnection.java`](src/model/DBConnection.java)

**Benefits:**
- All parts of the application (Controller, `DatabaseLogger`) share one database connection.
- Connection pooling is avoided for simplicity while still preventing redundant connections.
- Tables are auto-created on first connection, eliminating manual schema setup for development.

---

### State (Simple)

**Intent:** Allow an object to alter its behavior when its internal state changes.

**Implementation:**
- `DrawingController` tracks three state variables: `currentType` (`"Rectangle"`, `"Line"`, `"Circle"`), `eraserActive` (boolean), and `selectedShape`.
- The `enableDrawing()` method assigns different mouse event handlers based on the current state:
  - `"Circle"` → `onMouseClickedCircle()` (single-click placement, fixed radius).
  - `"Rectangle"` or `"Line"` → `onMousePressed()`, `onMouseDragged()`, `onMouseReleased()` (click-drag-release).
- `setEraserActive()` swaps handlers between eraser mode (`onMouseClickedEraser()`) and the active drawing mode.
- This is a lightweight, non-enum-based State pattern where conditionals in the controller drive behavior selection.

**Relevant Classes:**
- [`src/controller/DrawingController.java`](src/controller/DrawingController.java)

**Benefits:**
- The View is not responsible for understanding which mouse interaction model is active.
- Adding a new interaction mode (e.g., polygon with click-to-add-vertex) requires adding a new handler method and a state branch.
- The eraser mode cleanly toggles between deletion and drawing without complicating the shape-creation handlers.

---

### Observer (via JavaFX Event Handlers)

**Intent:** Define a one-to-many dependency between objects so that when one object changes state, all its dependents are notified automatically.

**Implementation:**
- JavaFX's built-in event handler system implements the Observer pattern.
- `DrawingView` registers observers (event handlers) on UI controls:
  - `btnRect.setOnAction(e -> controller.setShapeType("Rectangle"))`
  - `colorPicker.setOnAction(e -> controller.setColor(colorPicker.getValue()))`
  - `logChoice.setOnAction(e -> controller.setLogger(...))`
- The Controller indirectly notifies the View of state changes by modifying the shared shape list, which the View renders via `redrawCanvas()`.

**Relevant Classes:**
- [`src/view/DrawingView.java`](src/view/DrawingView.java)
- [`src/controller/DrawingController.java`](src/controller/DrawingController.java)

**Benefits:**
- UI components and business logic are loosely coupled.
- Adding a new toolbar button requires only a new handler and possibly a new Controller method.
- The pattern is provided by the JavaFX framework and requires no custom observer infrastructure.

---

## Architecture Overview

### Layer Separation

The application follows a strict three-layer architecture (MVC) with two supporting subsystems (Command, Logger):

```
┌─────────────────────────────────────────────┐
│                   View                       │
│            DrawingView (JavaFX)              │
├─────────────────────────────────────────────┤
│                 Controller                   │
│           DrawingController                  │
├────────────────────┬────────────────────────┤
│       Model        │   Supporting Subsystems │
│  Shape hierarchy   │  ┌─────────┬─────────┐ │
│  ShapeFactory      │  │ Command │ Logger  │ │
│  ColorDecorator    │  │ Pattern │ Strategy│ │
│  DBConnection      │  └─────────┴─────────┘ │
└────────────────────┴────────────────────────┘
```

### Model Layer

The [`src/model/`](src/model/) package contains:
- **`Shape` interface** — defines `draw()`, `getType()`, `getX()`, `getY()`, `getW()`, `getH()`, `serialize()`.
- **Concrete shapes** — `CircleShape` (fixed-radius 40px circle), `RectangleShape` (axis-aligned rectangle), `LineShape` (arbitrary line).
- **`ShapeType` enum** — maps display names to factory lambdas.
- **`ShapeFactory`** — static facade that delegates to `ShapeType`.
- **`ColorDecorator`** — wraps any `Shape` with a stroke color.
- **`DBConnection`** — Singleton JDBC connection to a MySQL database (`dessin`).

### View Layer

[`src/view/DrawingView.java`](src/view/DrawingView.java) builds a `BorderPane` with:
- **Top**: A `ToolBar` with shape buttons (Rectangle, Line, Circle), Undo/Redo, Eraser, ColorPicker, Logger selector, Clear/Save/Load buttons.
- **Center**: A `StackPane` containing a `VBox` with the `Canvas` (800×600).

### Controller Layer

[`src/controller/DrawingController.java`](src/controller/DrawingController.java) manages:
- **Shape list** (`List<Shape> shapes`).
- **Command history** (`CommandHistory`).
- **Current tool state** (type, color, eraser active).
- **Mouse event handlers** that create shapes, wrap them in `ColorDecorator`, execute `DrawShapeCommand`, and redraw.
- **Persistence** methods (`saveToDatabase`, `loadFromDatabase`, `saveToDatabase(String)`, `loadDrawingFromDatabase(int)`, `getDrawingsList`).

### Shape Creation Flow

1. User clicks a shape button in the toolbar.
2. `DrawingView` calls `controller.setShapeType(type)` and `controller.enableDrawing()`.
3. `enableDrawing()` assigns appropriate mouse handlers to the canvas based on the shape type.
4. User interacts with the canvas (click for circle, drag-release for rectangle/line).
5. Mouse handler calls `ShapeFactory.createShape(type, coordinates)` → `ShapeType.create(coordinates)` → concrete shape constructor.
6. The returned `Shape` is wrapped: `new ColorDecorator(shape, selectedColor)`.
7. A `DrawShapeCommand` is constructed and passed to `commandHistory.executeCommand()`.
8. `CommandHistory` calls `command.execute()` (adds shape to list), pushes to undo stack, clears redo stack.
9. `DrawingController.redrawCanvas()` clears the canvas and iterates over all shapes calling `shape.draw(gc)`.

### Drawing Workflow

- The canvas is redrawn entirely on every change (no incremental rendering).
- `redrawCanvas()` calls `gc.clearRect()`, draws a light-gray border, then iterates `shapes` calling `shape.draw(gc)` for each.
- Polymorphism ensures that each shape type renders itself correctly (`gc.strokeOval`, `gc.strokeRect`, `gc.strokeLine`), and `ColorDecorator` applies the stroke color before delegating.

### Persistence Workflow

- **Save**: `controller.saveToDatabase(drawingName)` inserts a row into the `drawings` table, then inserts each shape into `drawing_shapes` with the drawing's foreign key.
- **Load**: `controller.getDrawingsList()` queries `drawings` table. User selects a drawing. `controller.loadDrawingFromDatabase(drawingId)` queries `drawing_shapes`, reconstructs shapes via `switch` on type, wraps in `ColorDecorator` if color is present, and redraws.
- **Simple persistence**: `saveToDatabase()` (without name) clears and re-inserts into `shapes` table. `loadFromDatabase()` reads all rows.
- All persistence uses `DBConnection.getInstance()` for the shared connection.

### Logging Workflow

- All significant actions (shape creation, undo, redo, eraser toggle, DB operations) call `logger.log(message)`.
- The logger is initially `ConsoleLogger` but can be switched at runtime via the toolbar combo box.
- `FileLogger` writes to `log_<timestamp>.txt`.
- `DatabaseLogger` inserts into the `log` table and performs runtime diagnostics (database name, version, row count).

### Undo/Redo Workflow

1. Any drawing or deletion action creates a `Command` object and calls `commandHistory.executeCommand(command)`.
2. `executeCommand()` calls `command.execute()`, pushes to undo stack, clears redo stack.
3. **Undo**: `commandHistory.undo()` pops the undo stack, calls `command.undo()`, pushes to redo stack, then `redrawCanvas()`.
4. **Redo**: `commandHistory.redo()` pops the redo stack, calls `command.execute()`, pushes to undo stack, then `redrawCanvas()`.
5. `clear()` resets both stacks (used on "Clear Drawing").

## Technologies and Dependencies

| Technology | Version | Usage |
|------------|---------|-------|
| **Java** | 26 ([JavaSE-26](.classpath#L3)) | Application language and runtime |
| **JavaFX** | 26 ([javafx-sdk-26](.classpath#L9)) | GUI framework (controls, graphics, FXML) |
| **MySQL Connector/J** | 9.7.0 ([lib/mysql-connector-j-9.7.0.jar](lib/mysql-connector-j-9.7.0.jar)) | JDBC database driver |
| **MySQL** | (server-side) | Database for shape and log persistence |
| **Eclipse IDE** | ([.project](.project)) | Development environment |

### Java Modules (from [`src/module-info.java`](src/module-info.java))

| Module | Purpose |
|--------|---------|
| `javafx.controls` | UI controls (Button, ComboBox, ColorPicker, ToolBar, Canvas) |
| `javafx.graphics` | Canvas, GraphicsContext, Scene, Stage, layout panes |
| `javafx.fxml` | FXML loading support (declared but not used in source) |
| `java.sql` | JDBC API for database connectivity |

### Official Documentation Links

- [Java 26 Documentation](https://docs.oracle.com/en/java/javase/26/)
- [JavaFX 26 Documentation](https://openjfx.io/javadoc/26/)
- [MySQL Connector/J 9.7 Documentation](https://dev.mysql.com/doc/connector-j/9.7/en/)

## Project Structure

```
Masi/
├── .classpath                          # Eclipse classpath (Java 26, JavaFX 26, MySQL Connector)
├── .project                            # Eclipse project configuration
├── build.fxbuild                       # Eclipse FX build configuration
├── init_db.sql                         # Database schema initialization script
├── lib/
│   └── mysql-connector-j-9.7.0.jar     # MySQL JDBC driver
├── src/
│   ├── module-info.java                # Java module declaration
│   ├── main/
│   │   └── Main.java                   # Application entry point (JavaFX Application)
│   ├── model/
│   │   ├── Shape.java                  # Shape interface
│   │   ├── CircleShape.java            # Circle implementation (fixed radius)
│   │   ├── RectangleShape.java         # Rectangle implementation
│   │   ├── LineShape.java              # Line implementation
│   │   ├── ShapeType.java              # Enum with factory lambdas
│   │   ├── ShapeFactory.java           # Static factory facade
│   │   ├── ColorDecorator.java         # Decorator adding color behavior
│   │   └── DBConnection.java           # Singleton database connection
│   ├── view/
│   │   └── DrawingView.java            # JavaFX UI layout and controls
│   ├── controller/
│   │   └── DrawingController.java      # Event handling, state, persistence
│   ├── command/
│   │   ├── Command.java                # Command interface
│   │   ├── DrawShapeCommand.java       # Command for adding a shape
│   │   ├── DeleteShapeCommand.java     # Command for removing a shape
│   │   └── CommandHistory.java         # Undo/redo stack manager
│   └── logger/
│       ├── LoggerStrategy.java         # Strategy interface
│       ├── ConsoleLogger.java          # Logs to System.out
│       ├── FileLogger.java             # Logs to timestamped file
│       └── DatabaseLogger.java         # Logs to MySQL log table
└── bin/                                # Compiled output
```

### Package Responsibilities

| Package | Purpose |
|---------|---------|
| [`src/main/`](src/main/) | Application entry point. `Main` extends `javafx.application.Application` and launches the `DrawingView`. |
| [`src/model/`](src/model/) | Domain model: the `Shape` hierarchy, factory (`ShapeFactory` / `ShapeType`), decoration (`ColorDecorator`), and database connection (`DBConnection`). |
| [`src/view/`](src/view/) | JavaFX user interface. `DrawingView` composes the toolbar, canvas, and controls, and wires them to the Controller. |
| [`src/controller/`](src/controller/) | Application logic. `DrawingController` handles mouse input, manages the shape list, coordinates undo/redo, and performs database persistence. |
| [`src/command/`](src/command/) | Command pattern infrastructure. `Command` interface, concrete commands for draw/delete operations, and `CommandHistory` for undo/redo stacks. |
| [`src/logger/`](src/logger/) | Strategy pattern infrastructure. `LoggerStrategy` interface and three concrete implementations for console, file, and database logging. |

## Pattern-to-Code Mapping

| Pattern | Key Interfaces/Classes | Primary Role |
|---------|----------------------|--------------|
| **MVC** | `Shape`, `DrawingView`, `DrawingController` | Three-layer separation of concerns |
| **Strategy** | `LoggerStrategy`, `ConsoleLogger`, `FileLogger`, `DatabaseLogger` | Runtime-swappable logging destination |
| **Command** | `Command`, `DrawShapeCommand`, `DeleteShapeCommand`, `CommandHistory` | Encapsulated undo/redo operations |
| **Factory Method** | `ShapeFactory`, `ShapeType` | Polymorphic shape creation decoupled from callers |
| **Decorator** | `ColorDecorator`, `Shape` | Dynamic attachment of color behavior to shapes |
| **Singleton** | `DBConnection` | Single shared JDBC connection instance |
| **State (simple)** | `DrawingController` (mode fields) | Tool-dependent mouse interaction behavior |
| **Observer** | JavaFX `EventHandler` on UI controls | Event-driven UI-to-controller communication |

## Educational Value

Students and developers examining this codebase can learn:

- **How to implement design patterns in a cohesive application** — each pattern solves a real, visible problem rather than existing in isolation.
- **JavaFX event handling** — mouse event registration (`setOnMousePressed`, `setOnMouseDragged`, `setOnMouseReleased`, `setOnMouseClicked`), action handlers on buttons, and the `ColorPicker` control.
- **MVC architecture in a GUI context** — the View creates the Controller and delegates all logic; the Controller holds the model state and mediates all changes; the Model is pure domain logic with no UI dependency.
- **Undo/Redo via the Command pattern** — how two stacks and an interface with `execute()`/`undo()` provide a generic undo/redo mechanism that works for both additive (draw) and subtractive (delete) operations.
- **JDBC and database integration** — `Connection`, `PreparedStatement`, `ResultSet`, auto-generated keys, `CREATE TABLE IF NOT EXISTS`, foreign keys, and cascading deletes.
- **Runtime behavior composition** — the Decorator wraps shapes dynamically; the Strategy selects the logger at runtime; both demonstrate composition over inheritance.
- **Polymorphism and interface-based design** — the `Shape` interface drives the entire drawing pipeline (`List<Shape>`, `shape.draw(gc)`, `shape.getType()`).
- **Enum-based factories with lambdas** — `ShapeType` uses `Function<double[], Shape>` for a compact, type-safe factory per enum constant.

## Development Notes

### Prerequisites

- Java 26 JDK
- JavaFX 26 SDK
- MySQL server with a `dessin` database
- MySQL Connector/J 9.7.0 JAR (included at [`lib/mysql-connector-j-9.7.0.jar`](lib/mysql-connector-j-9.7.0.jar))

### Running

The project is configured for **Eclipse IDE** with the e(fx)clipse plugin. Build and run via:

1. Import the project into Eclipse.
2. Ensure the JavaFX 26 SDK path in [`.classpath`](.classpath) matches your local installation.
3. Add VM arguments: `--module-path <javafx-sdk-26/lib> --add-modules javafx.controls,javafx.graphics,javafx.fxml`.
4. Run `main.Main` as a Java application.

### Database Connection

The connection URL (`jdbc:mysql://localhost:3306/dessin?...`) is hardcoded in [`DBConnection.java`](src/model/DBConnection.java#L12-L14) with user `root` and no password. Adjust these values for your MySQL configuration. The `createDatabaseIfNotExist=true` parameter will create the `dessin` database automatically.

### Known Design Considerations

- Circle radius is fixed at 40px (hardcoded in `CircleShape.java`). Circles are placed with a single click rather than click-drag.
- The eraser uses a 10px tolerance hit-test (`DrawingController.java:168`).
- Undo/redo stacks are held in memory and cleared on `clearDrawing()`.
- The `javafx.fxml` module is declared in `module-info.java` but not used in the current source (no FXML files exist). It is retained for potential future FXML-based views.
