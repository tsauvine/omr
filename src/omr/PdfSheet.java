package omr;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Represents one page (one answer sheet) from a PdfDocument. Multiple
 * PdfSheets may have a single PdfDocument as their common data source. 
 * 
 * Sheet images are always rendered on the fly and not cached. Caching is
 * mostly needed when changing zoom level, and vector graphics needs to
 * re-rendered when zooming.
 * 
 * @author Tapio Auvinen
 */
public class PdfSheet extends Sheet {
	// TODO: rename this to MultipageSheet or something. Multipage TIFFs can be handled the same way.
	
    private PdfDocument pdfDocument;
    private int page;
    
    /**
     * Constructor
     * @param pdfDocument PdfDocument where the sheet is taken. 
     * @param page Which page to take
     * @param filePath Whole path of the document, including filename.
     * @param fileName Filename of the document. 
     */
    public PdfSheet(PdfDocument pdfDocument, int page, String filePath, String fileName) {
        super(filePath, fileName);
        this.pdfDocument = pdfDocument;
        this.page = page;
        this.id = fileName + "(" + page + ")";
    }

    /**
     * Returns the original unaligned sheet image at 100% zoom level.
     */
    @Override
    public BufferedImage getUnalignedBuffer() throws IOException {
        return getUnalignedBuffer(1.0);
    }
    
    /**
     * Returns the original unaligned sheet image at the requested zoom level.
     * @param zoomLevel 1.0 means 100%
     */
    @Override
    public BufferedImage getUnalignedBuffer(double zoomLevel) throws IOException {
        return this.pdfDocument.renderPage(zoomLevel, this.page);
    }
}
