package omr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

/**
 * Saves projects as XML.
 * 
 * @author Tapio Auvinen
 */
public class Serializer {
    private Document dom;
    
    /**
     * Saves a project as XML.
     * @param project Project to save.
     * @param file File to save the project in. If the file exists, it will be overwritten.
     * @throws Exception
     */
    public void saveProject(Project project, File file) throws Exception {
        // Initialize the XML document
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        dom = db.newDocument();
        
        // Create dom tree
        createDomTree(project);
        
        // Write to file
        DOMImplementation domImpl = dom.getImplementation();
        Object feature = domImpl.getFeature("LS", "3.0");
        if (feature == null) {
            throw new Exception("XML writer does not support LS 3.0");
        }
        
        DOMImplementationLS lsImpl = (DOMImplementationLS)feature;
        LSOutput lsOutput = lsImpl.createLSOutput();
        LSSerializer lsSerializer = lsImpl.createLSSerializer();
        DOMConfiguration lsConfig = lsSerializer.getDomConfig();
        lsConfig.setParameter("format-pretty-print", Boolean.TRUE);

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        lsOutput.setCharacterStream(writer);

        lsSerializer.write(dom, lsOutput);
        writer.close();
    }
    
    private void createDomTree(Project project) {
        Element root = dom.createElement("omr-project");
        root.setAttribute("version", "0.1");
        root.setAttribute("blackThreshold", Integer.toString(project.getHistogram().getBlackThreshold()));
        root.setAttribute("whiteThreshold", Integer.toString(project.getHistogram().getWhiteThreshold()));
        dom.appendChild(root);
        
        // Sheet structure
        SheetStructure structure = project.getSheetStructure();
        Element structureElement = dom.createElement("structure");

        // Set reference sheet
        Sheet referenceSheet = structure.getReferenceSheet();
        if (referenceSheet != null) {
        	structureElement.setAttribute("referenceSheet", referenceSheet.getId());
        }
        
        root.appendChild(structureElement);
        
        // Question groups
        for (QuestionGroup group : structure.getQuestionGroups()) {
        	structureElement.appendChild(createGroupElement(group));
        }
        
        // Registration markers
        for (RegistrationMarker marker : structure.getRegistrationMarkers()) {
            structureElement.appendChild(createRegistrationElement(marker));
        }
        
        // Answer sheets
	    Element sheetsElement = dom.createElement("answers");
	    root.appendChild(sheetsElement);
        
        for (Sheet sheet : project.getAnswerSheets()) {
            sheetsElement.appendChild(createAnswerSheetElement(sheet));
        }
    }
    
    private Element createGroupElement(QuestionGroup group){
        Element groupElement = dom.createElement("bubble-group");
        groupElement.setAttribute("rows", Integer.toString(group.getRowCount()));
        groupElement.setAttribute("columns", Integer.toString(group.getColumnCount()));
        groupElement.setAttribute("leftX", Integer.toString(group.getLeftX()));
        groupElement.setAttribute("topY", Integer.toString(group.getTopY()));
        groupElement.setAttribute("rightX", Integer.toString(group.getRightX()));
        groupElement.setAttribute("bottomY", Integer.toString(group.getBottomY()));
        groupElement.setAttribute("bubbleWidth", Integer.toString(group.getBubbleWidth()));
        groupElement.setAttribute("bubbleHeight", Integer.toString(group.getBubbleHeight()));
        groupElement.setAttribute("indexOffset", Integer.toString(group.getIndexOffset()));
        groupElement.setAttribute("type", group.getOrientation().toString());

        groupElement.appendChild(createAnswerKeyElement(group));
        
        return groupElement;
    }
    
    private Element createAnswerKeyElement(QuestionGroup group) {
    	Element element = dom.createElement("answer-key");
    	
    	StringBuilder sb = new StringBuilder();
    	for (boolean[] questions : group.getAnswerKey()) {
    		for (boolean alternative : questions) {
    			sb.append(alternative ? '1' : '0');
    		}
    		sb.append(',');
    	}
    	
    	if (sb.length() > 0) {
    		// Remove the extra ','
    		sb.deleteCharAt(sb.length() - 1);
    	}
    	
    	element.setTextContent(sb.toString());
    	
    	return element;
    }
    
    
    private Element createRegistrationElement(RegistrationMarker marker){
        Element markerElement = dom.createElement("registration-marker");
        markerElement.setAttribute("x", Integer.toString(marker.getX()));
        markerElement.setAttribute("y", Integer.toString(marker.getY()));
        markerElement.setAttribute("width", Integer.toString(marker.getImageWidth()));
        markerElement.setAttribute("height", Integer.toString(marker.getImageHeight()));
        markerElement.setAttribute("searchRadius", Integer.toString(marker.getSearchRadius()));

        return markerElement;
    }
    
    private Element createAnswerSheetElement(Sheet sheet){
        Element sheetElement = dom.createElement("sheet");
        sheetElement.setAttribute("id", sheet.getId());
        sheetElement.setAttribute("src", sheet.getFilePath());
        sheetElement.setAttribute("page", Integer.toString(sheet.getPage()));
        sheetElement.setAttribute("rotation", Integer.toString(sheet.getRotation()));
        
        return sheetElement;
    }
}
