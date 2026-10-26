package com.mbv;

import com.mbv.config.AppConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class MbvApplication extends Application {

    private static AnnotationConfigApplicationContext context;
    private static Stage primaryStage;

    public static void main(String[] args) {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/css/theme-blue.css").toExternalForm());

        stage.setTitle("MBV - Mail Backup Visualizer");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static AnnotationConfigApplicationContext getContext() {
        return context;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
