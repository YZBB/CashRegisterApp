package ba.unsa.etf.si.utility.javafx;

import ba.unsa.etf.si.App;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import java.util.Optional;

import static ba.unsa.etf.si.App.primaryStage;

public class StageUtils {

    private StageUtils() {}

    public static Rectangle2D getScreenBounds() {
        return Screen.getPrimary().getBounds();
    }

    public static void centerStage(Stage stage, int width, int height) {
        stage.setWidth(width);
        stage.setHeight(height);
        Rectangle2D primScreenBounds = getScreenBounds();
        stage.setX((primScreenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((primScreenBounds.getHeight() - stage.getHeight()) / 2);
    }

    public static void setStage(Stage stage, String title, boolean resizable, StageStyle stageStyle, Modality modality) {
        stage.setTitle(title);
        stage.setResizable(resizable);
        stage.initStyle(stageStyle);
        stage.initModality(modality);
    }

    public static void setStageDimensions(Stage stage) {
        Rectangle2D bounds = getScreenBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    public static Optional<ButtonType> showAlert(String title, String header, Alert.AlertType alertType, ButtonType... buttonTypes) {
        Alert alert = new Alert(alertType, "", buttonTypes);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.getDialogPane().getStylesheets().add(App.class.getResource("css/alert.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        return alert.showAndWait();
    }

    public static Timeline setAnimation(Parent root, EventHandler<ActionEvent> handler) {
        Timeline timeline = new Timeline();
        KeyValue kv = new KeyValue(root.translateYProperty(), 0, Interpolator.EASE_IN);
        KeyFrame kf = new KeyFrame(Duration.seconds(1), kv);
        timeline.getKeyFrames().add(kf);
        timeline.setOnFinished(handler);
        return timeline;
    }

    public static void showNotification(Pos pos, String title, String text, int duration) {
        Notifications.create().position(pos).owner(primaryStage).title(title).text(text).hideCloseButton().hideAfter(Duration.seconds(duration)).showInformation();
    }
}
