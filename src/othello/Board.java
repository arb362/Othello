package othello;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Shape;
import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author ablankenship
 */
public class Board extends GridPane {

    private static final Logger LOGGER = Logger.getLogger(Board.class.getName());
    private int numRows;
    private int numColumns;
    private int[][] playingField;
    private final int numPlayers = 2;
    private boolean validMove = false;
    private final BooleanProperty alternateGraphics = new SimpleBooleanProperty();
    //Player turn is used to bind the value for the label 
    private final IntegerProperty playerTurn = new SimpleIntegerProperty();
    private final IntegerProperty playerOneScore = new SimpleIntegerProperty();
    private final IntegerProperty playerTwoScore = new SimpleIntegerProperty();
    //Multiplayer stores the value of the player if it is multiplayer
    //Doing this allows us to always set the multiplayer turn without 
    //having to signal any listeners. 
    private int multiplayer = 0;
    private final ArrayList<OthelloListener> notifyTurnList = new ArrayList<>();
    private Image whiteImg;
    private Image blackImg;
    private int passCounter;
    private int[] lastMove;

    /**
     * Create a Board node.
     *
     * @param numRows Number of rows
     * @param numColumns Number of columns
     */
    public Board(int numRows, int numColumns) {
        setupLogging();
        this.numRows = numRows;
        this.numColumns = numColumns;
        alternateGraphics.set(true);
        setupImages();
        setupBoard();

    }

    /**
     * Notify each listener on turn completed.
     */
    public void notifyDone(int c, int r) {
        notifyTurnList.forEach((on) -> {
            on.onTurnComplete(c, r);
        });

    }
    
    

    /**
     * Retrieve the list of objects wanting to be notified about events
     * happening inside of the game.
     *
     * @return List of objects wanting to be notified about events
     */
    public ArrayList<OthelloListener> getNotificationListeners() {
        return notifyTurnList;
    }

    /**
     *
     * @return AlternateGraphicsProperty
     */
    public final BooleanProperty AlternateGraphicsProperty() {
        return alternateGraphics;
    }

    /**
     * Shorthand for getting if there is or isn't AlternateGraphics.
     *
     * @return True or False
     */
    public boolean isAlternateGraphics() {
        return alternateGraphics.get();
    }

    /**
     * Set which player this instance will be.
     *
     * @param multiplayer player number that this instance is going to be in a
     * multiplayer match
     */
    public void setMultiplayer(int multiplayer) {
        if (multiplayer <= numPlayers && multiplayer >= 0) {
            this.multiplayer = multiplayer;
        }
    }

    /**
     * Draw the board to System.out for debugging purposes.
     *
     */
    private void drawBoard() {
        for (int col = 0; col < numColumns; col++) {
            for (int row = 0; row < numRows; row++) {
                System.out.print((playingField[col][row]));
                System.out.print(" ");
            }
            System.out.println("");
        }
        System.out.println(lastMove);
    }

    /**
     * Get playerTurnProperty
     *
     * @return playerTurn Property
     */
    public IntegerProperty getPlayerTurn() {
        return playerTurn;
    }

    /**
     * Get PlayerOne Score
     *
     * @return playerOneScore property
     */
    public IntegerProperty getPlayerOneScore() {
        return playerOneScore;
    }

    /**
     * Get PlayerTwoScore
     *
     * @return playerTwoScore property
     */
    public IntegerProperty getPlayerTwo() {
        return playerTwoScore;
    }

    /**
     * 1)Increment the player to the next one. 2)Notify everyone interested in a
     * a turn completion. 3)Check if both people have passed twice. 4)If it's a
     * multiplayer game, reset the player to the preset player
     */
    public void nextPlayer(int c, int r) {
        this.playerTurn.setValue(playerTurn.get() + 1);
        if (this.playerTurn.get() > numPlayers) {
            this.playerTurn.setValue(1);
        }
        int[] lmove ={c,r};
        setLastMove(lmove);
        if(multiplayer ==0){
            showLastMove();
        }
        notifyDone(c, r);
        
        
        

        passCounter += 1;
        if (passCounter == 4) {
            gameOver();
        }
        if (multiplayer > 0) {
            setPlayerTurn(multiplayer);
        }

    }

    /**
     *
     * @param num Player turn it is supposed to currently be.
     */
    public void setPlayerTurn(int num) {
        if (num > numPlayers) {
            return;
        } else {
            this.playerTurn.setValue(num);
        }
    }

    /**
     *
     * @return int[][] of playing field
     */
    public int[][] getPlayingField() {
        return playingField;
    }

    /**
     *
     * @param playingField Sets the passed int[][] to current playing field
     */
    public void setPlayingField(int[][] playingField) {
        this.playingField = playingField;
        updateBoard();
        drawBoard();
        if (isAlternateGraphics()) {

            alternateGraphicsRender();

        }
        showLastMove();
        if(checkForVictory()){
            gameOver();
        }
    }

    /**
     * Basically recreate a board
     *
     * @param cols number of columns
     * @param rows number of rows
     */
    public void clear(int cols, int rows) {
        this.numRows = rows;
        this.numColumns = cols;
        clear();
        disableProperty().set(false);
    }

    /**
     * Recreate the board but use the default rows and columns
     */
    public void clear() {
        this.getChildren().removeAll(this.getChildren());
        this.getRowConstraints().removeAll(this.getRowConstraints());
        this.getColumnConstraints().removeAll(this.getColumnConstraints());
        LOGGER.info(String.valueOf(this.getChildren().isEmpty()));
        playerOneScore.set(0);
        playerTwoScore.set(0);
        setupBoard();
    }

    /**
     * Check to see if any pieces were flipped
     *
     * @param col column of selected piece
     * @param row row of selected piece
     */
    public void checkFlipped(int col, int row) {
        checkUp(col, row);
        LOGGER.finest(String.valueOf(validMove));
        checkDown(col, row);
        LOGGER.finest(String.valueOf(validMove));
        checkLeft(col, row);
        LOGGER.finest(String.valueOf(validMove));
        checkRight(col, row);
        LOGGER.finest(String.valueOf(validMove));
        checkUpLeft(col, row);
        LOGGER.finest(String.valueOf(validMove));
        checkUpRight(col, row);
        LOGGER.finest(String.valueOf(validMove));
        checkDownLeft(col, row);
        LOGGER.finest(String.valueOf(validMove));
        checkDownRight(col, row);
        LOGGER.finest(String.valueOf(validMove));

    }

    /**
     * Notifies everyone the game is over.
     */
    private void gameOver() {
        LOGGER.finest("Game over!");
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.getButtonTypes().remove(1);
        a.setGraphic(null);
        a.setHeaderText("Game over!");
        a.setTitle("Game over!");
        a.setContentText("Team 1 had " + playerOneScore.get()
                + ".\nTeam 2 had " + playerTwoScore.get() + ".");
        a.show();
        this.disableProperty().set(true);

    }

    /**
     *
     * @return True or false if victory achieved by one player or the other
     */
    private boolean checkForVictory() {
        boolean result = true;
        playerOneScore.set(0);
        playerTwoScore.set(0);
        for (int cols = 0; cols < numRows; cols++) {
            for (int row = 0; row < numRows; row++) {

                if (playingField[cols][row] == 0) {
                    result = false;
                } else if (playingField[cols][row] == 1) {
                    playerOneScore.setValue(playerOneScore.get() + 1);
                } else {
                    playerTwoScore.setValue(playerTwoScore.get() + 1);
                }

            }
        }
        return result;
    }

    /**
     * All of the board setup 1) Generates Row constraints 2) Generates Column
     * constraints 3)Finds beginning and end points for determining borders 4)
     * Creates the buttons and adds them to the grid. 5)Create default buttons
     * that are already flipped
     */
    private void setupBoard() {

        playingField = new int[numColumns][numRows];
        playerTurn.setValue(1);

        for (int row = 0; row < numRows; row++) {
            RowConstraints rc = new RowConstraints();
            rc.setMinHeight(44);
            rc.setMaxHeight(75);
            rc.setFillHeight(true);
            rc.setVgrow(Priority.ALWAYS);
            this.getRowConstraints().add(rc);
        }
        for (int col = 0; col < numColumns; col++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setMaxWidth(75);
            cc.setMinWidth(52);
            cc.setFillWidth(true);
            cc.setHgrow(Priority.ALWAYS);

            this.getColumnConstraints().add(cc);
        }

        int begin[][] = new int[numColumns][2];
        int end[][] = new int[numRows][2];

        for (int col = 0; col < numColumns; col++) {
            if (col < numColumns / 2) {
                begin[col][0] = col;
                begin[col][1] = col;
                end[col][0] = col;
                end[col][1] = numRows - 1 - col;
            } else if (col > numColumns / 2) {
                begin[col][0] = col;
                begin[col][1] = numRows % col;
                end[col][0] = col;
                end[col][1] = numRows - 1 - numRows % col;
            } else {
                begin[col][0] = col;
                begin[col][1] = col - 1;
                end[col][0] = col;
                end[col][1] = col;

            }

            for (int row = 0; row < numRows; row++) {
                playingField[col][row] = 0;

                Button btn = new Button();
                btn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                btn.autosize();

                //This handles the top left and bottom right corners
                if (col == row) {

                    if (row < (numColumns / 2)) {
                        btn.setStyle("-fx-border-color: Black; "
                                + "-fx-border-style: solid hidden hidden solid;"
                                + "-fx-border-width: 3;");
                    } else {
                        btn.setStyle("-fx-border-color: Black; "
                                + "-fx-border-style:  hidden solid solid hidden;"
                                + "-fx-border-width: 3;");
                    }

                    //This handles the bottom left and top right corners    
                } else if (col + row == (numColumns - 1)) {
                    if (col < row) {
                        btn.setStyle("-fx-border-color: Black; "
                                + "-fx-border-style: hidden  hidden solid solid;"
                                + "-fx-border-width: 3;");
                    } else {
                        btn.setStyle("-fx-border-color: Black; "
                                + "-fx-border-style: solid solid hidden hidden;"
                                + "-fx-border-width: 3;");
                    }
                    //This handles the top, bottom, left and right
                } else {
                    //Top
                    if (row < begin[col][1]) {
                        btn.setStyle("-fx-border-color: Black; "
                                + "-fx-border-style: solid hidden hidden hidden;"
                                + "-fx-border-width: 3;");

                        //Bottom    
                    } else if (row > end[col][1]) {
                        btn.setStyle("-fx-border-color: Black; "
                                + "-fx-border-style:  hidden hidden solid hidden;"
                                + "-fx-border-width: 3;");
                        //Left
                    } else if (row > begin[col][1] && col < (numColumns / 2)) {
                        btn.setStyle("-fx-border-color: Black; "
                                + "-fx-border-style:  hidden hidden  hidden solid;"
                                + "-fx-border-width: 3;");
                        //Right
                    } else if (row <= end[col][1]) {
                        btn.setStyle("-fx-border-color: Black; "
                                + "-fx-border-style:  hidden solid hidden  hidden ;"
                                + "-fx-border-width: 3;");
                    }
                }
                btn.setId(String.valueOf(row) + "," + String.valueOf(col));
                btn.setOnAction((ActionEvent event) -> {
                    System.out.println(((Button) (event.getSource())).getParent().getId());
                    LOGGER.info("Button clicked; ActionEvent fired");
                    int r = Integer.parseInt(btn.getId().split(",")[1]);
                    int c = Integer.parseInt(btn.getId().split(",")[0]);
                    doOnButtonClick(c, r);
                });
                this.add(btn, col, row);                

            }

        }

        playingField[numColumns / 2][numRows / 2] = 1;
        playingField[numColumns / 2][numRows / 2 - 1] = 2;
        playingField[numColumns / 2 - 1][numRows / 2] = 2;
        playingField[numColumns / 2 - 1][numRows / 2 - 1] = 1;
        Button btnEdit = ((Button) (this.lookup("#" + (numColumns / 2) + "," + (numRows / 2))));
        btnEdit.setOnAction(null);

        btnEdit = ((Button) (this.lookup("#" + (numColumns / 2) + "," + (numRows / 2 - 1))));
        btnEdit.setOnAction(null);

        btnEdit = ((Button) (this.lookup("#" + (numColumns / 2 - 1) + "," + (numRows / 2))));
        btnEdit.setOnAction(null);

        btnEdit = ((Button) (this.lookup("#" + (numColumns / 2 - 1) + "," + (numRows / 2 - 1))));
        btnEdit.setOnAction(null);

        updateBoard();
        if (isAlternateGraphics()) {
            alternateGraphicsRender();
        }

    }

    private void checkLeft(int col, int row) {
        int value = playingField[col][row];
        while (col > 0) {
            col--;
            if (playingField[col][row] == 0) {
                reset();
                break;
            } else if (playingField[col][row] == value) {
                updateBoard();
                break;
            } else {
                playingField[col][row] = -1 * playingField[col][row];
            }
        }
        reset();
    }

    private void checkRight(int col, int row) {
        int value = playingField[col][row];
        while (col < numColumns - 1) {
            col++;
            if (playingField[col][row] == 0) {
                reset();
                break;
            } else if (playingField[col][row] == value) {
                updateBoard();
                break;
            } else {
                playingField[col][row] = -1 * playingField[col][row];
            }
        }
        reset();
    }

    private void checkUp(int col, int row) {
        int value = playingField[col][row];
        while (row > 0) {
            row--;
            if (playingField[col][row] == 0) {
                reset();
                break;
            } else if (playingField[col][row] == value) {
                updateBoard();
                break;
            } else {
                playingField[col][row] = -1 * playingField[col][row];
            }
        }
        reset();
    }

    private void checkDown(int col, int row) {
        int value = playingField[col][row];
        while (row < numRows - 1) {
            row++;
            if (playingField[col][row] == 0) {
                reset();
                break;
            } else if (playingField[col][row] == value) {
                updateBoard();
                break;
            } else {
                playingField[col][row] = -1 * playingField[col][row];
            }
        }
        reset();
    }

    private void checkUpLeft(int col, int row) {
        int value = playingField[col][row];
        while (row > 0 && col > 0) {
            row--;
            col--;
            if (playingField[col][row] == 0) {
                reset();
                break;
            } else if (playingField[col][row] == value) {
                updateBoard();
                break;
            } else {
                playingField[col][row] = -1 * playingField[col][row];
            }
        }
        reset();
    }

    private void checkDownLeft(int col, int row) {
        int value = playingField[col][row];
        while (row < numRows - 1 && col > 0) {
            row++;
            col--;
            if (playingField[col][row] == 0) {
                reset();
                break;
            } else if (playingField[col][row] == value) {
                updateBoard();
                break;
            } else {
                playingField[col][row] = -1 * playingField[col][row];
            }
        }
        reset();
    }

    private void checkUpRight(int col, int row) {
        int value = playingField[col][row];
        while (row > 0 && col < numColumns - 1) {
            row--;
            col++;

            if (playingField[col][row] == 0) {
                reset();
                break;
            } else if (playingField[col][row] == value) {
                updateBoard();
                break;
            } else {
                playingField[col][row] = -1 * playingField[col][row];
            }
        }
        reset();
    }

    private void checkDownRight(int col, int row) {
        int value = playingField[col][row];

        while (row < numRows - 1 && col < numColumns - 1) {
            row++;
            col++;
            if (playingField[col][row] == 0) {
                reset();
                break;
            } else if (playingField[col][row] == value) {
                updateBoard();
                break;
            } else {
                playingField[col][row] = -1 * playingField[col][row];
            }
        }
        reset();
    }

    /**
     * This resets everything back to the way it was at the beginning of the
     * turn if there aren't any endpoints found It can do this because each
     * piece is set to -1 and then at the end of the check it is either a valid
     * move or an invalid move. If it is invalid it will be updated and the
     * negative values will be set back to positive values Refer to updateBoard
     * for info on what happens when it is correct.
     */
    private void reset() {
        for (int col = 0; col < numColumns; col++) {
            for (int row = 0; row < numRows; row++) {
                if (playingField[col][row] < 0) {
                    playingField[col][row] = playingField[col][row] * -1;
                }
            }
        }
    }

    /**
     * This will finalize/update the board. It gets each value and if it is
     * negative and this is called, it must mean there is a valid turn therefore
     * we change all of the previously negative pieces to positive values
     * equivalent to the player whose turn it is. Since the player turn is not
     * changed until after all of the checks are made we are safe to assume
     * this.
     */
    private void updateBoard() {
        for (int col = 0; col < numColumns; col++) {
            for (int row = 0; row < numRows; row++) {
                Button change = (Button) (this.lookup("#" + col + "," + row));
                if (playingField[col][row] < 0) {
                    validMove = true;
                    playingField[col][row] = playerTurn.get();
                    change.setOnAction(null);

                }
                if (!isAlternateGraphics()) {

                    change.setText(String.valueOf(
                            playingField[col][row] != 0 ? playingField[col][row] : ""));
                    change.setGraphic(null);
                } else {

                    change.setText("");
                }

            }
        }
    }

    /**
     * Renders the alternate graphics with the images. Do not try to combine
     * this with the update board. Changing the graphics before the turn is
     * completed (i.e. the turn is valid) can display images that aren't really
     * there in the playingField.
     *
     *
     */
    private void alternateGraphicsRender() {
        for (int col = 0; col < numColumns; col++) {
            for (int row = 0; row < numRows; row++) {
                Button change = (Button) (this.lookup("#" + col + "," + row));
                change.setText("");                
                if (playingField[col][row] == 2) {
                    ImageView tmp = new ImageView(blackImg);
                    tmp.fitWidthProperty().set(30);
                    tmp.pickOnBoundsProperty().set(true);
                    tmp.preserveRatioProperty().set(true);
                    change.setGraphic(tmp);
                    change.setShape(null);

                } else if (playingField[col][row] == 1) {
                    ImageView tmp = new ImageView(whiteImg);
                    tmp.fitWidthProperty().set(30);
                    tmp.pickOnBoundsProperty().set(true);
                    change.setGraphic(tmp);
                    change.setShape(null);
                }
            }
        }
    }

    /**
     * Actions performed on Button Click This contains the list of things that
     * must happen on a button click. 1. Changes the value to the current player
     * 2. Checks to see which ones need to be flipped 3. If there are any valid
     * moves, it will redraw the board if using alternate graphics 4. Check for
     * victory 5. Go to next player
     *
     * @param c column of button clicked
     * @param r row of button clicked
     */
    private void doOnButtonClick(int c, int r) {

        playingField[c][r] = playerTurn.get();
        Button change = (Button) (lookup("#" + c + "," + r));
        change.setText(!isAlternateGraphics() ? String.valueOf(playingField[c][r]) : "");
        checkFlipped(c, r);
        if (checkValid(c, r, change)) {
            drawBoard();
            if (isAlternateGraphics()) {

                alternateGraphicsRender();
            }

            if (checkForVictory()) {
                gameOver();
            } 
            nextPlayer(c, r);

        }

    }

    /**
     * Checks to see if the turn had any valid moves.
     *
     * @param col Col of turn
     * @param row Row of turn
     * @param change The button that was clicked
     * @return weather the move is valid or not
     */
    private boolean checkValid(int col, int row, Button change) {
        if (!validMove) {
            playingField[col][row] = 0;
            change.setText(String.valueOf(playingField[col][row] != 0 ? playingField[col][row] : ""));
            return false;

        } else {

            LOGGER.log(Level.FINEST, "Setting {0} to null", change.getId());
            change.setOnAction(null);
            validMove = false;
            passCounter = 0;
            return true;
        }
    }

    private void setupLogging() {
        Handler consoleHandler = new ConsoleHandler();
        Handler fileHandler;
        try {
            Formatter simpleFormatter = new SimpleFormatter();
            fileHandler = new FileHandler("./board.log");
            fileHandler.setFormatter(simpleFormatter);
            LOGGER.addHandler(consoleHandler);
            LOGGER.addHandler(fileHandler);
            consoleHandler.setLevel(Level.WARNING);
            fileHandler.setLevel(Level.ALL);
            LOGGER.setLevel(Level.ALL);

        } catch (IOException | SecurityException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        LOGGER.config("Configuration done.");
    }

    /**
     * Base 64 decodes images and sets them to blackImg and whiteImg
     */
    private void setupImages() {
        //Base64 encoded images to avoid having to lug img files around.
        String white = "iVBORw0KGgoAAAANSUhEUgAAAB4AAAAeCAYAAAA7MK6iAAAABGdBTUE"
                + "AALGPC/xhBQAAAAlwSFlzAAAOwgAADsIBFShKgAAAABl0RVh0U29mdHdhcmU"
                + "AcGFpbnQubmV0IDQuMC4xMzQDW3oAAALySURBVEhLxZdPSJNhHMdfgg0lpig"
                + "EdYkllIJCgkdBcIFEbCoDPXboJHN5GQwvIdOTdfGSSfc6lHiIQagdpA6BhIc"
                + "gkoGH6SgKGuzVtb/vnn6f3kdEUqdtL/vCF16e5/v7flDcu5/GOXVL/FD8Uvx"
                + "FnBYrbZ45444M2Zp1R7wgfufxeD6PjY3tjY6OZgKBQMHv9yvMM2fckSGrZ5i"
                + "9sK6L7zc1Nb3p7u7O9/b2qqmpKbW6unqmyZBlhlk6dNe5dMXtdj9qaWkp9vf"
                + "3q8XFRbW8vLxfLBZNVUVkyDLDLB100WlXny63+JXX6y1Eo9HK5uZmRspUqVS"
                + "qIN1/qsiQZYZZOuiiU3efqEvi521tbebc3Fxld3dX5fP5ku68sJilgy466da"
                + "Mf/SgubnZnJiYqOzs7BT1fM2ii066YdioI111uVyJwcFBtb29ndUzdROddMO"
                + "AZSNtxTo7O9X6+nrdoYeiGwYsG2kYHnFqcnJSZbNZS+fqLrphwNJM415ra6u"
                + "Kx+O/dcYxwYAlTD/gBX7/6XR6X987JhiwhPkU8IdYLKavnBcsYX4E/GNmZkY"
                + "fOy9YwvwFuNwAsAXYahQ43QAw3+HGp9nZWX3svGDBBPzM5/Mp0zQP9J1jggE"
                + "LJuAAH+q1tTXHXpeHgqFfIAHAvL6+h8NhlcvlHHtl0g1DWD81868ed3V1qY2"
                + "NDcd+arphwLKRtm7IirI3NDSkEolE3eF00g0Dlo08Uli+rHOhUMhKJpN1WwT"
                + "oopNuGDbquFziF+3t7Qfz8/PlVCqlCoXCf68+zNJBF510a8aJuiyOd3R0lKa"
                + "npytbW1sZWd5UuVw+97JHlhlm6aCLTt19pq7JivKEP/uBgQG1tLSkVlZW9i3"
                + "Lqvq1SYYsM8zSQReddnV13RSHZCl/29PTU+nr61ORSIT15UyTIcsMs3Torgv"
                + "rrpi19L0s51/Hx8e/BYNBc2RkpDQ8PKwwz5xxR4asnmG2Zt0WR8WvxfyDxqr"
                + "KZxLzzBl3ZMhWkWH8ARbyXR1w1Z7RAAAAAElFTkSuQmCC";
        String black = "iVBORw0KGgoAAAANSUhEUgAAAB4AAAAeCAYAAAA7MK6iAAAABGdBTUE"
                + "AALGPC/xhBQAAAAlwSFlzAAAOwgAADsIBFShKgAAAABl0RVh0U29mdHdhcmU"
                + "AcGFpbnQubmV0IDQuMC4xMzQDW3oAAAJPSURBVEhLxZe9a1NRGMYPkoRAPpd"
                + "CC4nhku8PkinJH+AiYv+FDh1LXd0cutalixS616FuUnBxsoNLRx0ExwyK2KJ"
                + "Fqtb0+vzCaaG913rT3Ete+JGXc5/3eUhuknOuCVg18Ug8F+/FkXAt9KxxDQ3"
                + "ameue2BKv0+n0u263eyxOxZlwLfSsHaNBa2eYnbruipVkMvmy3W7/FG6j0bh"
                + "4h/8EDVpmmNXaisArUC0kEoknmUzmd71ed/v9vpvP58+17ht2HbTMMIsHXlp"
                + "fEDdWQuwVi8VfDKqfCTzwUr8n8PatO2Inl8udlMtlX6PbgBee6ncEGZ5a1X0"
                + "5CXIvpwVPvNWviiu1GIvFPlYqFd/BMMCbDPWL4rI2CoWC70CY2IwNMamMGPF"
                + "N1Guk2IyRINM8yGazbiqVCvyTuS1kkKX+oTBbzWbTI4oKm/VMmIPhcOgRRIX"
                + "NeivM58Fg4BFEhc36KsyfOQSPhRnPK/hoDsHs4eZwDsGHwmy3Wi2PICps1rY"
                + "wyzo5eARRYbOWxeTv61MUu9J1bMYXMfnLpDa1YXuEYWMzNsVlOfF4fFStVj3"
                + "isMCbDPWOuFLr2qxP9XGEvlngibf6deGpuNjVYe2H4zihheOFp/pdQYZvpcR"
                + "+qVQ6q9VqvkbTgAde6vcF3jfWku7FU/ZNvoVs3jqsBf4E0DLDLB54aX1JBKq"
                + "qWNN9edXpdM55Yghy3EWDlhlmtbYm8Jq67guOpW90OP/Q6/W+CR5XxgRYxqx"
                + "xDQ1aO8PszNUTj8ULwQPad3HxTulZ4xoatP8pY/4CxeruOoJDugEAAAAASUV"
                + "ORK5CYII=";

        byte[] wImg = DatatypeConverter.parseBase64Binary(white);
        byte[] bImg = DatatypeConverter.parseBase64Binary(black);
        BufferedImage wBI = null;
        BufferedImage bBI = null;
        try {
            bBI = ImageIO.read(new ByteArrayInputStream(bImg));
            wBI = ImageIO.read(new ByteArrayInputStream(wImg));
        } catch (IOException ex) {
            Logger.getLogger(Board.class.getName()).log(Level.SEVERE, null, ex);
        }
        WritableImage bWI = null;
        WritableImage wWI = null;

        blackImg = SwingFXUtils.toFXImage(bBI, bWI);
        whiteImg = SwingFXUtils.toFXImage(wBI, wWI);

    }

    private void showLastMove() {
        if (lastMove[0] != -1) {
            Button btn = (Button)(lookup("#"+String.valueOf(lastMove[0]) + "," + String.valueOf(lastMove[1])));
            btn.setShape(new Ellipse());
        }
    }
    public void setLastMove(int[] lmove){
        lastMove = lmove;
    }

}
