package pl.edu.amu.wmi.students.mario.remotekeyboard.server;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import pl.edu.amu.wmi.students.mario.remotekeyboard.server.task.ServerState;
import pl.edu.amu.wmi.students.mario.remotekeyboard.server.task.ServerTask;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * @author Mariusz Mączkowski
 */
public class RemoteKeyboardController implements Initializable {
    @FXML
    private GridPane gridPane;
    @FXML
    private Label ipLabel;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private Spinner<Integer> portSpinner;
    private ObjectProperty<ServerState> serverState = new SimpleObjectProperty<>(
            ServerState.STOPPED);
    private ServerTask serverTask;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            List<String> adresses = Arrays.stream(InetAddress.getAllByName(InetAddress.getLocalHost()
                    .getCanonicalHostName()))
                    .filter(Inet4Address.class::isInstance)
                    .map(InetAddress::getHostAddress)
                    .collect(Collectors.toList());
            ipLabel.setText(String.join("\n", adresses));
            startButton.disableProperty().bind(Bindings.createBooleanBinding(
                    () -> serverState.get() != ServerState.STOPPED, serverState));
            stopButton.disableProperty().bind(Bindings.createBooleanBinding(
                    () -> serverState.get() != ServerState.STARTED, serverState));
        } catch (UnknownHostException e) {
            ipLabel.setText(resources.getString("unknown"));
            gridPane.setDisable(true);
        }
    }

    @FXML
    private void handleStartButton() {
        serverTask = new ServerTask(portSpinner.getValue());
        serverState.bind(serverTask.serverStateProperty());
        Thread serverThread = new Thread(serverTask);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @FXML
    private void handleStopButton() {
        serverTask.cancel();
    }
}
