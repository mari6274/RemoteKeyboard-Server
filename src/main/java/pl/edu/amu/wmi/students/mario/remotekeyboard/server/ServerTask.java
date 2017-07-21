package pl.edu.amu.wmi.students.mario.remotekeyboard.server;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Created by Mariusz on 2017-07-18.
 */
public class ServerTask extends Task<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerTask.class);
    private ReadOnlyObjectWrapper<SERVER_STATE> serverState = new ReadOnlyObjectWrapper<>(SERVER_STATE.STOPPED);
    private int port;
    private Thread wakeUpThread = new Thread(new WakeUpRunnable());
    private DatagramSocket serverSocket;

    public ServerTask(int port) {
        this.port = port;
        serverState.addListener(
                (observable, oldValue, newValue) -> LOGGER.debug("State changed from {} to {}", oldValue, newValue));
    }

    public ReadOnlyObjectProperty<SERVER_STATE> serverStateProperty() {
        return serverState.getReadOnlyProperty();
    }

    @Override
    protected Void call() throws Exception {

        serverState.set(SERVER_STATE.STARTING);

        serverSocket = new DatagramSocket(port);
        byte[] buf = new byte[4];
        Robot robot = new Robot();

        startWakeUpThread();

        serverState.set(SERVER_STATE.STARTED);
        while (serverState.get() == SERVER_STATE.STARTED) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            serverSocket.receive(packet);
            int keyCode = ByteBuffer.wrap(packet.getData(), 0, packet.getLength()).getInt();
            LOGGER.debug("Received keyCode: {}", keyCode);
            if (keyCode == 0) {
                continue;
            }
            try {
                robot.keyPress(keyCode);
                robot.keyRelease(keyCode);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid key code: {}", keyCode);
            }
        }
        serverSocket.close();
        wakeUpThread.interrupt();
        return null;
    }

    private void startWakeUpThread() {
        wakeUpThread.setDaemon(true);
        wakeUpThread.start();
    }

    @Override
    protected void done() {
        serverState.set(SERVER_STATE.STOPPED);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        serverState.set(SERVER_STATE.STOPPING);
        return true;
    }

    @Override
    protected void setException(Throwable t) {
        super.setException(t);
        Optional.ofNullable(serverSocket).ifPresent(DatagramSocket::close);
        wakeUpThread.interrupt();
        LOGGER.error(t.getMessage(), t);
    }

    private class WakeUpRunnable implements Runnable {
        byte[] buf = ByteBuffer.allocate(4).putInt(0).array();

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(3000);
                    DatagramSocket socket = new DatagramSocket();
                    socket.send(new DatagramPacket(buf, 4, InetAddress.getLocalHost(), port));
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
