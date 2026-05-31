package com.iae;

import com.iae.controller.MainController;
import com.iae.repository.ConfigurationRepository;
import com.iae.service.ConfigurationService;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    private static final long MIN_SPLASH_VISIBLE_MS = 1400;

    @Override
    public void start(Stage primaryStage) {
        SplashScreen splash = new SplashScreen();
        splash.show();

        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Initializing database...");
                new ConfigurationRepository();
                Thread.sleep(220);

                updateMessage("Loading configurations...");
                new ConfigurationService().seedDefaultsIfEmpty();
                Thread.sleep(220);

                updateMessage("Preparing workspace...");
                Thread.sleep(220);
                return null;
            }
        };

        initTask.messageProperty().addListener((obs, oldMsg, msg) -> splash.updateStatus(msg));
        initTask.setOnSucceeded(event -> finishStartup(primaryStage, splash));
        initTask.setOnFailed(event -> {
            Throwable ex = initTask.getException();
            String message = (ex != null && ex.getMessage() != null)
                    ? ex.getMessage()
                    : "Unknown error during startup.";
            splash.closeWithFade(() -> {
                showFatalError(message);
                Platform.exit();
            });
        });

        Thread initThread = new Thread(initTask, "iae-init");
        initThread.setDaemon(true);
        initThread.start();
    }

    private void finishStartup(Stage stage, SplashScreen splash) {
        try {
            splash.updateStatus("Loading interface...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            Scene scene = new Scene(root, 960, 620);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            stage.setTitle("CE 316 - Assignment Grader");
            try {
                stage.getIcons()
                        .add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/app-icon.png")));
            } catch (Exception ignore) {
            }
            stage.setScene(scene);
            stage.setMinWidth(880);
            stage.setMinHeight(560);
            stage.setOnCloseRequest(closeEvt -> {
                if (!controller.requestClose()) {
                    closeEvt.consume();
                }
            });

            splash.updateStatus("Ready.");

            long extraDelay = Math.max(0, MIN_SPLASH_VISIBLE_MS - splash.elapsedMillis());
            PauseTransition hold = new PauseTransition(Duration.millis(extraDelay));
            hold.setOnFinished(evt -> splash.closeWithFade(stage::show));
            hold.play();
        } catch (Exception ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            splash.closeWithFade(() -> {
                showFatalError(message);
                Platform.exit();
            });
        }
    }

    private void showFatalError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("CE 316 IAE");
        alert.setHeaderText("The application could not start.");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
