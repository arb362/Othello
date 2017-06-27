/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othello.network;
import java.util.ArrayList;
import othello.OthelloListener;

/**
 *
 * @author ablankenship
 */
public interface MultiplayerEngine {
    
    
    public abstract void send();
    public abstract int[][] getPlayingField();
    public abstract void setPlayingField(int[][] playingField);    
    public abstract void stopEngine();
    public abstract void notifySent();
    public abstract void notifyReceived();    
    public abstract ArrayList<OthelloListener> getOthelloListeners();
    public abstract void setLastMove(int column, int row);
    public abstract int[] getLastMove();
    
}
