package othello.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.java_websocket.WebSocketImpl;
import othello.Board;
import org.java_websocket.client.WebSocketClient;

import org.java_websocket.handshake.ServerHandshake;
import othello.OthelloListener;

public class ClientEngine extends WebSocketClient implements MultiplayerEngine {

    private static final Logger LOGGER = Logger.getLogger(ClientEngine.class.getName());

    private String gameID;
    private int[][] playingField;
    private boolean validGameID = false;
    private int playerID = 0;
    private int[] lastMove;
    private final ArrayList<OthelloListener> othelloListeners = new ArrayList<>();
    private Object[] exchangeValues = new Object[2];

    public ClientEngine(URI serverURI, String gameID) {
        super(serverURI);
        setupLogging();
        this.gameID = gameID;
        this.connect();
        WebSocketImpl.DEBUG = true;
        LOGGER.info(this.getDraft().toString());

    }

    @Override
    public void send() {

        send(prepareDataSend());
        notifySent();

    }

    public int getPlayerID() {
        return playerID;
    }

    @Override
    public void setPlayingField(int[][] playingField) {
        this.playingField = playingField;
    }

    @Override
    public int[][] getPlayingField() {
        return playingField;
    }

    public byte[] prepareDataSend() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] ret_value = null;
        try {
            ObjectOutput out = new ObjectOutputStream(bos);
            exchangeValues[0]=playingField;
            exchangeValues[1]=lastMove;
            out.writeObject(exchangeValues);
            out.flush();
            ret_value = bos.toByteArray();

        } catch (IOException ex) {
            Logger.getLogger(Board.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                Logger.getLogger(Board.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return ret_value;
    }

    public void prepareDataReceive(ByteBuffer data) {
        byte[] barray = new byte[data.remaining()];
        data.get(barray);
        ByteArrayInputStream bin = new ByteArrayInputStream(barray);
        try {
            ObjectInputStream in = new ObjectInputStream(bin);
            exchangeValues = (Object[])in.readObject();
            playingField = (int[][]) exchangeValues[0];
            lastMove = (int[]) exchangeValues[1];
        } catch (IOException | ClassNotFoundException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return;
        }
        return;
    }

    /**
     *
     * @param handshakedata
     */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LOGGER.info("Sent message");
        String message = String.valueOf(gameID) + ":Othello";
        send(message);

    }

    @Override
    public void onMessage(String message) {
        LOGGER.severe("onmessage");
        LOGGER.severe(message);
        String[] messageParts = message.split(":");
        switch (messageParts[0]) {
            case "Connection":
                if (messageParts[1].equals("true")) {
                    validGameID = true;

                }
                break;
            case "Player":
                playerID = Integer.valueOf(messageParts[1]);
                setupCompleted();
                break;

        }

    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        prepareDataReceive(bytes);
        System.out.print("Last Move: ");
        System.out.println(lastMove);
        notifyReceived();

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.severe("onclose");
        LOGGER.severe(reason);
        notifyDisconnect();

    }

    @Override
    public void onError(Exception ex) {
        LOGGER.severe("error");
        LOGGER.severe(ex.toString());
        LOGGER.severe(Arrays.toString(ex.getStackTrace()));

    }

    @Override
    public void stopEngine() {
        this.close();
    }

    private void setupLogging() {
        Handler consoleHandler = new ConsoleHandler();
        Handler fileHandler;
        try {
            Formatter simpleFormatter = new SimpleFormatter();
            fileHandler = new FileHandler("./cci.log");
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
