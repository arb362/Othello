/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othello.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import othello.Board;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import othello.OthelloListener;

/**
 *
 * @author ablankenship
 */
public class ServerEngine extends WebSocketServer implements MultiplayerEngine {

    private static final Logger LOGGER = Logger.getLogger(ServerEngine.class.getName());
    private String gameID;
    private WebSocket gamePlayerConn;
    private int[][] playingField;
    private boolean msgReceived = false;
    private boolean opened = false;
    private boolean connected = false;
    private final ArrayList<OthelloListener> othelloListeners = new ArrayList<>();
    private Object[] exchangeValues = new Object[2];
    private int[] lastMove;

    public ServerEngine(InetSocketAddress address, String gameID) {
        super(address);
        setupLogging();
        this.gameID = gameID;
        this.start();
        LOGGER.finest("Started with gameID");
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

        opened = true;

    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        opened = false;
        notifyDisconnect();
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer bytes) {
        prepareDataReceive(bytes);
        notifyReceived();

    }

    @Override
    public void onMessage(WebSocket conn, String text) {
        LOGGER.info("Received string message");
        LOGGER.info(text);
        LOGGER.info(text.substring(0, text.indexOf(":")));
        LOGGER.info(gameID);
        LOGGER.info(String.valueOf(text.substring(0, text.indexOf(":")).equals(gameID)));
        if (text.substring(0, text.indexOf(":")).equals(gameID)) {
            gamePlayerConn = conn;
            conn.send("Connection:true");
            //Host is always #1 and joiner is always #2
            conn.send("Player:2");
            setupCompleted();

        }

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

        try {
            throw ex;
        } catch (Exception ex1) {
            Logger.getLogger(ServerEngine.class.getName()).log(Level.SEVERE, null, ex1);
        }
    }

    @Override
    public void send() {
        gamePlayerConn.send(prepareDataSend());
        notifySent();

    }

    @Override
    public int[][] getPlayingField() {
        return playingField;
    }

    @Override
    public void setPlayingField(int[][] pf) {
        this.playingField = pf;
    }

    private byte[] prepareDataSend() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] ret_value = null;
        try {
            ObjectOutput out = new ObjectOutputStream(bos);
            exchangeValues[0] = playingField;
            exchangeValues[1] = lastMove;
            out.writeObject(exchangeValues);
            out.flush();
            ret_value = bos.toByteArray();

        } catch (IOException ex) {
            Logger.getLogger(Board.class
                    .getName()).log(Level.SEVERE, null, ex);
            return null;
        } finally {
            try {
                bos.close();

            } catch (IOException ex) {
                Logger.getLogger(Board.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        return ret_value;
    }

    private void prepareDataReceive(ByteBuffer data) {
        byte[] barray = new byte[data.remaining()];
        data.get(barray);
        ByteArrayInputStream bin = new ByteArrayInputStream(barray);
        try {
            ObjectInputStream in = new ObjectInputStream(bin);
            exchangeValues = (Object[]) in.readObject();
            playingField = (int[][]) exchangeValues[0];
            lastMove = (int[]) exchangeValues[1];

        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(ClientEngine.class
                    .getName()).log(Level.SEVERE, null, ex);
            return;

        }
        return;
    }

    @Override
    public void stopEngine() {
        try {
            this.stop(1);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

    }

    private void setupLogging() {
        Handler consoleHandler = new ConsoleHandler();
        Handler fileHandler;
        try {
            Formatter simpleFormatter = new SimpleFormatter();
            fileHandler = new FileHandler("./sci.log");
            fileHandler.setFormatter(simpleFormatter);
            LOGGER.addHandler(consoleHandler);
            LOGGER.addHandler(fileHandler);
            consoleHandler.setLevel(Level.ALL);
            fileHandler.setLevel(Level.ALL);
            LOGGER.setLevel(Level.ALL);

        } catch (IOException | SecurityException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        LOGGER.config("Configuration done.");
    }

    @Override
    public void notifySent() {
        othelloListeners.forEach((mn) -> {
            mn.onSent();
        });
    }

    @Override
    public void notifyReceived() {
        othelloListeners.forEach((mn) -> {
            mn.onReceived();
        });
    }

    @Override
    public ArrayList<OthelloListener> getOthelloListeners() {
        return othelloListeners;
    }

    private void setupCompleted() {
        othelloListeners.forEach((mn) -> {
            mn.onSetup();
        }
        );
    }

    private void notifyDisconnect() {
        othelloListeners.forEach((mn) -> {
            mn.onDisconnect();
        });
    }

    @Override
    public void setLastMove(int column, int row) {
        int[] move = {column, row};
        lastMove = move;
    }

    @Override
    public int[] getLastMove() {
        return lastMove;
    }

}
