package com.iae;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Lightweight, transparent splash window shown while {@link Main} performs
 * its one-time initialization. The visible status label tracks the
 * background task so the user has feedback during the brief startup.
 */
public class SplashScreen {

    private static final double WIDTH = 520;
    private static final double HEIGHT = 340;

    private final Stage stage;
    private final StackPane root;
    private final Label statusLabel;
    private final long startedAtMs;

    public SplashScreen() {
        this.startedAtMs = System.currentTimeMillis();

        Label brand = new Label("CE 316 IAE");
        brand.getStyleClass().add("splash-title");

        Label tagline = new Label("Integrated Assignment Environment");
        tagline.getStyleClass().add("splash-subtitle");

        Region divider = new Region();
        divider.getStyleClass().add("splash-divider");
        divider.setPrefSize(96, 3);
        divider.setMaxSize(96, 3);
        divider.setMinSize(96, 3);

        VBox header = new VBox(10, brand, tagline);
        header.setAlignment(Pos.CENTER);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(320);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.getStyleClass().add("splash-progress");

        statusLabel = new Label("Starting...");
        statusLabel.getStyleClass().add("splash-status");

        Label version = new Label("v1.0.0");
        version.getStyleClass().add("splash-version");

        VBox footer = new VBox(10, progressBar, statusLabel, version);
        footer.setAlignment(Pos.CENTER);

        VBox card = new VBox(28, header, divider, footer);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("splash-card");
        card.setPrefSize(WIDTH - 40, HEIGHT - 40);

        root = new StackPane(card);
        root.getStyleClass().add("splash-root");
        root.setPrefSize(WIDTH, HEIGHT);

        Scene scene = new Scene(root, WIDTH, HEIGHT, Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        stage = new Stage(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("CE 316 IAE");
    }

    public void show() {
        root.setOpacity(0);
        stage.show();
        stage.centerOnScreen();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(320), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    public void updateStatus(String text) {
        statusLabel.setText(text == null ? "" : text);
    }

    public void closeWithFade(Runnable onClosed) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(260), root);
        fadeOut.setFromValue(root.getOpacity());
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            stage.close();
            if (onClosed != null) {
                onClosed.run();
            }
        });
        fadeOut.play();
    }

    public long elapsedMillis() {
        return System.currentTimeMillis() - startedAtMs;
    }
}
