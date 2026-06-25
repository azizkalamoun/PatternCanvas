package controller;

import command.CommandHistory;
import command.DeleteShapeCommand;
import command.DrawShapeCommand;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import logger.ConsoleLogger;
import logger.LoggerStrategy;
import model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DrawingController {
	private final Canvas canvas;
	private final GraphicsContext gc;
	private final List<Shape> shapes = new ArrayList<>();
	private final CommandHistory commandHistory = new CommandHistory();
	private String currentType = "Rectangle";
	private LoggerStrategy logger = new ConsoleLogger();
	private Color selectedColor = Color.BLACK;
	private double startX, startY;
	private Shape tempShape;
	private boolean eraserActive = false;
	private Shape selectedShape = null;

	public DrawingController(Canvas canvas) {
		this.canvas = canvas;
		this.gc = canvas.getGraphicsContext2D();
		enableDrawing();
	}

	public void setShapeType(String type) {
		ShapeType.fromString(type);
		this.currentType = type;
		this.eraserActive = false;
		this.selectedShape = null;
		logger.log("Forme sélectionnée : " + type);
		enableDrawing();
	}

	public void setColor(Color color) {
		this.selectedColor = color;
		logger.log("Couleur sélectionnée : " + colorToString(color));
	}

	private String colorToString(Color color) {
		return String.format("RGB(%d,%d,%d)", (int) (color.getRed() * 255), (int) (color.getGreen() * 255),
				(int) (color.getBlue() * 255));
	}

	public void setLogger(LoggerStrategy logger) {
		this.logger = logger;
		logger.log("Stratégie de log définie.");
	}

	public void enableDrawing() {
		if (!eraserActive) {
			if ("Circle".equals(currentType)) {
				canvas.setOnMousePressed(null);
				canvas.setOnMouseDragged(null);
				canvas.setOnMouseReleased(null);
				canvas.setOnMouseClicked(this::onMouseClickedCircle);
			} else {
				canvas.setOnMousePressed(this::onMousePressed);
				canvas.setOnMouseDragged(this::onMouseDragged);
				canvas.setOnMouseReleased(this::onMouseReleased);
				canvas.setOnMouseClicked(null);
			}
		}
	}

	public void setEraserActive(boolean active) {
		this.eraserActive = active;
		if (active) {
			canvas.setOnMousePressed(null);
			canvas.setOnMouseDragged(null);
			canvas.setOnMouseReleased(null);
			canvas.setOnMouseClicked(this::onMouseClickedEraser);
			logger.log("Gomme activée");
		} else {
			canvas.setOnMouseClicked(null);
			if ("Circle".equals(currentType)) {
				canvas.setOnMousePressed(null);
				canvas.setOnMouseDragged(null);
				canvas.setOnMouseReleased(null);
				canvas.setOnMouseClicked(this::onMouseClickedCircle);
			} else {
				canvas.setOnMousePressed(this::onMousePressed);
				canvas.setOnMouseDragged(this::onMouseDragged);
				canvas.setOnMouseReleased(this::onMouseReleased);
			}
			logger.log("Gomme désactivée");
		}
	}

	public boolean isEraserActive() {
		return eraserActive;
	}

	private void onMousePressed(MouseEvent e) {
		startX = e.getX();
		startY = e.getY();
	}

	private void onMouseDragged(MouseEvent e) {
		double endX = e.getX();
		double endY = e.getY();
		redrawCanvas();
		try {
			tempShape = ShapeFactory.createShape(currentType, startX, startY, endX, endY);
			tempShape = new ColorDecorator(tempShape, selectedColor);
			tempShape.draw(gc);
		} catch (IllegalArgumentException ex) {
			logger.log("Erreur création forme: " + ex.getMessage());
		}
	}

	private void onMouseReleased(MouseEvent e) {
		if (tempShape != null) {
			Shape shape = tempShape;
			tempShape = null;
			commandHistory.executeCommand(new DrawShapeCommand(shapes, shape));
			logger.log("Forme dessinée : " + shape.getType());
		}
	}

	private void onMouseClickedCircle(MouseEvent e) {
		try {
			Shape shape = ShapeFactory.createShape("Circle", e.getX(), e.getY());
			shape = new ColorDecorator(shape, selectedColor);
			shape.draw(gc);
			commandHistory.executeCommand(new DrawShapeCommand(shapes, shape));
			logger.log("Cercle dessiné.");
		} catch (IllegalArgumentException ex) {
			logger.log("Erreur création cercle: " + ex.getMessage());
		}
	}

	private void onMouseClickedEraser(MouseEvent e) {
		double clickX = e.getX();
		double clickY = e.getY();
		
		for (int i = shapes.size() - 1; i >= 0; i--) {
			Shape shape = shapes.get(i);
			if (isShapeAtPoint(shape, clickX, clickY)) {
				commandHistory.executeCommand(new DeleteShapeCommand(shapes, shape, i));
				logger.log("Forme supprimée : " + shape.getType());
				redrawCanvas();
				return;
			}
		}
	}

	private boolean isShapeAtPoint(Shape shape, double clickX, double clickY) {
		double x = shape.getX();
		double y = shape.getY();
		double w = shape.getW();
		double h = shape.getH();
		
		double tolerance = 10;
		
		if (shape.getType().equals("Circle")) {
			double distance = Math.sqrt(Math.pow(clickX - (x + w/2), 2) + Math.pow(clickY - (y + h/2), 2));
			return distance <= w/2 + tolerance;
		} else if (shape.getType().equals("Rectangle")) {
			return clickX >= x - tolerance && clickX <= x + w + tolerance &&
				   clickY >= y - tolerance && clickY <= y + h + tolerance;
		} else if (shape.getType().equals("Line")) {
			double lineLength = Math.sqrt(w * w + h * h);
			double distance = Math.abs(w * (y - clickY) - (x - clickX) * h) / lineLength;
			return distance <= tolerance;
		}
		return false;
	}

	public void undo() {
		if (commandHistory.undo()) {
			redrawCanvas();
			logger.log("Annulation effectuée");
		}
	}

	public void redo() {
		if (commandHistory.redo()) {
			redrawCanvas();
			logger.log("Rétablissement effectué");
		}
	}

	public void redrawCanvas() {
		gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
		gc.setStroke(javafx.scene.paint.Color.LIGHTGRAY);
		gc.setLineWidth(2);
		gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());
		for (Shape shape : shapes) {
			shape.draw(gc);
		}
	}

	public boolean saveToDatabase() {
		try {
			Connection conn = DBConnection.getInstance();

			String clearSql = "DELETE FROM shapes";
			try (PreparedStatement clearStmt = conn.prepareStatement(clearSql)) {
				clearStmt.executeUpdate();
			}

			String sql = "INSERT INTO shapes(type, x1, y1, x2, y2, color) VALUES (?, ?, ?, ?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				for (Shape s : shapes) {
					stmt.setString(1, s.getType());

					if (s.getType().equals("Circle")) {
						stmt.setDouble(2, s.getX());
						stmt.setDouble(3, s.getY());
						stmt.setDouble(4, s.getX() + s.getW());
						stmt.setDouble(5, s.getY() + s.getH());
					} else {
						stmt.setDouble(2, s.getX());
						stmt.setDouble(3, s.getY());
						stmt.setDouble(4, s.getX() + s.getW());
						stmt.setDouble(5, s.getY() + s.getH());
					}
					Color color = Color.BLACK;
					if (s instanceof ColorDecorator) {
						color = ((ColorDecorator) s).getColor();
					}
					stmt.setString(6, colorToString(color));

					stmt.executeUpdate();
				}
			}

			logger.log("Sauvegarde des formes en base réussie.");
			return true;
		} catch (Exception ex) {
			logger.log("Erreur DB: " + ex.getMessage());
			return false;
		}
	}

	public boolean loadFromDatabase() {
		try {
			Connection conn = DBConnection.getInstance();
			String sql = "SELECT type, x1, y1, x2, y2, color FROM shapes";

			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				ResultSet rs = stmt.executeQuery();

				shapes.clear();
				while (rs.next()) {
					String type = rs.getString("type");
					double x1 = rs.getDouble("x1");
					double y1 = rs.getDouble("y1");
					double x2 = rs.getDouble("x2");
					double y2 = rs.getDouble("y2");
					String colorStr = rs.getString("color");

					Shape shape;
					switch (type) {
					case "Line":
						shape = new LineShape(x1, y1, x2, y2);
						break;
					case "Rectangle":
						shape = new RectangleShape(x1, y1, x2, y2);
						break;
					case "Circle":
						shape = new CircleShape(x1, y1);
						break;
					default:
						continue;
					}
					Color color = parseColor(colorStr);
					if (color != null) {
						shape = new ColorDecorator(shape, color);
					}

					shapes.add(shape);
				}
			}

			redrawCanvas();
			logger.log("Chargement des formes depuis la base réussi.");
			return true;
		} catch (Exception ex) {
			logger.log("Erreur DB: " + ex.getMessage());
			return false;
		}
	}

	public boolean saveToDatabase(String drawingName) {
		try {
			Connection conn = DBConnection.getInstance();

			String drawingSql = "INSERT INTO drawings (name) VALUES (?)";
			int drawingId;

			try (PreparedStatement drawingStmt = conn.prepareStatement(drawingSql,
					PreparedStatement.RETURN_GENERATED_KEYS)) {
				drawingStmt.setString(1, drawingName);
				drawingStmt.executeUpdate();

				try (ResultSet generatedKeys = drawingStmt.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						drawingId = generatedKeys.getInt(1);
					} else {
						throw new SQLException("Création du dessin échouée, aucun ID obtenu.");
					}
				}
			}

			String shapeSql = "INSERT INTO drawing_shapes(drawing_id, type, x1, y1, x2, y2, color) VALUES (?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement shapeStmt = conn.prepareStatement(shapeSql)) {
				for (Shape s : shapes) {
					shapeStmt.setInt(1, drawingId);
					shapeStmt.setString(2, s.getType());

					if (s.getType().equals("Circle")) {
						shapeStmt.setDouble(3, s.getX());
						shapeStmt.setDouble(4, s.getY());
						shapeStmt.setDouble(5, s.getX() + s.getW());
						shapeStmt.setDouble(6, s.getY() + s.getH());
					} else {
						shapeStmt.setDouble(3, s.getX());
						shapeStmt.setDouble(4, s.getY());
						shapeStmt.setDouble(5, s.getX() + s.getW());
						shapeStmt.setDouble(6, s.getY() + s.getH());
					}
					Color color = Color.BLACK;
					if (s instanceof ColorDecorator) {
						color = ((ColorDecorator) s).getColor();
					}
					shapeStmt.setString(7, colorToString(color));

					shapeStmt.executeUpdate();
				}
			}

			logger.log("Dessin '" + drawingName + "' sauvegardé avec succès (ID: " + drawingId + ")");
			return true;
		} catch (Exception ex) {
			logger.log("Erreur DB: " + ex.getMessage());
			return false;
		}
	}

	public List<String> getDrawingsList() {
		List<String> drawingsList = new ArrayList<>();
		try {
			Connection conn = DBConnection.getInstance();
			String sql = "SELECT id, name, created_at FROM drawings ORDER BY created_at DESC";

			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				ResultSet rs = stmt.executeQuery();

				while (rs.next()) {
					int id = rs.getInt("id");
					String name = rs.getString("name");
					String date = rs.getTimestamp("created_at").toString();
					drawingsList.add(id + " - " + name + " (" + date + ")");
				}
			}
		} catch (Exception ex) {
			logger.log("Erreur DB: " + ex.getMessage());
		}
		return drawingsList;
	}

	public boolean loadDrawingFromDatabase(int drawingId) {
		try {
			Connection conn = DBConnection.getInstance();
			String sql = "SELECT type, x1, y1, x2, y2, color FROM drawing_shapes WHERE drawing_id = ?";

			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setInt(1, drawingId);
				ResultSet rs = stmt.executeQuery();

				shapes.clear();
				while (rs.next()) {
					String type = rs.getString("type");
					double x1 = rs.getDouble("x1");
					double y1 = rs.getDouble("y1");
					double x2 = rs.getDouble("x2");
					double y2 = rs.getDouble("y2");
					String colorStr = rs.getString("color");

					Shape shape;
					switch (type) {
					case "Line":
						shape = new LineShape(x1, y1, x2, y2);
						break;
					case "Rectangle":
						shape = new RectangleShape(x1, y1, x2, y2);
						break;
					case "Circle":
						shape = new CircleShape(x1, y1);
						break;
					default:
						continue;
					}

					Color color = parseColor(colorStr);
					if (color != null) {
						shape = new ColorDecorator(shape, color);
					}

					shapes.add(shape);
				}
			}

			redrawCanvas();
			logger.log("Dessin ID " + drawingId + " chargé avec succès.");
			return true;
		} catch (Exception ex) {
			logger.log("Erreur DB: " + ex.getMessage());
			return false;
		}
	}

	public void clearDrawing() {
		shapes.clear();
		commandHistory.clear();
		redrawCanvas();
		logger.log("Dessin effacé.");
	}

	private Color parseColor(String colorStr) {
		if (colorStr == null || colorStr.isEmpty()) {
			return Color.BLACK;
		}

		try {
			if (colorStr.startsWith("RGB(") && colorStr.endsWith(")")) {
				String[] parts = colorStr.substring(4, colorStr.length() - 1).split(",");
				if (parts.length == 3) {
					int r = Integer.parseInt(parts[0].trim());
					int g = Integer.parseInt(parts[1].trim());
					int b = Integer.parseInt(parts[2].trim());
					return Color.rgb(r, g, b);
				}
			}
		} catch (Exception e) {
			logger.log("Erreur parsing couleur: " + e.getMessage());
		}

		return Color.BLACK;
	}
}