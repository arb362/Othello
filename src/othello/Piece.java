/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othello;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author ablankenship
 */
public class Piece extends Button {

    ImageView blackImgView, whiteImgView;

    public Piece() {
        initialize();
    }

    public Piece(String text) {
        super(text);
        initialize();

    }

    private void initialize() {
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        autosize();
        Image[] img = setupImages();
        blackImgView = new ImageView(img[0]);
        whiteImgView = new ImageView(img[1]);
        
        blackImgView.fitWidthProperty().set(30);
        blackImgView.pickOnBoundsProperty().set(true);
        blackImgView.preserveRatioProperty().set(true);
        whiteImgView.fitWidthProperty().set(30);
        whiteImgView.pickOnBoundsProperty().set(true);
        whiteImgView.preserveRatioProperty().set(true);
    }

    public void setPiece(int color) {
        switch (color) {

            case 2: // Black            
                this.setGraphic(blackImgView);
                
                break;
            case 1: //White
                this.setGraphic(whiteImgView);
                break;
            default:
                this.setGraphic(null);
                break;

        }
        this.setShape(null);
    }

    /**
     * Base 64 decodes images and sets them to blackImg and whiteImg
     */
    private Image[] setupImages() {
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

        Image blackImg = SwingFXUtils.toFXImage(bBI, bWI);
        Image whiteImg = SwingFXUtils.toFXImage(wBI, wWI);
        Image[] imgs = {blackImg, whiteImg};
        return imgs;
    }

}
