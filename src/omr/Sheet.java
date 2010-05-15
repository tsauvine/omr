package omr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import javax.imageio.ImageIO;

import omr.QuestionGroup.Orientation;

/**
 * Represents an answer sheet.
 * 
 * @author Tapio Auvinen
 */

public class Sheet extends Observable {
	
	protected String id;         // Unique id of this sheet
    protected String fileName;   // e.g. AnswerSheet001.jpg
    protected String filePath;   // e.g. /home/teacher/AnswerSheet001.jpg
    
    protected String studentId;        // The whole student id with number and letter
    protected String studentIdNumber;  // Number part of the student id
    protected String studentIdLetter;  // Check letter of the student id

    private HashMap<QuestionGroup,int[][]> brightness;      // [row][column]
    private HashMap<RegistrationMarker,Point> markers;      // Detected marker positions
    private HashMap<QuestionGroup,int[][]> choices;         // Choices for each question group. [row][column] negative = black, 0 = uncertain, postive = white
    private HashMap<QuestionGroup,int[][]> overrideChoices; // negative = force black, 0 = auto, postive = force white
    protected String userId;     // Id of the student
    
    private Histogram histogram;
    
    protected int rotation;                       // Rotation in degrees. 0, 90, 180 or 270
    protected AffineTransform transformation;     // Transformation that aligns the image with the reference sheet
    protected BufferedImage rawBuffer;            // Unaligned, un-zoomed buffer. The buffer is kept cached as long as the sheet has registered observers. Always TYPE_INT_RGB
    
    public enum SheetStatus {
    	NOT_ANALYZED(""),
    	ANALYZED_WITH_ERRORS("!"),
    	ANALYZED("OK");
    	
    	private String text;
    	
    	private SheetStatus(String text) {
    		this.text = text;
    	}
    	
    	@Override
        public String toString() {
    		return this.text;
    	}
    };
    
    private boolean answersValid;        // True if the sheet does not contain uncertain answers
    
    
    public Sheet(String filePath, String fileName) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.id = fileName;
        this.rotation = 0;
        
        this.studentId = "";
        this.studentIdNumber = "";
        this.studentIdLetter = "";
        
        this.histogram = new Histogram(false);
        
        this.transformation = new AffineTransform();
        
        this.answersValid = false;
    }
    
    public void setId(String id) {
    	this.id = id;
    }
    
    public String getId() {
    	return this.id;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getStudentId() {
        return this.studentId;
    }
    
    /**
     * Returns true if the loaded file can be read by the OMR algorithm, ie. the file is in a proper format.
     */
    public boolean isValidFile() {
        // TODO: validate file
        return true;
    }
    
    /**
     * Returns the name of the sheet image file without the path, e.g. AnswerSheet001.jpg
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Returns the full path of the sheet image file, e.g. /home/teacher/AnswerSheet001.jpg
     */
    public String getFilePath() {
        return filePath;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Histogram getHistogram() {
        return this.histogram;
    }
    
    /**
     * Returns the page of a multi-page document that this sheet represents.
     * @return Page [0,n]. 0 if this is not a multi-page document.
     */
    public int getPage() {
    	return 0; // Multi-page versions will override this
    }
    
    @Override
    public String toString() {
    	return this.id;
    }
    
    public SheetStatus getStatus() {
    	if (answersValid) {
    		return SheetStatus.ANALYZED;
    	} else if (this.choices != null) {
    		return SheetStatus.ANALYZED_WITH_ERRORS;
    	} else {
    		return SheetStatus.NOT_ANALYZED;
    	}
    }
   
    /**
     * @param degrees Degrees to rotate clockwise
     */
    public void setRotation(int degrees) {
    	// Round to 0, 90, 180, 270
    	this.rotation = ((degrees / 90) * 90) % 360;
    	
    	if (this.rotation < 0) {
    		this.rotation += 360;
    	}
    	
    	// Invalidate cache
    	this.rawBuffer = null;
    }
    
    /**
     * Returs the rotation of the sheet.
     * @return 0, 90, 180, 270, how many degrees the sheet is rotated clockwise
     */
    public int getRotation() {
    	return this.rotation;
    }
    
    
    @Override
    synchronized public void deleteObserver(Observer observer) {
        super.deleteObserver(observer);
    
        if (this.countObservers() < 2) {  // 2 because SheetsContainer is always listening. FIXME: this isn't robust. Use a different mechanism for keeping track of buffer users.
            // Release buffer
            this.rawBuffer = null;
        }
    }
    
    @Override
    synchronized public void deleteObservers() {
        super.deleteObservers();
        
        if (this.countObservers() < 2) {  // 2 because SheetsContainer is always listening
            // Release buffer
            this.rawBuffer = null;
        }
    }
    
    public void invalidateRegistration() {
    	this.markers = null;
    	invalidateBrightnesses();
    }
    
    public void invalidateBrightnesses() {
    	this.brightness = null;
    	invalidateAnswers();
    }
    
    public void invalidateAnswers() {
    	this.choices = null;
    }
    
    /**
     * Returns an aligned buffer in the original resolution.
     * The aligned buffer is not cached. Instead, it is generated on the fly
     * from the original buffer which may be cached.  
     */
    public BufferedImage getAlignedBuffer() throws OutOfMemoryError, IOException {
        return this.getAlignedBuffer(this.getUnalignedBuffer());
    }
    
    private BufferedImage getAlignedBuffer(BufferedImage unalignedBuffer) throws OutOfMemoryError, IOException {
        // Transform the unaligned buffer
    	BufferedImage transformedBuffer = new BufferedImage(unalignedBuffer.getWidth(), unalignedBuffer.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = transformedBuffer.createGraphics();
        g.drawRenderedImage(unalignedBuffer, this.transformation);
        g.dispose();
        
        return transformedBuffer;
    }
    
    /**
     * Returns an aligned buffer zoomed with te given multiplier.
     * The aligned buffer is not cached. Instead, it is generated on the fly
     * from the original buffer which may be cached.  
     */
    public BufferedImage getAlignedBuffer(double zoomLevel) throws OutOfMemoryError, IOException {
        BufferedImage tempBuffer = this.getUnalignedBuffer();
        
        // Scale the image
        AffineTransform transform = AffineTransform.getScaleInstance(zoomLevel, zoomLevel);
        transform.concatenate(this.transformation);
        
        BufferedImage transformedBuffer = new BufferedImage((int)(tempBuffer.getWidth() * zoomLevel), (int)(tempBuffer.getHeight() * zoomLevel), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = transformedBuffer.createGraphics();
        g.drawRenderedImage(tempBuffer, transform);
        g.dispose();
        
        return transformedBuffer;
    }
    
   
    /**
     * Returns the original unaligned buffer in the original resolution.
     * The buffer is cached as long as this sheet has registered observers.
     * First call to this method is very slow because the image file must be decoded.
     */
    public BufferedImage getUnalignedBuffer() throws OutOfMemoryError, IOException {
        // Return the cached buffer if available
        if (this.rawBuffer != null) {
            return this.rawBuffer;
        }

        // Load image
        BufferedImage tempBuffer = ImageIO.read(new File(filePath));  // read() returns null if image format is unsupported

        if (tempBuffer == null) {
            throw new IOException("Image " + fileName + " cannot be opened. Unsupported format.");
        }
        
        // Convert to the required bit format and rotate
        BufferedImage convertedBuffer;
        AffineTransform transform;
    	
    	if (this.rotation == 90) {
    		convertedBuffer = new BufferedImage(tempBuffer.getHeight(), tempBuffer.getWidth(), BufferedImage.TYPE_INT_RGB);
            transform = AffineTransform.getTranslateInstance(tempBuffer.getHeight(), 0);
        } else if (this.rotation == 180) {
        	convertedBuffer = new BufferedImage(tempBuffer.getWidth(), tempBuffer.getHeight(), BufferedImage.TYPE_INT_RGB);
        	transform = AffineTransform.getTranslateInstance(tempBuffer.getWidth(), tempBuffer.getHeight());
        } else if (this.rotation == 270) {
        	convertedBuffer = new BufferedImage(tempBuffer.getHeight(), tempBuffer.getWidth(), BufferedImage.TYPE_INT_RGB);
        	transform = AffineTransform.getTranslateInstance(0, tempBuffer.getWidth());
        } else {
        	convertedBuffer = new BufferedImage(tempBuffer.getWidth(), tempBuffer.getHeight(), BufferedImage.TYPE_INT_RGB);
        	transform = new AffineTransform();
        }
    	
    	transform.concatenate(AffineTransform.getRotateInstance(Math.toRadians(this.rotation)));
        
        Graphics2D g = convertedBuffer.createGraphics();
        g.drawRenderedImage(tempBuffer, transform);
        g.dispose();
            
        // Cache buffer if there are observers
        if (this.countObservers() > 0) {
            this.rawBuffer = convertedBuffer; 
        }
        
        return convertedBuffer;
    }
    
    /**
     * Returns the original unaligned buffer zoomed with the given multiplier.
     * The zoomed buffer is not cached. Instead, it is generated on the fly
     * from the original buffer which may be cached.  
     */
    public BufferedImage getUnalignedBuffer(double zoomLevel) throws OutOfMemoryError, IOException {
        BufferedImage tempBuffer = this.getUnalignedBuffer();
        BufferedImage scaledBuffer = new BufferedImage((int)(tempBuffer.getWidth() * zoomLevel), (int)(tempBuffer.getHeight() * zoomLevel), BufferedImage.TYPE_INT_RGB);
        
        AffineTransform zoom = AffineTransform.getScaleInstance(zoomLevel, zoomLevel);
        Graphics2D g = scaledBuffer.createGraphics();
        g.drawRenderedImage(tempBuffer, zoom);
        g.dispose();
        
        return scaledBuffer;
    }
    
    /**
     * Tells whether a bubble is selected or not. If the answer has been overridden by operator, the manually set value is returned instead of the original one.
     * @param group Question group
     * @param row
     * @param column
     * @return negative number if the bubble is clearly blackened, 0 if uncertain, positive if the bubble is clearly white
     */
    public int getAnswer(QuestionGroup group, int row, int column) {
        if (this.choices == null) {
        	return 0;
        }
    	
    	int[][] choices = this.choices.get(group);
        if (choices == null) {
            return 0;
        }
        
        int[][] overrideChoices = null;
        if (this.overrideChoices != null) {
        	overrideChoices = this.overrideChoices.get(group);
        }
        
        if (overrideChoices != null && overrideChoices[row][column] != 0) {
        	return overrideChoices[row][column];
        } else {
        	return choices[row][column];
        }
    }
    
    /**
     * Returns the answer override status
     * @param group
     * @param row Question number [0,n] in the given group (0 is the first question in the group regardless of index offset)
     * @param column Alternative number [0,n]
     * @return negative = force black, 0 = auto, postive = force white
     */
    public int getAnswerOverride(QuestionGroup group, int row, int column) {
    	if (this.overrideChoices == null) {
            return 0;
        }
    	
    	int[][] overrideChoices = this.overrideChoices.get(group);
        if (overrideChoices == null) {
            return 0;
        }
        
        return overrideChoices[row][column];
    }
    
    /**
     * Toggles the answer override between "force black", "force white" and "auto".
     */
    public void toggleAnswer(QuestionGroup group, int row, int column) {
        // Create override array if it doesn't exist
        if (this.overrideChoices == null) {
        	this.overrideChoices = new HashMap<QuestionGroup, int[][]>();
        }

        // Create override array for this group if necessary
    	int[][] overrideChoices = this.overrideChoices.get(group);
        if (overrideChoices == null) {
        	overrideChoices = new int[group.getRowCount()][group.getColumnCount()];
        	this.overrideChoices.put(group, overrideChoices);
        }
        
        // Toggle between 1, 0, -1
        overrideChoices[row][column]--;
        if (overrideChoices[row][column] < -1) {
        	overrideChoices[row][column] = 1;
        }
        
        // Update answer validity
        validateAnswers(group);
        
        // Notify observers
        setChanged();
        notifyObservers();
    }
    
    /**
     * Checks if all answers are detected with certainty or manually overridden.
     */
    private void validateAnswers(QuestionGroup group) {
    	if (this.choices == null) {
    		this.answersValid = false;
    		return;
    	}
    	
    	for (QuestionGroup key : this.choices.keySet()) {
    		int[][] choices = this.choices.get(key);
	    	if (choices == null) {
	    		this.answersValid = false;
				return;
	    	}
    		
    		int[][] overrideChoices = null;
    		if (this.overrideChoices != null) {
    			overrideChoices = this.overrideChoices.get(key);
    		}
	    	
	    	for (int row = 0; row < choices.length; row++) {
	    		for (int col = 0; col < choices[row].length; col++) {
	    			// Check that all uncertain answers are overridden
	    			if (choices[row][col] == 0 && (overrideChoices == null || overrideChoices[row][col] == 0)) {
	    				this.answersValid = false;
	    				return;
	    			}
	    		}
	    	}
    	}
    	
    	this.answersValid = true;
    }
    
    /**
     * Returns the selected alternatives as a string.
     * @param group Question group
     * @param question Number of the question in the group [0,n].
     * @return example: "AC", null if choices have not been calculated
     */
    public String getChoices(QuestionGroup group, int question) {
        if (this.choices == null) {
        	return null;
        }
    	
    	int[][] choices = this.choices.get(group);
        if (choices == null) {
            return null;
        }
        
        String result = "";
        
        if (group.getOrientation() == Orientation.CHECK_LETTER) {
        	// FIXME: this should be in QuestionGroup 
        	String[] row0 = {"A","B","C","D","E","F","H","J","K","L"}; 
        	String[] row1 = {"M","N","P","R","S","T","U","V","W",""};
        	
        	if (group.getRowCount() < 2 || group.getColumnCount() < 10) {
        		return "";
        	}
        	
        	for (int alternative = 0; alternative < group.getAlternativesCount(); alternative++) {
        		if (getAnswer(group, 0, alternative) < 0) {
        			return row0[alternative];
        		}
        		if (getAnswer(group, 1, alternative) < 0) {
        			return row1[alternative];
        		}
        	}
        	
        } else {
	        for (int alternative = 0; alternative < group.getAlternativesCount(); alternative++) {
	        	int answer;
	        	if (group.getOrientation() == Orientation.VERTICAL || group.getOrientation() == Orientation.STUDENT_NUMBER) {
	        		answer = getAnswer(group, question, alternative);
	        	} else {
	        		answer = getAnswer(group, alternative, question);
	        	}
	        	
	        	if (answer < 0) {
	            	result += group.getAlternative(alternative);
	            }
	        }
	    }
        
        return result;
    }
    
    /**
     * 
     * @param group Question group
     * @param row Bubble row number [0,n]
     * @param column Bubble column number [0,n]
     * @return average value of the pixels contained by the bubble [0...255]. 255 if value has not been calculated. 
     */
    public int getBubbleBrightness(QuestionGroup group, int row, int column) {
        if (this.brightness == null) {
        	return 255;
        }
    	
    	int[][] brightnessArray = this.brightness.get(group);
        if (brightnessArray == null) {
            return 255;
        }
        
        return brightnessArray[row][column];
    }
    
    /**
     * Returns the location of a given alignment marker in this sheet.
     * @param marker Marker who's location is queried
     * @return Point which contains the coordinates of the marker relative to the upper left corner of this sheet. Null if marker location is not known.
     */
    public Point getRegistrationMarkerLocation(RegistrationMarker marker) {
    	if (this.markers == null) {
    		return null;
    	}
    	
    	return this.markers.get(marker);
    }

    
    /**
     * Calculates and caches brightness values of the bubbles.
     * @param structure Sheet structure that contains positions of the bubbles.
     */
    public void analyze(SheetStructure structure, Histogram globalHistogram) throws OutOfMemoryError, IOException {
        BufferedImage unalignedBuffer = getUnalignedBuffer();
        
        // Locate registration markers
        if (this.markers == null) {
        	// Invalidate everything when alignemnt changes
        	this.invalidateBrightnesses();
            
        	this.markers = new HashMap<RegistrationMarker, Point>();
	        for (RegistrationMarker marker : structure.getRegistrationMarkers()) {
	        	locateMarker(unalignedBuffer, marker);
	        }
        }
        
        // Calculate transformation
        calculateTransformation(structure);
        
        // Calculate bubble brightnesses
        if (this.brightness == null) {
        	// Invalidate answers when brightnesses change
            this.invalidateAnswers();
        	
	        this.histogram.reset();
	        this.brightness = new HashMap<QuestionGroup, int[][]>();
	        
	        BufferedImage alignedBuffer = getAlignedBuffer(unalignedBuffer);
	        unalignedBuffer = null;  // Not needed any more
	        for (QuestionGroup group : structure.getQuestionGroups()) {
	            calculateBrightnesses(alignedBuffer, group, globalHistogram);
	        }
	        
	        // Calculate threshold
	        this.histogram.guessThreshold();
        }
    }
    
    /**
     * Initializes the transformation matrix for aligning the sheet. Markers should be located before calling this.
     * @param structure
     */
    private void calculateTransformation(SheetStructure structure) {
    	AbstractList<RegistrationMarker> markers = structure.getRegistrationMarkers();
    	
    	Point referenceMarker1 = null;              // Original marker positions in the referece sheet
    	Point referenceMarker2 = null;
    	Point translatedMarker1 = null;             // Translated marker positions in this sheet
    	Point translatedMarker2 = null;
    	this.transformation = new AffineTransform();
    	
    	if (markers.size() < 1) {
    		// No markers available. Don't translate. 
    		return;
    	}
    	
    	if (markers.size() >= 1) {
    		// At least one marker available
    		referenceMarker1 = markers.get(0).getPoint();
    		translatedMarker1 = this.getRegistrationMarkerLocation(markers.get(0));
    		
    		if (translatedMarker1 == null) {
    			return;  // Marker location not calculated. Don't do anything.
    		}
    	}
    	if (markers.size() >= 2) {
    		// Two markers available
    		referenceMarker2 = markers.get(1).getPoint();
    		translatedMarker2 = this.getRegistrationMarkerLocation(markers.get(1));
    	}
    		
    	
    	final double translationX = translatedMarker1.getX() - referenceMarker1.getX();
		final double translationY = translatedMarker1.getY() - referenceMarker1.getY();
		
		if (translatedMarker2 != null) {
            // Calculate translation, rotation and scaling of the sheet
	    	final double originalDx = referenceMarker2.getX() - referenceMarker1.getX();         // X distance between reference markers
	    	final double originalDy = referenceMarker2.getY() - referenceMarker1.getY();         // Y distance between reference markers
	    	final double translatedDx = translatedMarker2.getX() - translatedMarker1.getX();     // X distance between translated markers
	    	final double translatedDy = translatedMarker2.getY() - translatedMarker1.getY();     // Y distance between translated markers
    		
	    	final double angle = Math.atan2(translatedDy, translatedDx) - Math.atan2(originalDy, originalDx);
	    	final double scale = Math.sqrt(originalDx*originalDx + originalDy*originalDy) / Math.sqrt(translatedDx*translatedDx + translatedDy*translatedDy);
	    	
            // Multiple markers available. Do translation, rotation and scaling.
	    	// Generate a transformation matrix
	    	// 4. Translate back
    		transformation.translate(referenceMarker1.getX() - translationX, referenceMarker1.getY() - translationY);
    		
	    	// 3. Scale
	    	transformation.scale(scale,scale);
	    	
	    	// 2. Rotate
	    	transformation.rotate(-angle);
	    	
	    	// 1. Translate first marker to origin to rotate around that point
	    	transformation.translate(-referenceMarker1.getX(), -referenceMarker1.getY());
    	
    	} else {
    		// One marker available. Do translation only.
    		transformation.translate(-translationX, -translationY);
    	}
    }
    
    private void calculateBrightnesses(final BufferedImage buffer, final QuestionGroup group, Histogram globalHistogram) {
    	final int[] array = ((DataBufferInt)buffer.getRaster().getDataBuffer()).getData(); // Image buffer
        
        // Initialize the array where brightness values are saved
    	int[][] brightnessArray = new int[group.getRowCount()][group.getColumnCount()]; 
        this.brightness.put(group, brightnessArray);
        
        // Prepare histogram
        BufferedImage[] histogramExamples = globalHistogram.getExamples();

        final int bufferWidth = buffer.getWidth();
        final int bufferHeight = buffer.getHeight();
        final int columnCount = group.getColumnCount();
        final int rowCount = group.getRowCount();
        final int bubbleWidth = group.getBubbleWidth();
        final int bubbleHeight = group.getBubbleHeight();
        
        final double exampleScale = 24.0 / Math.max(bubbleWidth, bubbleHeight);
        final int exampleWidth = (int)(bubbleWidth * exampleScale);
        final int exampleHeight = (int)(bubbleHeight * exampleScale);
        
        final double columnSpacing = columnCount <= 1 ? 0 : (double)group.getWidth() / (columnCount - 1);
        final double rowSpacing = rowCount <= 1 ? 0 : (double)group.getHeight() / (rowCount - 1);

        // Iterate through bubble rows
        for (int row = 0; row < rowCount; row++) {
        	final int bubbleY = Math.max((int)(group.getTopY() - bubbleHeight / 2 + row * rowSpacing), 0);  // Top edge y coordinate

            // Iterate through each bubble on the row
            for (int col = 0; col < columnCount; col++) {
            	final int bubbleX = Math.max((int)(group.getLeftX() - bubbleWidth / 2 + col * columnSpacing), 0);  // Left edge x coordinate
                
                // Read every pixel in the bubble
                long sum = 0;
                final int bubbleBottomY = Math.min(bubbleY + bubbleHeight, bufferHeight - 1);     // Bottom edge y coordinate
                final int bubbleRightX = Math.min(bubbleX + bubbleWidth, bufferWidth);        // Right edge x coordinate
                
                for (int y = bubbleY; y < bubbleBottomY; y++) {
                    int offset = y * bufferWidth;
                          
                    for (int x = bubbleX; x < bubbleRightX; x++) {
                    	final int index = offset + x;
                        sum += ((array[index] & 0x00FF0000) >> 16)
                        	 + ((array[index] & 0x0000FF00) >> 8)
                        	 + ((array[index] & 0x000000FF));
                    }
                }
                
                final int brightness = (int)((double)sum / (bubbleWidth * bubbleHeight * 3));
                
                // Save brightness value
                brightnessArray[row][col] = brightness;
                
                // Update histogram
                if (brightness < 0) {
                    globalHistogram.increase(0);
                    this.histogram.increase(0);
                } else if (brightness > 255) {
                    globalHistogram.increase(255);
                    this.histogram.increase(255);
                } else {
                    globalHistogram.increase(brightness);
                    this.histogram.increase(brightness);
                    
                    // Copy example bubble
                    if (histogramExamples[brightness] == null) {
                        BufferedImage example = new BufferedImage(exampleWidth, exampleHeight, BufferedImage.TYPE_INT_RGB);
                        histogramExamples[brightness] = example;

                        // Copy bubble
                        Graphics g = example.createGraphics();
                        g.drawImage(buffer,
                            0, 0, exampleWidth, exampleHeight,
                            bubbleX, bubbleY, bubbleRightX, bubbleBottomY,
                            null);
                        g.dispose();
                    }
                }
            }   
        }
    }
    
    private void locateMarker(final BufferedImage sheetBuffer, final RegistrationMarker marker) {
    	if (sheetBuffer == null) {
			System.err.println("locateMarker: sheet buffer not available");
			return;
		}
    	
    	BufferedImage markerBuffer = marker.getBuffer();
		if (markerBuffer == null) {
			System.err.println("locateMarker: marker buffer not available");
			return;
		}
		
		final int[] sheetArray = ((DataBufferInt)sheetBuffer.getRaster().getDataBuffer()).getData();
		final int sheetWidth = sheetBuffer.getWidth();
		final int sheetHeight = sheetBuffer.getHeight();
		
		final int[] markerArray = ((DataBufferInt)markerBuffer.getRaster().getDataBuffer()).getData();
		final int markerX = marker.getX();
		final int markerY = marker.getY();
		final int markerWidth = markerBuffer.getWidth();
		final int markerHeight = markerBuffer.getHeight();
		final int searchRadius = marker.getSearchRadius();
		
		long minDifference = Long.MAX_VALUE;
		int foundX = markerX;
		int foundY = markerY;
		
		final int searchStartY = Math.max(markerY - searchRadius - markerHeight/2, 0);
		final int searchStartX = Math.max(markerX - searchRadius - markerWidth/2, 0);
		final int searchEndY = Math.min(searchStartY + 2 * searchRadius, sheetHeight - markerHeight);
		final int searchEndX = Math.min(searchStartX + 2 * searchRadius, sheetWidth - markerWidth);
		
		for (int searchY = searchStartY; searchY < searchEndY; searchY++) {
			for (int searchX = searchStartX; searchX < searchEndX; searchX++) {
				long difference = 0;
				
	    		// Calculate difference between marker and the sheet
	    		for (int y = 0; y < markerHeight; y++) {
	    			final int markerOffset = y * markerWidth;
	    			final int sheetOffset = (y + searchY) * sheetWidth + searchX;
	    		
		    		for (int x = 0; x < markerWidth; x++) {
		    			final int markerIndex = markerOffset + x;
		    			final int sheetIndex = sheetOffset + x;
		    			
		    			// Sum the absolute differences in red, green and blue component
	                    difference += Math.abs(((markerArray[markerIndex] & 0x00FF0000) >> 16) - ((sheetArray[sheetIndex] & 0x00FF0000) >> 16))
	                    	+ Math.abs(((markerArray[markerIndex] & 0x0000FF00) >> 8) - ((sheetArray[sheetIndex] & 0x0000FF00) >> 8))
	                    	+ Math.abs((markerArray[markerIndex] & 0x000000FF) - (markerArray[markerIndex] & 0x000000FF));
	                    	
		    			/*
		    			int d = ((markerArray[markerIndex] & 0x00FF0000) >> 16) - ((sheetArray[sheetIndex] & 0x00FF0000) >> 16)
		    				+ ((markerArray[markerIndex] & 0x0000FF00) >> 8) - ((sheetArray[sheetIndex] & 0x0000FF00) >> 8)
	                    	+ (markerArray[markerIndex] & 0x000000FF) - (markerArray[markerIndex] & 0x000000FF);
	                    // Don't need to divide by 3, because it doesn't change the sign nor the minimum position
	                    
	                    if (d < 0) {
		    				difference -= d;
		    			}
	                    */
		    		}
	    		}
	    		
	    		// Store minimum
	    		if (difference < minDifference) {
                	minDifference = difference;
                	foundX = searchX;
                	foundY = searchY;
                }
	    	}
		}
		
		// Store the location of the marker
		Point location = new Point(foundX + markerWidth / 2, foundY + markerHeight/2); 
        this.markers.put(marker, location);
    }
    
    public void calculateAnswers(QuestionGroup group, int blackThreshold, int whiteThreshold) {
        // Don't do anything if bubbles have not been analyzed
    	if (this.brightness == null) {
    		return;
        }
    	
    	int[][] brightnessArray = this.brightness.get(group);
        if (brightnessArray == null) {
            return;
        }
        
        int rowCount = group.getRowCount();
        int columnCount = group.getColumnCount();
        
        // Reset choice array
        if (this.choices == null) {
        	this.choices = new HashMap<QuestionGroup, int[][]>();
        }
        
        int[][] choices = new int[rowCount][columnCount];
        this.choices.put(group, choices);
        
        // Populate the choices array based on bubble brightnesses
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < columnCount; col++) {
                int brightness = brightnessArray[row][col];
                
                if (brightness < blackThreshold) {
                    choices[row][col] = -1;
                } else if (brightness >= whiteThreshold) {
                    choices[row][col] = 1;
                } else {
                    choices[row][col] = 0;
                }
            }   
        }
        
        // Set student number
        if (group.getOrientation() == Orientation.STUDENT_NUMBER) {
        	this.studentIdNumber = "";
        	for (int row = 0; row < choices.length; row++) { 
        		this.studentIdNumber += getChoices(group, row);
        	}
        }
        
        // Set check letter
        if (group.getOrientation() == Orientation.CHECK_LETTER) {
        	this.studentIdLetter = getChoices(group, 0);
        }
        
        this.studentId = this.studentIdNumber + this.studentIdLetter;
        
        // Validate answers
        validateAnswers(group);
        
        // Notify observers
        setChanged();
        notifyObservers();
    }
    
    /**
     * Creates a JPEG picture with an annotated answer sheet that can be shown to the student.
     * @return byte array containing a JPEG
     */
    public byte[] getFeedbackJpeg(SheetStructure structure) throws IOException {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	
    	Sheet sheet = this;
    	BufferedImage sheetBuffer = sheet.getAlignedBuffer();
        Graphics2D g = sheetBuffer.createGraphics();
    	
    	// Draw registration markers
        for (RegistrationMarker marker : structure.getRegistrationMarkers()) {
        	Point markerLocation = sheet.getRegistrationMarkerLocation(marker);
        	
        	if (markerLocation == null) {
        		g.setColor(Color.RED);   // Marker location not known
        	} else {
        		g.setColor(Color.BLUE);  // Marker location known
        	}
        	
        	g.drawRect((int)((marker.getX() - marker.getImageWidth() / 2.0)),
                    (int)((marker.getY() - marker.getImageHeight() / 2.0)),
                    (marker.getImageWidth()),
                    (marker.getImageHeight()));
        }
        
        // Draw bubbles
        for (QuestionGroup group : structure.getQuestionGroups()) {
            int columnCount = group.getColumnCount();
            int rowCount = group.getRowCount();
            int bubbleWidth = (group.getBubbleWidth());
            int bubbleHeight = (group.getBubbleHeight());
            int xOffset = (int)((group.getLeftX() - group.getBubbleWidth() / 2.0));
            int yOffset = (int)((group.getTopY() - group.getBubbleHeight() / 2.0));
            double columnSpacing = columnCount <= 1 ? 0 : group.getWidth() / (columnCount - 1);
            double rowSpacing = rowCount <= 1 ? 0 : group.getHeight() / (rowCount - 1);
            
            for (int row = 0; row < rowCount; row++) {
                int bubbleTopY = (int)(row * rowSpacing) + yOffset;
                int bubbleBottomY = bubbleTopY + bubbleHeight;
                
                for (int col = 0; col < columnCount; col++) {
                    int bubbleLeftX = (int)(col * columnSpacing) + xOffset;
                    int bubbleRightX = bubbleLeftX + bubbleWidth;
                    
                    int answer = sheet.getAnswer(group, row, col);
                    
                    if (answer < 0) {
                    	// Selected bubble
                    	g.setColor(Color.BLUE);
                        g.drawRect(bubbleLeftX, bubbleTopY, bubbleWidth, bubbleHeight);
                    } else if (answer > 0) {
                    	// Unselected bubble
                    	g.setColor(Color.GRAY);
                        
                        // Draw corners
                        g.drawLine(bubbleLeftX, bubbleTopY, bubbleLeftX + 4, bubbleTopY);
                        g.drawLine(bubbleLeftX, bubbleBottomY, bubbleLeftX + 4, bubbleBottomY);
                        g.drawLine(bubbleRightX - 4, bubbleTopY, bubbleRightX, bubbleTopY);
                        g.drawLine(bubbleRightX - 4, bubbleBottomY, bubbleRightX, bubbleBottomY);
                        g.drawLine(bubbleLeftX, bubbleTopY, bubbleLeftX, bubbleTopY + 4);
                        g.drawLine(bubbleRightX, bubbleTopY, bubbleRightX, bubbleTopY + 4);
                        g.drawLine(bubbleLeftX, bubbleBottomY - 4, bubbleLeftX, bubbleBottomY);
                        g.drawLine(bubbleRightX, bubbleBottomY - 4, bubbleRightX, bubbleBottomY);
                    } else {
                    	// Uncertain bubble
                        g.setColor(Color.RED);
                        g.drawRect(bubbleLeftX - 2, bubbleTopY - 2, bubbleWidth + 4, bubbleHeight + 4);
                        g.drawRect(bubbleLeftX, bubbleTopY, bubbleWidth, bubbleHeight);
                        
                        g.drawString("?", bubbleRightX + 5, bubbleBottomY); 
                    }
                }   
            }
        }

        g.dispose();

        // Write the JPEG
    	ImageIO.write(sheetBuffer, "jpeg", out);
    	
    	return out.toByteArray();
    }
}
