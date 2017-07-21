package pl.edu.amu.wmi.students.mario.remotekeyboard.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.ResourceBundle;

/**
 * Created by Mariusz on 2017-07-18.
 */
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        ResourceBundle resourceBundle = ResourceBundle.getBundle(getClass().getPackage().getName() + ".remotekeyboard");
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("RemoteKeyboard.fxml"), resourceBundle);
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("RemoteKeyboard");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("icon.png")));
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
