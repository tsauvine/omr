package omr;

import java.awt.image.BufferedImage;

/**
 * Histogram of bubble brighnesses.
 * 
 * @author Tapio Auvinen
 */
public class Histogram {
    private int[] histogram;             // Number of occurrences at each brightness level
    private BufferedImage[] examples;    // Bubble image from each brightness level 
    private int minIndex;                // Lowest index that has occurrences 
    private int maxIndex;				 // Highest index that has occurrences
    private int maxValue;                // Highest number of occurrences, i.e. the height of the highest peak in the histogram.
    private int mode;                    // Index that has highest number of occurrences, i.e. the index of the highest peak in the histogram.
    private int sum;                     // Total number of occurrences (== number of bubbles)
    private int blackThreshold;
    private int whiteThreshold;
    
    
    public Histogram() {
        this(true);
    }
    
    public Histogram(boolean enableExampleBubbles) {
        this.histogram = new int[256];
        this.sum = 0;
        
        if (enableExampleBubbles) {
            this.examples = new BufferedImage[256];
        }
        
        blackThreshold = -1;
        whiteThreshold = 256;
    }

    /**
     * Returns the histogram to the empty state.
     */
    public void reset() {
        // Reset histogram
        for (int i = 0; i < 256; i++) {
            this.histogram[i] = 0;
        }
        
        if (this.examples != null) {
            for (int i = 0; i < 256; i++) {
                this.examples[i] = null;
            }
        }
        
        this.minIndex = 256;
        this.maxIndex = 0;
        this.maxValue = 0;
        this.mode = 0;
        this.sum = 0;
    }
    
    /**
     * Increases the height of a bar by one.
     * @param index Bubble brightness
     */
    public void increase(int index) {
        histogram[index]++;
        sum++;
        
        if (index < minIndex) {
            minIndex = index;
        }
        
        if (index > maxIndex) {
            maxIndex = index;
        }
        
        if (histogram[index] > maxValue) {
        	maxValue = histogram[index];
        	mode = index;
        }
    }
    
    /**
     * Returns the histogram array.
     * @return int[256] where each slot contains the number of bubbles with that brightness.
     */
    public int[] getHistogram() {
        return this.histogram;
    }
    
    /**
     * Returns an array of bubble images corresponding to each brightness level. The array may contain nulls.
     * @return
     */
    public BufferedImage[] getExamples() {
        return this.examples;
    }
    
    /**
     * Sets an example bubble image.
     * @param index Brightness
     * @param image Bubble image
     */
    public void setExample(int index, BufferedImage image) {
        if (this.examples == null) {
            return;
        }
        
        this.examples[index] = image;
    }
    
    /**
     * Returns an example bubble with the given brightness.
     * @param index
     * @return
     */
    public BufferedImage getExample(int index) {
        if (this.examples == null) {
            return null;
        }
        
        return this.examples[index];
    }
    
    /**
     * Get the smallest brightness encountered.
     */
    public int getMinIndex() {
        return minIndex;
    }
    
    /**
     * Get the highest brightness encountered.
     */
    public int getMaxIndex() {
        return maxIndex;
    }
    
    /**
     * Get the highest number of bubbles with the same brightness level, i.e. the height of the highest peak in the histogram.
     */
    public int getMaxValue() {
        return maxValue;
    }
    
    /**
     * Get the brightness level that has most bubbles, i.e. the location of the highest peak in the histogram.
     */
    public int getMode() {
        return mode;
    }
    
    /**
     * Returns the median of the logarithmic histogram.
     */
    public int getLogMedian() {
    	double threshold = Math.log(this.sum) / 2;
    	int sum = 0;
    	
    	for (int i = 0; i < 255; i++) {
    		sum += histogram[i];
    		if (sum > 1 && Math.log(sum) >= threshold) {
    			return i;
    		}
    	}
    	
    	return 0;
    }
    
    /**
     * Returns the average bubble brightness.
     * @return
     */
    public int getAverage() {
    	int sum = 0;
    	for (int i = 0; i < 255; i++) {
    		sum += i * histogram[i];
     	}
    	
    	int average = sum / this.sum; 

    	return average;
    }
    
    /**
     * Returns the highest brightness that is considered filled.   
     */
    public int getBlackThreshold() {
        return blackThreshold;
    }

    /**
     * Sets the highest brightness that is considered filled.
     */
    public void setBlackThreshold(int blackThreshold) {
        this.blackThreshold = blackThreshold;
        
        if (this.blackThreshold < 0) this.blackThreshold = 0;
        if (this.blackThreshold > 255) this.blackThreshold = 255;
        if (this.blackThreshold > this.whiteThreshold) this.whiteThreshold = this.blackThreshold;
    }

    /**
     * Returns the lowest brightness that is considered unfilled. 
     */
    public int getWhiteThreshold() {
        return whiteThreshold;
    }

    /**
     * Sets the lowest brightness that is considered unfilled.
     */
    public void setWhiteThreshold(int whiteThreshold) {
        this.whiteThreshold = whiteThreshold;
        
        if (this.whiteThreshold < 0) this.whiteThreshold = 0;
        if (this.whiteThreshold > 255) this.whiteThreshold = 255;
        if (this.whiteThreshold < this.blackThreshold) this.blackThreshold = this.whiteThreshold;
    }
    
    /** 
     * Automatiacally sets the thresholds to sensible values.
     * Currently, the blackThreshold is set to the midpoint between mode and the median of the logarithmic histogram,
     * and whiteThreshold is set to 75% between mode and the median of the logarithmic histogram.
     * The formula may change in future version. 
     */
    public void guessThreshold() {
        int median = this.getLogMedian();
        int mode = this.getMode();
        
        int blackThreshold = (int)(median + (mode - median) * .5);
        int whiteThreshold = (int)(median + (mode - median) * .75);
        
        // Mode is known to be white. White threshold must be a bit smaller.
        if (whiteThreshold > mode - 2) {
            whiteThreshold = mode - 2;
        }
        
        this.setBlackThreshold(blackThreshold);
        this.setWhiteThreshold(whiteThreshold);
    }
}
