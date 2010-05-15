package omr;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Represents one page (one answer sheet) from a PdfDocument. Multiple
 * PdfSheets may have a single PdfDocument as their common data source. 
 * 
 * @author Tapio Auvinen
 */
public class PdfSheet extends Sheet {
	// TODO: rename this to MultipageSheet or something. Multipage TIFFs can be handled the same way.
	
    private PdfDocument pdfDocument;
    private int page;
    
    public PdfSheet(PdfDocument pdfDocument, int page, String filePath, String fileName) {
        super(filePath, fileName);
        this.pdfDocument = pdfDocument;
        this.page = page;
        this.id = fileName + "(" + page + ")";
    }

    @Override
    public BufferedImage getUnalignedBuffer() throws IOException {
        return getUnalignedBuffer(1.0);
    }
    
    @Override
    public BufferedImage getUnalignedBuffer(double zoomLevel) throws IOException {
        return this.pdfDocument.renderPage(zoomLevel, this.page);
    }
}
