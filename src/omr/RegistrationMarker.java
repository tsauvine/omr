package omr;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Observable;

public class RegistrationMarker extends Observable {
    public enum RegistrationMarkerEvent {
        MARKER_CHANGED
    }
    
    private int x;              // Middlepoint of the marker (center of the image) 
    private int y;
    private int imageWidth;     // Width of the marker image
    private int imageHeight;    // Height of the marker image
    private int searchRadius;
    
    private BufferedImage markerBuffer;  // Image of the marker
    
    public RegistrationMarker(int x, int y) {
        this.x = x;
        this.y = y;
        this.imageWidth = 32;
        this.imageHeight = 32;
        this.searchRadius = 16;
    }

    public Point getPoint() {
    	return new Point(x, y);
    }
    
    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
        
        // Notify observers
        setChanged();
        notifyObservers();
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
        
        // Notify observers
        setChanged();
        notifyObservers();
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
        
        if (this.imageWidth < 1) {
            this.imageWidth = 1;
        }
        
        // Notify observers
        setChanged();
        notifyObservers();
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
        
        if (this.imageHeight < 1) {
            this.imageHeight = 1;
        }
        
        // Notify observers
        setChanged();
        notifyObservers();
    }

    public int getSearchRadius() {
        return searchRadius;
    }

    public void setSearchRadius(int radius) {
        this.searchRadius = radius;
        
        if (this.searchRadius < 0) {
            this.searchRadius = 0;
        }
        
        // Notify observers
        setChanged();
        notifyObservers();
    }

    /**
     * Copies the marker image from the given sheet.
     * @param sheetBuffer unaligned unzoomed buffer image
     * @return true if marker image was succesfully copied
     */
    public void copyMarkerImage(BufferedImage sheetBuffer) {
        if (sheetBuffer == null) {
            return;
        }
        
        // Copy marker image from the sheet buffer
        int leftX = this.x - this.imageWidth / 2;
        int topY = this.y - this.imageHeight / 2;
        
        this.markerBuffer = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        this.markerBuffer.getGraphics().drawImage(sheetBuffer,
            0, 0, imageWidth, imageHeight,
            leftX, topY, leftX + imageWidth, topY + imageHeight,
            null);
        
        // Notify observers. Image changed.
        setChanged();
        notifyObservers();
    }
    
    public void copyMarkerImage(Sheet sheet) {
        try {
            this.copyMarkerImage(sheet.getUnalignedBuffer());
        } catch (OutOfMemoryError e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }
    }
    
    public BufferedImage getBuffer() {
    	return this.markerBuffer;
    }
}
