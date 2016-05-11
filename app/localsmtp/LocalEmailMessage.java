package localsmtp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.time.ZonedDateTime;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import play.libs.Mail;

/**
 * This is only used for local development 
 * and (maybe) on a test/staging server. 
 * 
 * See server/localsmtp/LocalEmailServer.java for more documentation. 
 * 
 * @author hansi
 *
 */
public class LocalEmailMessage{
	private File emlFile;
	public final String id; 
	
	public static class Meta{
		public String from; 
		public String to;
		public String subject;
		public boolean read; 
		public ZonedDateTime date;
	}
	
	public Meta meta; 
		
	
	private LocalEmailMessage( File emlFile ){
		this.emlFile = emlFile;
		this.id = emlFile.getName(); 
	}
	
	public static LocalEmailMessage create( File emlFile, Meta meta ){
		LocalEmailMessage msg = new LocalEmailMessage(emlFile); 
		msg.meta = meta; 
		msg.save(); 
		return msg; 
	}
	
	public static LocalEmailMessage load( File emlFile ){
		LocalEmailMessage msg = new LocalEmailMessage(emlFile); 
		msg.load(); 
		return msg; 
	}
	
	
	public boolean load(){
		File json = new File(emlFile.getAbsolutePath() + ".json"); 
		try( FileInputStream in = new FileInputStream( json ) ){
			meta = GsonMaster.get().fromJson(new InputStreamReader(in), Meta.class );
			return true; 
		} catch (IOException e) {
			meta = new Meta();
			return false; 
		}
	}
	
	public boolean save(){
		File jsonFile = new File(emlFile.getAbsolutePath() + ".json");
		String jsonData = GsonMaster.get().toJson(meta);
		
		try {
			Files.write(jsonFile.toPath(), jsonData.getBytes("UTF-8"));
			return true; 
		} catch (IOException e) {
			e.printStackTrace();
			return false; 
		} 
	}
	
	public MimeMessage parseData(){
		try(FileInputStream in = new FileInputStream(emlFile)) {
			return new MimeMessage(Mail.getSession(), in);
		} catch (MessagingException | IOException e) {
			return new MimeMessage(Mail.getSession()); 
		}
	}

	public InputStream getInputStream(){
		try{
			return new FileInputStream(emlFile);
		}
		catch( IOException e ){
			return null; 
		}
	}
	
	public long getLength(){
		return emlFile.length(); 
	}
}
