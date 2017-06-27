/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othello;

/**
 *
 * @author Andrew
 */
public interface OthelloListener {

    /**
     * The method that is called whenever there is a message sent successfully
     */
    public abstract void onSent();

    /**
     * The method that is called whenever there is a message received
     * successfully
     */
    public abstract void onReceived();

    /**
     * Handle what happens when a player is disconnected
     */
    public abstract void onDisconnect();

    /**
     * The method that is called whenever a turn is completed
     */
    public abstract void onTurnComplete(int column, int row);

    /**
     * The method that is called to set the turn on the interface
     *
     * @param player int that signifies which player it is, 1 or 2
     */
    public abstract void setTurn(int player);

    /**
     * The method that is called the multiEngine is setup successfully.
     */
    public abstract void onSetup();

}
