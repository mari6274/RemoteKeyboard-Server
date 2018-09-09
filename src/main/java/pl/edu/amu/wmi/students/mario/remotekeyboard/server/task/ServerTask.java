package pl.edu.amu.wmi.students.mario.remotekeyboard.server.task;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.InputEvent;
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
    private static final int BUFFER_SIZE = 9;
    private final ReadOnlyObjectWrapper<ServerState> serverState = new ReadOnlyObjectWrapper<>(ServerState.STOPPED);
    private final int port;
    private final Thread wakeUpThread = new Thread(new WakeUpRunnable());
    private final Robot robot;
    private DatagramSocket serverSocket;

    public ServerTask(int port) {
        this.port = port;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
        serverState.addListener(
                (observable, oldValue, newValue) -> LOGGER.debug("State changed from {} to {}", oldValue, newValue));
    }

    public ReadOnlyObjectProperty<ServerState> serverStateProperty() {
        return serverState.getReadOnlyProperty();
    }

    @Override
    protected Void call() throws Exception {

        serverState.set(ServerState.STARTING);

        serverSocket = new DatagramSocket(port);
        byte[] buf = new byte[BUFFER_SIZE];

        startWakeUpThread();

        serverState.set(ServerState.STARTED);
        while (serverState.get() == ServerState.STARTED) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            serverSocket.receive(packet);
            handlePacketData(packet);
        }
        serverSocket.close();
        wakeUpThread.interrupt();
        return null;
    }

    private void handlePacketData(DatagramPacket packet) {
        byte mark = ByteBuffer.wrap(packet.getData(), 0, 1).get();
        switch (mark) {
            case PacketTypes.MOUSE_MOVE:
                int x = ByteBuffer.wrap(packet.getData(), 1, 4).getInt();
                int y = ByteBuffer.wrap(packet.getData(), 5, 4).getInt();
                Point location = MouseInfo.getPointerInfo().getLocation();
                robot.mouseMove(x + ((int) Math.round(location.getX())), y + ((int) Math.round(location.getY())));
                break;
            case PacketTypes.MOUSE_CLICK:
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                break;
            case PacketTypes.KEY_CODE:
                int keyCode = ByteBuffer.wrap(packet.getData(), 1, 4).getInt();
                try {
                    robot.keyPress(keyCode);
                    robot.keyRelease(keyCode);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid key code: {}", keyCode);
                }
        }
    }

    private void startWakeUpThread() {
        wakeUpThread.setDaemon(true);
        wakeUpThread.start();
    }

    @Override
    protected void done() {
        serverState.set(ServerState.STOPPED);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        serverState.set(ServerState.STOPPING);
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
        byte[] buf = ByteBuffer.allocate(BUFFER_SIZE).put(PacketTypes.WAKE_UP).array();

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(3000);
                    DatagramSocket socket = new DatagramSocket();
                    socket.send(new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(), port));
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
