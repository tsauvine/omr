package omr;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Loads a project from an XML file. 
 * 
 * @author Tapio Auvinen
 */
public class Deserializer {

    private Document dom;
    private Project project;
    
    public Deserializer() {
        this.project = new Project();
    }
    
    public Project loadProject(File file) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
       
        dom = builder.parse(file);
        
        this.project = new Project();
        loadAnswerSheets();
        loadStructure();
        
        // Thresholds
        Element root = dom.getDocumentElement();
        project.getHistogram().setBlackThreshold(parseIntAttribute(root, "blackThreshold"));
        project.getHistogram().setWhiteThreshold(parseIntAttribute(root, "whiteThreshold"));
        
        return project;
    }
    
    private void loadStructure() {
    	SheetStructure structure = project.getSheetStructure();
    	Element root = dom.getDocumentElement();
        NodeList structureNodes = root.getElementsByTagName("structure");
        
        if (structureNodes.getLength() < 1) {
            return;
        }
        
        Element structureElement = (Element)structureNodes.item(0);
        
        // Load question groups
        loadQuestionGroups(structureElement);
        
        // Load registration markers
        loadRegistrationMarkers(structureElement);
        
        // Set reference sheet
        String referenceSheetId = structureElement.getAttribute("referenceSheet");
        if (referenceSheetId.length() > 0) {
        	Sheet referenceSheet = project.getSheetsContainer().getSheet(referenceSheetId);
        	structure.setReferenceSheet(referenceSheet);
        }
    }
    
    private void loadQuestionGroups(Element structureElement) {
        SheetStructure structure = project.getSheetStructure();
        
        // Load question groups
        NodeList groupNodes = structureElement.getElementsByTagName("bubble-group");
        
        for(int i = 0; i < groupNodes.getLength(); i++) {
            Element element = (Element)groupNodes.item(i);
            
            QuestionGroup group = new QuestionGroup(
                    parseIntAttribute(element, "leftX"),
                    parseIntAttribute(element, "topY"),
                    parseIntAttribute(element, "rightX"),
                    parseIntAttribute(element, "bottomY")
            );
            
            group.setOrientation(element.getAttribute("type"));
            group.setRowCount(parseIntAttribute(element, "rows"));
            group.setColumnCount(parseIntAttribute(element, "columns"));
            group.setBubbleWidth(parseIntAttribute(element, "bubbleWidth"));
            group.setBubbleHeight(parseIntAttribute(element, "bubbleHeight"));
            group.setIndexOffset(parseIntAttribute(element, "indexOffset"));
            
            // Load answer key
            loadAnswerKey(element, group);

            structure.addQuestionGroup(group);
        }
    }
        
    private void loadAnswerKey(Element groupElement, QuestionGroup group) {
        NodeList answerNodes = groupElement.getElementsByTagName("answer-key");
        
        if (answerNodes.getLength() < 1) {
            return;
        }
        
        Element element = (Element)answerNodes.item(0);
        
        String rawKey = element.getTextContent();
        String [] rawRows = rawKey.split(",");
        
        // Parse answer key
        boolean[][] answerKey = group.getAnswerKey();
        for (int row = 0; row < answerKey.length; row++) {
            for (int col = 0; col < answerKey[row].length; col++) {
                if (rawRows.length > row && rawRows[row].length() > col) {
                    answerKey[row][col] = rawRows[row].charAt(col) == '1';
                }
            }
        }
    }
    
    private void loadRegistrationMarkers(Element structureElement) {
        SheetStructure structure = project.getSheetStructure();
        
        // Load registration markers
        NodeList registrationNodes = structureElement.getElementsByTagName("registration-marker");
        
        for(int i = 0; i < registrationNodes.getLength(); i++) {
            Element element = (Element)registrationNodes.item(i);
            
            RegistrationMarker marker = new RegistrationMarker(
                    parseIntAttribute(element, "x"),
                    parseIntAttribute(element, "y")
            );
            
            marker.setSearchRadius(parseIntAttribute(element, "searchRadius"));

            structure.addRegistrationMarker(marker);
        }
    }
    
    private void loadAnswerSheets() throws IOException {
        SheetsContainer sheets = project.getSheetsContainer();
        
        // Get elements 
        Element root = dom.getDocumentElement();
        NodeList answerSheetsElement = root.getElementsByTagName("answers");
        
        if (answerSheetsElement.getLength() < 1) {
            return;
        }
        
        // Create question groups
        NodeList sheetNodes = ((Element)answerSheetsElement.item(0)).getElementsByTagName("sheet");
        
        for(int i = 0; i < sheetNodes.getLength(); i++) {
            Element sheetElement = (Element)sheetNodes.item(i);
            Sheet sheet = sheets.importSheet(sheetElement.getAttribute("id"), sheetElement.getAttribute("src"), parseIntAttribute(sheetElement, "page"));
            
            String rotation = sheetElement.getAttribute("rotation");
            if (rotation.length() > 0) {
            	sheet.setRotation(Integer.parseInt(rotation));
            }
            
        }
    }
    
    private int parseIntAttribute(Element element, String attributeName) {
    	try {
    		return Integer.parseInt(element.getAttribute(attributeName));
    	} catch (NumberFormatException e) {
    		return -1;
    	}
    }
  
}
