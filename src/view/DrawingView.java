package view;

import controller.DrawingController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import logger.ConsoleLogger;
import logger.DatabaseLogger;
import logger.FileLogger;
import model.DBConnection;
import java.util.List;
import java.util.Optional;

public class DrawingView extends BorderPane {
    public DrawingView() {
        Canvas canvas = new Canvas(800, 600);
        DrawingController controller = new DrawingController(canvas);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());

        Button btnRect = new Button("Rectangle");
        Button btnLine = new Button("Ligne");
        Button btnCircle = new Button("Cercle");
        
        Button btnUndo = new Button("↶");
        btnUndo.setTooltip(new Tooltip("Annuler (Ctrl+Z)"));
        btnUndo.setStyle("-fx-font-size: 14;");
        
        Button btnRedo = new Button("↷");
        btnRedo.setTooltip(new Tooltip("Rétablir (Ctrl+Y)"));
        btnRedo.setStyle("-fx-font-size: 14;");
        
        Button btnEraser = new Button("🗑");
        btnEraser.setTooltip(new Tooltip("Gomme"));
        btnEraser.setStyle("-fx-font-size: 14;");
        
        Button btnClear = new Button("Effacer");
        btnClear.setTooltip(new Tooltip("Effacer le dessin"));

        Button btnSaveDB = new Button("Enregistrer (DB)");
        Button btnLoadDB = new Button("Charger (DB)");

        ColorPicker colorPicker = new ColorPicker(Color.BLACK);

        ComboBox<String> logChoice = new ComboBox<>();
        logChoice.getItems().addAll("Console", "Fichier", "Base");
        logChoice.setValue("Console");

        DBConnection.testConnection();

        btnRect.setOnAction(e -> {
            controller.setShapeType("Rectangle");
            controller.enableDrawing();
            btnEraser.setStyle("-fx-font-size: 14;");
        });
        
        btnLine.setOnAction(e -> {
            controller.setShapeType("Line");
            controller.enableDrawing();
            btnEraser.setStyle("-fx-font-size: 14;");
        });
        
        btnCircle.setOnAction(e -> {
            controller.setShapeType("Circle");
            controller.enableDrawing();
            btnEraser.setStyle("-fx-font-size: 14;");
        });
        
        btnUndo.setOnAction(e -> controller.undo());
        btnRedo.setOnAction(e -> controller.redo());
        
        btnEraser.setOnAction(e -> {
            boolean isEraserActive = controller.isEraserActive();
            controller.setEraserActive(!isEraserActive);
            if (!isEraserActive) {
                btnEraser.setStyle("-fx-font-size: 14; -fx-base: #ffcccc;");
            } else {
                btnEraser.setStyle("-fx-font-size: 14;");
            }
        });
        
        btnClear.setOnAction(e -> {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText("Effacer le dessin");
            confirmation.setContentText("Êtes-vous sûr de vouloir effacer tout le dessin?");
            
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                controller.clearDrawing();
            }
        });
        
        btnSaveDB.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("Mon dessin");
            dialog.setTitle("Enregistrer le dessin");
            dialog.setHeaderText("Entrez un nom pour ce dessin");
            dialog.setContentText("Nom:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                String drawingName = result.get().trim();
                if (controller.saveToDatabase(drawingName)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Sauvegarde");
                    alert.setHeaderText(null);
                    alert.setContentText("Dessin '" + drawingName + "' sauvegardé avec succès dans la base de données.");
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setHeaderText(null);
                    alert.setContentText("Erreur lors de la sauvegarde dans la base de données.");
                    alert.showAndWait();
                }
            }
        });
        
        btnLoadDB.setOnAction(e -> {
            List<String> drawings = controller.getDrawingsList();
            if (drawings.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Information");
                alert.setHeaderText(null);
                alert.setContentText("Aucun dessin trouvé dans la base de données.");
                alert.showAndWait();
                return;
            }
            
            ChoiceDialog<String> dialog = new ChoiceDialog<>(drawings.get(0), drawings);
            dialog.setTitle("Charger un dessin");
            dialog.setHeaderText("Sélectionnez un dessin à charger");
            dialog.setContentText("Dessin:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String selected = result.get();
                int drawingId = Integer.parseInt(selected.split(" - ")[0]);
                
                if (controller.loadDrawingFromDatabase(drawingId)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Chargement");
                    alert.setHeaderText(null);
                    alert.setContentText("Dessin chargé avec succès depuis la base de données.");
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setHeaderText(null);
                    alert.setContentText("Erreur lors du chargement depuis la base de données.");
                    alert.showAndWait();
                }
            }
        });

        colorPicker.setOnAction(e -> controller.setColor(colorPicker.getValue()));

        logChoice.setOnAction(e -> {
            switch (logChoice.getValue()) {
                case "Fichier" -> controller.setLogger(new FileLogger());
                case "Base" -> controller.setLogger(new DatabaseLogger());
                default -> controller.setLogger(new ConsoleLogger());
            }
        });

        VBox canvasContainer = new VBox(canvas);
        canvasContainer.setStyle("-fx-border-color: #cccccc; -fx-border-width: 2;");
        canvasContainer.setPadding(new Insets(5));
        canvasContainer.setAlignment(Pos.CENTER);

        StackPane centerContainer = new StackPane(canvasContainer);
        centerContainer.setAlignment(canvasContainer, Pos.CENTER);

        ToolBar toolbar = new ToolBar(
                btnRect, btnLine, btnCircle,
                new Separator(),
                btnUndo, btnRedo,
                new Separator(),
                btnEraser,
                new Separator(),
                new Label("Couleur : "), colorPicker,
                new Separator(),
                new Label("Logger : "), logChoice,
                new Separator(),
                btnClear, btnSaveDB, btnLoadDB
        );

        setTop(toolbar);
        setCenter(centerContainer);
        setPadding(new Insets(10));

        controller.setShapeType("Rectangle");
        controller.setColor(Color.BLACK);
        controller.setLogger(new ConsoleLogger());
        controller.enableDrawing();
    }
}