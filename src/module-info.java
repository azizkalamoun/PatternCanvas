module Final {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    requires java.sql;
    
    exports main;
    exports controller;
    exports command;
    exports model;
    exports view;
    exports logger;
}