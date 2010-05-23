package omr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFRenderer;

/**
 * Class PdfDocument represents a multi-page PDF document which may contain
 * answer sheets from multiple students. Multiple PdfSheet objects may use
 * one PdfDocument as their common data source, so that the same Pdf file
 * does not need to be opened and closed repeatedly.
 * 
 * @author Tapio Auvinen
 */

public class PdfDocument {
	private PDFFile pdfFile;
	
	public PdfDocument(File file) throws FileNotFoundException, IOException {
	    RandomAccessFile raf = new RandomAccessFile(file, "r");
	    FileChannel channel = raf.getChannel();
	    ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
	    this.pdfFile = new PDFFile(buf);
	}
	
	/**
	 * Renders one page from the pdf.
	 * @param zoom 1.0 = default zoom level. DPI = 96
	 * @param pageNumber
	 * @return Buffered image containing the page. Can return null if something goes wrong.
	 */
	synchronized public BufferedImage renderPage(double zoom, int pageNumber) {
		// Get the right page
	    PDFPage page = this.pdfFile.getPage(pageNumber);
	
	    int width = (int)(page.getWidth() * zoom);
	    int height = (int)(page.getHeight() * zoom);
	    
	    // create and configure a graphics object
	    BufferedImage buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	    Graphics2D g2 = buffer.createGraphics();
	    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	
	    // Render the page
	    PDFRenderer renderer = new PDFRenderer(page, g2, new Rectangle(0, 0, buffer.getWidth(), buffer.getHeight()), null, Color.WHITE);
	    try {
	    	page.waitForFinish();
	    } catch (InterruptedException e) {
	    	return null;
	    }
	    renderer.run();
	    
	    return buffer;
	}
	
	/**
	 * Returns the number of pages in this document.
	 */
	public int getPageCount() {
		return this.pdfFile.getNumPages();
	}
}
