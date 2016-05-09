package controllers.localsmtp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import localsmtp.LocalEmailMessage;
import localsmtp.LocalSmtpPlugin;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;

import play.Logger;
import play.Play;
import play.mvc.Controller;

/**
 * This is the webinterface to query the emails that were sent through the local smtp server. 
 * It is reachable through http://localhost:9000/@emails
 * @author hansi
 */
public class LocalEmailBrowser extends Controller{

	@Before
	private static void checkAccess(){
		if( !"localsmtp".equals(Play.configuration.getProperty("mail.smtp.user"))){
			forbidden("Local email services not enabled");
		}
		else if( Play.mode.isProd() ){
			Logger.warn("Warning, localsmtp is accessed in prod mode. It is highly suggested to use it only for dev and test" );
		}
	}
	
	public static void index(){
		LocalSmtpPlugin plugin = Play.plugin(LocalSmtpPlugin.class); 
		List<LocalEmailMessage> emails = plugin.getRecentEmails();   
		render(emails); 
	}
	
	public static void embed( String id ){
		LocalSmtpPlugin plugin = Play.plugin(LocalSmtpPlugin.class); 
		LocalEmailMessage email = plugin.getEmailById(id); 
		notFoundIfNull(email);
		
		if( !email.meta.read ){
			email.meta.read = true; 
			email.save(); 
		}
		
		MimeMessage message = email.parseData();
		String body, bodyClass;
		try {
			body = getTextFromMessage(message);
			Logger.error(message.getContentType()); 
			bodyClass = StringUtils.startsWith(message.getContentType(),"text/plain")?"plain":"html"; 
		} catch (IOException | MessagingException e) {
			body = "Failed to parse body"; 
			bodyClass=""; 
		}
		
		render(email, body, bodyClass); 
	}
	
	public static void download( String id ){
		LocalSmtpPlugin plugin = Play.plugin(LocalSmtpPlugin.class); 
		LocalEmailMessage email = plugin.getEmailById(id); 
		notFoundIfNull(email);
		
		renderBinary(email.getInputStream(), email.meta.subject+".eml", email.getLength(), "message/rfc822", false);
	}
	
	private static String getTextFromMessage(Message message) throws MessagingException, IOException{
	    String result = "";
	    if (message.isMimeType("text/plain")) {
	        result = message.getContent().toString();
	    } else if (message.isMimeType("multipart/*")) {
	        MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
	        result = getTextFromMimeMultipart(mimeMultipart);
	    }
	    return result;
	}

	private static String getTextFromMimeMultipart(
	        MimeMultipart mimeMultipart) throws MessagingException, IOException{
	    StringBuilder result = new StringBuilder();
	    int count = mimeMultipart.getCount();
	    for (int i = 0; i < count; i++) {
	        BodyPart bodyPart = mimeMultipart.getBodyPart(i);
	        if (bodyPart.isMimeType("text/plain")) {
	            result.append(bodyPart.getContent());
	            break; // without break same text appears twice in my tests
	        } else if (bodyPart.isMimeType("text/html")) {
	            String html = (String) bodyPart.getContent();
	            result.append(html);
	        } else if (bodyPart.getContent() instanceof MimeMultipart){
	            result.append(getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent()));
	        }
	    }
	    return result.toString();
	}
	
	private static String toHtml(String text){
		return StringEscapeUtils.escapeHtml(text); 		
	}
}
