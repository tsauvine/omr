package omr;

import java.io.FileInputStream;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import omr.QuestionGroup.Orientation;

/**
 * Sends feedback emails.
 * 
 * @author Tapio Auvinen
 */
public class Mailer {

	public Mailer() {
		
	}
	
	/**
	 * Sends the feedback for one sheet.
	 */
	public void sendFeedback(Project project, Sheet sheet) throws Exception {
	    String studentId = sheet.getStudentId();
	    if (studentId == null || studentId.length() < 1) {
	        System.err.println("Answer sheet " + sheet.getId() + " has no student id. Skipping email.");
	    }
	    
	    // Load properties
	    Properties properties = new Properties() ;
        properties.load(new FileInputStream("omr.properties"));
        
	    String host = properties.getProperty("smtp-host");
		String from = properties.getProperty("from-address");
		String to = properties.getProperty("address-prefix") + sheet.getStudentId() + properties.getProperty("address-suffix");

		Properties sysProps = System.getProperties();
		sysProps.put("mail.smtp.host", host);

		// Get session
		Session session = Session.getDefaultInstance(properties, null);
		
		// Create the message body
		BodyPart messageBody = new MimeBodyPart();
		messageBody.setText(generateFeedback(project.getGradingScheme(), project.getSheetStructure(), sheet));

		// Create the attachment
		BodyPart messageAttachment = new MimeBodyPart();
		DataSource source = new ByteArrayDataSource(sheet.getFeedbackJpeg(project.getSheetStructure()), "image/jpeg");
		messageAttachment.setDataHandler(new DataHandler(source));
		messageAttachment.setFileName("answer.jpeg");
		
		// Create the multipart message
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBody);
		multipart.addBodyPart(messageAttachment);

		// Define message
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		message.setSubject(properties.getProperty("subject"));
		message.setContent(multipart);

		// Send the message
		Transport.send(message);
	}
	
	/**
	 * Generates the feedback text.
	 */
	private String generateFeedback(GradingScheme grading, SheetStructure structure, Sheet sheet) {
	    StringBuilder sb = new StringBuilder();
	    
	    sb.append("Student ID: " + sheet.getStudentId() + "\n\n");
	    
	    // Header
	    sb.append("question: answer [correct] --> score\n");
	    
	    for (QuestionGroup group : structure.getQuestionGroups()) {
	        // Skip non-questions
	        if (group.getOrientation() != Orientation.VERTICAL && group.getOrientation() != Orientation.HORIZONTAL) {
                continue;
            }
	        
	        // Iterate though each question
	        for (int question = 0; question < group.getQuestionsCount(); question++) {
	            sb.append(group.getQuestionNumber(question) + ": ");       // Question number
	            sb.append(String.format("%1$-" + 1 + "s", sheet.getChoices(group, question)) + " ");       // Student's answer
	            sb.append(String.format("%1$-" + 4 + "s", "[" + group.getCorrectChoices(question) + "]")); // Correct answer
	            sb.append("--> ");
	            sb.append(grading.getScore(sheet, group, question));       // Score
	            sb.append("\n");
	        }
	    }
	    
        // Total score
	    sb.append("\nTotal: ");
        sb.append(grading.getScore(sheet, structure) + "\n");
	    
	    return sb.toString();
	}
	
}
