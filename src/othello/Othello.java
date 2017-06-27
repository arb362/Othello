/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othello;

import java.io.*;
import java.net.InetAddress;
import java.util.logging.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import othello.network.ClientEngine;
import othello.network.ServerEngine;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import othello.network.MultiplayerEngine;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

/**
 *
 * @author ablankenship
 */
public class Othello extends Application implements OthelloListener {

    private static final Logger LOGGER = Logger.getLogger(Othello.class.getName());
    private String gameID;
    private Board gameBoard;
    private MultiplayerEngine multiEngine = null;
    private boolean cancelAction = false;
    private Stage waitDialogStage = null;
    private Stage primaryStage;
    private VBox vButtons;

    @Override
    public void start(Stage pStage) {
        primaryStage = pStage;
        setupLogging();
        ScrollPane scrollPane = new ScrollPane();
        AnchorPane anchorPane = new AnchorPane();
        gameBoard = new Board(8, 8);
        AnchorPane.setTopAnchor(gameBoard, 5.0);
        AnchorPane.setBottomAnchor(gameBoard, 10.0);
        AnchorPane.setLeftAnchor(gameBoard, 5.0);
        AnchorPane.setRightAnchor(gameBoard, 150.0);

        vButtons = setupButtons(primaryStage);
        anchorPane.getChildren().addAll(gameBoard, vButtons);

        scrollPane.setContent(anchorPane);
        Scene scene = new Scene(scrollPane);

        primaryStage.setTitle("Othello!");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.sizeToScene();
        primaryStage.setMinWidth(primaryStage.getWidth());
        primaryStage.setMinHeight(primaryStage.getHeight());

    }

    /**
     * Make sure that everything is stopped before the window closes.
     *
     * @throws java.lang.Exception
     */
    @Override
    public void stop() throws Exception {

        if (multiEngine != null) {
            multiEngine.stopEngine();
        }
        if (waitDialogStage != null) {
            waitDialogStage.close();
        }
        super.stop();
    }

    /**
     * Sends the gameBoard playing field, disables the gameBoard, and then
     * immediately calls multiplayerReceive to wait for a response.
     */
    private void multiplayerSend(int c, int r) {
        multiEngine.setLastMove(c,r);
        multiEngine.setPlayingField(gameBoard.getPlayingField());
        gameBoard.disableProperty().set(true);
        multiEngine.send();
        if (waitDialog("Sending Data")) {
            LOGGER.info("Data Sent!");
            multiplayerReceive();
        }

    }

    /**
     * Makes sure the gameBoard is disabled(primarily for games where the player
     * is player 2) and then waits to receive data. As soon as it is received it
     * sets the gameBoard to this data.
     */
    private void multiplayerReceive() {
        gameBoard.disableProperty().set(true);
        if (waitDialog("Waiting to receive Data")) {
            LOGGER.info("Received Data");
            gameBoard.setLastMove(multiEngine.getLastMove());
            gameBoard.setPlayingField(multiEngine.getPlayingField());            
            gameBoard.disableProperty().set(false);
        }
    }

    /**
     * Calling this method will setup the multiEngine based on whether this is a
     * server/client configuration.
     */
    private void multiSetup() {
        List<String> list = new ArrayList<>();
        list.add("Host Local");
        list.add("Join Local");
        list.add("Join Remote");
        ChoiceDialog<String> cd = new ChoiceDialog("Host Local", list);
        Optional<String> result = cd.showAndWait();
        if (result.isPresent()) {
            if ("Host Local".equals(result.get())) {
                //Checks to see if host is setup correctly
                if (setupHost()) {
                    //Host is always player 1
                    gameBoard.setMultiplayer(1);
                    //Make sure to get notified of any turn changes
                    gameBoard.getNotificationListeners().add(this);
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setContentText("Successfully connected to Client");
                    a.show();
                    LOGGER.finest("Hosting setup successfully!");
                } else {
                    //If not setup correctly, set engine to null
                    cancelAction = false;
                    multiEngine = null;
                }

            } else if (result.get().contains("Join")) {
                //Make sure client is setup correctly
                if (setupClient(result.get())) {

                    int turn = ((ClientEngine) multiEngine).getPlayerID();
                    //Sets player turn based on information that it is returned
                    //immediately after connection.
                    gameBoard.setPlayerTurn(turn);
                    //Sets the player value for multiplayer
                    gameBoard.setMultiplayer(turn);
                    gameBoard.getNotificationListeners().add(this);
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setContentText("Successfully connected to Server");
                    a.show();
                    LOGGER.finest("Joined successfully!");
                    if (turn > 1) {
                        //If this is player 2 immediately start waiting for data
                        multiplayerReceive();
                    }

                } else {
                    cancelAction = false;
                    multiEngine = null;
                }

            }

        }
    }

    /**
     * Sets up a client
     *
     * @param choice from the response dialog
     * @return Whether it was successful or not
     */
    private boolean setupClient(String choice) {

        TextInputDialog tid = new TextInputDialog();
        tid.setContentText("Enter the website/IP address you would like"
                + "to connect to.");
        Optional<String> hostName = tid.showAndWait();

        if (hostName.isPresent()) {
            TextInputDialog tid2 = new TextInputDialog();
            tid2.setContentText("Enter your game ID");
            Optional<String> gameCode = tid2.showAndWait();
            gameCode.ifPresent(e -> gameID = e);
            if (gameID.isEmpty()) {
                return false;
            }
            try {
                URI hosturi;
                if (choice.contains("Local")) {
                    //Locally it will be hosted on port 10101
                    hosturi = new URI("ws://" + hostName.get() + ":10101");
                } else {
                    //Remotely the user can add a port
                    hosturi = new URI("ws://" + hostName.get());
                }
                multiEngine = new ClientEngine(hosturi, gameID);
                //Notify if data sent/received
                multiEngine.getOthelloListeners().add(this);
                waitDialog("Trying to connect...");//, a);
                return !cancelAction;
            } catch (URISyntaxException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                return !cancelAction;
            }
        }
        return !cancelAction;

    }

    /**
     * Configures a host
     *
     * @return Whether it was successful or not
     */
    private boolean setupHost() {

        gameID = String.valueOf(new Random().nextInt(9000) + 1000);
        LOGGER.finest(gameID);

        try {
            //Host it on local ip address with port 10101
            multiEngine = new ServerEngine(
                    new InetSocketAddress(10101), gameID);
            //Notify if data sent or received
            multiEngine.getOthelloListeners().add(this);
            String ip = InetAddress.getLocalHost().getHostAddress();

            LOGGER.finest("Waiting for connection!");
            waitDialog("Tell your friend to "
                    + "use this gameID: " + gameID
                    + "\n\nIP Address is " + ip + "\n\n"
                    + " \nWaiting for a client to connect...");
            LOGGER.finest("Done");
            return !cancelAction;
        } catch (Exception ex) {
            multiEngine.stopEngine();
            return !cancelAction;
        }

    }

    /**
     * Setups logging
     */
    private void setupLogging() {
        Handler consoleHandler = new ConsoleHandler();
        Handler fileHandler;
        try {
            Formatter simpleFormatter = new SimpleFormatter();
            fileHandler = new FileHandler("./othello.log");
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

    /**
     * This is a dialog that is displayed conveying information with a cancel
     * button. The primary use of this is for processes that will be running in
     * the background and you want to have the ability to cancel them.
     *
     * @param text Text to be displayed
     * @return Whether or not it was canceled. If it was canceled returns false
     * else true
     */
    private boolean waitDialog(String text) {

        waitDialogStage = new Stage();
        StackPane cancelPane = new StackPane();
        Label lbl = new Label(text);
        Button btn = new Button("Cancel");
        btn.setOnAction((ActionEvent e) -> {
            multiEngine.stopEngine();
            waitDialogStage.close();
            gameBoard.disableProperty().set(false);
            cancelAction = true;
        });
        cancelPane.getChildren().addAll(lbl, btn);
        StackPane.setAlignment(btn, Pos.BOTTOM_CENTER);
        Scene cancelScene = new Scene(cancelPane, 250, 250, Color.TRANSPARENT);

        waitDialogStage.setScene(cancelScene);
        waitDialogStage.setX(vButtons.localToScreen(vButtons.getBoundsInLocal()).getMinX() - 10);
        waitDialogStage.setY(primaryStage.getY());

        waitDialogStage.setOnCloseRequest(e -> {
            System.out.println("Closing");
            System.out.println(e.getSource());
            System.out.println(e.getTarget());
            cancelAction = true;
            multiEngine.stopEngine();
            gameBoard.disableProperty().set(false);
        });

        waitDialogStage.setAlwaysOnTop(true);

        waitDialogStage.showAndWait();
        return !cancelAction;

    }

    @Override
    public void onSent() {
        //Delay so we can close it using any thread       
        Platform.runLater(() -> {
            waitDialogStage.close();
        });

    }

    @Override
    public void onReceived() {
        //Delay so we can close it using any thread        
        Platform.runLater(() -> {
            waitDialogStage.close();
        });
    }

    @Override
    public void onTurnComplete(int c, int r) {
        multiplayerSend(c,r);
    }

    /**
     * Saves the playing board
     */
    private void saveGame() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save your game....");
        fc.setInitialFileName("Othello1.otsav");
        FileChooser.ExtensionFilter filter
                = new FileChooser.ExtensionFilter("Saved Othello Games (*.otsav)", "*.otsav");
        fc.getExtensionFilters().add(filter);
        File saveFile = fc.showSaveDialog(waitDialogStage);
        if (saveFile != null) {

            try {
                ObjectOutput oos = new ObjectOutputStream(
                        new FileOutputStream(saveFile));
                oos.writeObject(gameBoard.getPlayingField());
                oos.flush();
                oos.close();
            } catch (FileNotFoundException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setContentText("File not found.");
                a.show();
                LOGGER.log(Level.SEVERE, null, ex);

            } catch (IOException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setContentText("Can't Save File.");
                a.show();
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

    }

    /**
     * Loads the previously saved playing board.
     */
    private void loadGame() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load your game....");
        fc.setInitialFileName("Othello1.otsav");
        FileChooser.ExtensionFilter filter
                = new FileChooser.ExtensionFilter("Saved Othello Games (*.otsav)", "*.otsav");
        fc.getExtensionFilters().add(filter);
        File loadFile = fc.showOpenDialog(waitDialogStage);
        if (loadFile != null) {

            try {
                try (ObjectInput ois = new ObjectInputStream(
                        new FileInputStream(loadFile))) {
                    int[][] tempPlayingField = ((int[][]) ois.readObject());
                    gameBoard.clear(tempPlayingField.length, tempPlayingField.length);
                    gameBoard.setPlayingField(tempPlayingField);
                    Stage primaryStage = (Stage) (gameBoard.getScene().getWindow());
                    primaryStage.sizeToScene();
                    primaryStage.setMinWidth(primaryStage.getWidth());
                    primaryStage.setMinHeight(primaryStage.getHeight());
                }
            } catch (FileNotFoundException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setContentText("File not found.");
                a.show();
                LOGGER.log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setContentText("Can't Load File.");
                a.show();
                LOGGER.log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setContentText("Can't Load File.");
                a.show();
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

    }

    /**
     * Creates the Pane where the score will be displayed
     *
     * @return BorderPane which has the score bound to the gameBoard
     */
    private BorderPane setupScorePane() {

        BorderPane scorePane = new BorderPane();;
        Label p1 = new Label("P1");
        Label p2 = new Label("P2");
        Label score = new Label("Score");
        Label lblScoreP1 = new Label("help");

        lblScoreP1.textProperty().bind(gameBoard.getPlayerOneScore().asString());
        Label lblScoreP2 = new Label("me");
        lblScoreP2.textProperty().bind(gameBoard.getPlayerTwo().asString());
        BorderPane BPtop = new BorderPane();
        BorderPane BPmiddle = new BorderPane();
        BPtop.setLeft(p1);
        BPtop.setCenter(score);
        BPtop.setRight(p2);
        BPmiddle.setLeft(lblScoreP1);
        BPmiddle.setRight(lblScoreP2);
        scorePane.setTop(BPtop);
        scorePane.setBottom(BPmiddle);

        return scorePane;
    }

    /**
     * Sets up a VBox with the buttons for the playing field
     *
     * @param primaryStage
     * @return VBox with buttons and score setup
     */
    private VBox setupButtons(Stage primaryStage) {

        BorderPane scorePane = setupScorePane();
        VBox vButtons = new VBox();
        CheckBox cbGraphics = new CheckBox("Alternate Graphics");
        cbGraphics.selectedProperty().set(
                gameBoard.AlternateGraphicsProperty().get());
        cbGraphics.setOnAction((event) -> {
            gameBoard.AlternateGraphicsProperty().set(
                    cbGraphics.selectedProperty().get());
            gameBoard.setPlayingField(gameBoard.getPlayingField());
        });
        Label lblPlayer = new Label("Player Turn:");

        lblPlayer.textProperty().set(gameBoard.isAlternateGraphics()
                ? "White is 1\nBlack is 2\nPlayer Turn:"
                : "Player Turn"
        );
        Label lblTurn = new Label("");
        lblTurn.textProperty().bind(gameBoard.getPlayerTurn().asString());

        Button btnAdd = new Button("New");
        Button btnSave = new Button("Save");
        Button btnLoad = new Button("Load");
        Button btnMulti = new Button("Multiplayer");
        Button btnPass = new Button("Pass");

        btnAdd.setOnMouseClicked((MouseEvent e) -> {
            TextInputDialog tid = new TextInputDialog();
            tid.setGraphic(null);
            tid.setHeaderText("Please enter the number of rows "
                    + "and columns you would like.\n(No larger than 30"
                    + ", no smaller than 4 and divisible by 2)\n"
                    + "Warning! Larger than 20 and it may start lagging!");
            tid.setTitle("Number of rows and columns");
            tid.showAndWait();
            int results = Integer.parseInt(tid.resultProperty().getValue());
            if (results <= 30 && results >= 4 && results % 2 == 0) {
                LOGGER.info("reset button pressed.");
                gameBoard.clear(results, results);
            } else {
                btnAdd.fireEvent(e);
            }
            primaryStage.sizeToScene();
            primaryStage.setMinWidth(primaryStage.getWidth());
            primaryStage.setMinHeight(primaryStage.getHeight());
        });

        btnSave.setOnAction(e -> {
            saveGame();
        });

        btnLoad.setOnAction(e -> {
            loadGame();
        });

        btnMulti.setOnAction(e -> {
            multiSetup();
        });

        btnPass.setOnMouseClicked(e -> {
            gameBoard.nextPlayer(-1,-1);
        });

        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnLoad.setMaxWidth(Double.MAX_VALUE);
        btnMulti.setMaxWidth(Double.MAX_VALUE);
        btnPass.setMaxWidth(Double.MAX_VALUE);

        vButtons.setSpacing(10);
        vButtons.setPadding(new Insets(0, 20, 0, 0));
        vButtons.getChildren().addAll(lblPlayer, lblTurn, btnAdd, btnSave,
                btnLoad, btnMulti, btnPass, scorePane, cbGraphics);
        AnchorPane.setRightAnchor(vButtons, 5.0);
        AnchorPane.setBottomAnchor(vButtons, 5.0);
        AnchorPane.setTopAnchor(vButtons, 5.0);
        return vButtons;
    }

    @Override
    public void setTurn(int player) {
        gameBoard.setMultiplayer(player);
    }

    @Override
    public void onSetup() {
        Platform.runLater(() -> {
            waitDialogStage.close();
        });
    }

    /**
     * Notify user if multiplayer is disconnected
     */
    @Override
    public void onDisconnect() {
        cancelAction = true;
        multiEngine.stopEngine();
        gameBoard.disableProperty().set(false);
        Platform.runLater(() -> {
            waitDialogStage.close();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.contentTextProperty().set("Multiplayer Disconnected.");
            a.show();

        });
    }

    /**
     * Used so that the IDE can launch it.
     *
     * @param args
     */
    public static void main(String[] args) {
        launch(args);
    }
}
