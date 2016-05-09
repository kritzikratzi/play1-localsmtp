package localsmtp;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import play.Play;
import play.PlayPlugin;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.libs.Crypto;

/**
 * Based on the 'Wiser' mail server demo, 
 * part of SubEthaSMTP, Apache licensed. 
 * 
 * This tiny thingie thing is a mail server running locally that 
 * swallows all emails and stores them in a database. 
 * 
 * 
 * Original authors:
 * @author Jon Stevens
 * @author Jeff Schnitzer
 * 
 * Adopted for the play framework by 
 * @author Hansi Raber
 */
public class LocalSmtpPlugin extends PlayPlugin implements SimpleMessageListener{
	
	private final static Logger log = LoggerFactory.getLogger(LocalSmtpPlugin.class);
	private SMTPServer server;

	// internal storage
	private HashMap<String, LocalEmailMessage> messages = new HashMap<>(); 
	
	// sorted index for displaying the messages
	private LinkedList<LocalEmailMessage> sortedMessages = new LinkedList<>(); 
	
	// and a lock. 
	// because locks are great! 
	ReentrantLock messageLock = new ReentrantLock(); 
	
	/**
	 * Create a new SMTP server with this class as the listener. The default
	 * port is 25. Call setPort()/setHostname() before calling start().
	 */
	public LocalSmtpPlugin() {
		// todo: retry if port is in use. 
		server = new SMTPServer(new SimpleMessageListenerAdapter(this));
	}

	/** Always accept everything */
	public boolean accept(String from, String recipient) {
		if (log.isDebugEnabled())
			log.debug("Accepting mail from " + from + " to " + recipient);

		return true;
	}

	/** Store message in db */
	public void deliver(String from, String recipient, InputStream data)
			throws TooMuchDataException, IOException {
		if (log.isDebugEnabled())
			log.debug("Delivering mail from " + from + " to " + recipient);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		data = new BufferedInputStream(data);

		// read the data from the stream
		int current;
		while ((current = data.read()) >= 0) {
			out.write(current);
		}

		byte[] bytes = out.toByteArray();

		if (log.isDebugEnabled())
			log.debug("Creating message from data with " + bytes.length
					+ " bytes");

		// create a new WiserMessage.
		
		
		try {
			MimeMessage mail = new MimeMessage(getSession(), new ByteArrayInputStream(bytes));
			
			LocalEmailMessage.Meta meta = new LocalEmailMessage.Meta(); 
			meta.from = from; 
			meta.to = recipient;
			meta.subject = mail.getSubject(); 
			meta.date = ZonedDateTime.now();
			
			File emlFile = new File(dataDir(), "msg-" + UUID.randomUUID().toString() + ".eml"); 
			Files.write(emlFile.toPath(), bytes, StandardOpenOption.CREATE);
			
			LocalEmailMessage msg = LocalEmailMessage.create(emlFile, meta);
			
			messageLock.lock(); 
			messages.put(msg.id, msg); 
			sortedMessages.add(msg); 
			messageLock.unlock();
			
		} catch (MessagingException e) {
			e.printStackTrace();
		} 
	}

	/**
	 * Creates the JavaMail Session object for parsing mimemails. 
	 */
	public static Session getSession() {
		return Session.getDefaultInstance(new Properties());
	}

	/**
	 * @return the server implementation
	 */
	public SMTPServer getServer() {
		return this.server;
	}
	
	/**
     * Called at application start (and at each reloading)
     * Time to start stateful things.
     */
    public void onApplicationStart() {
		if( "localsmtp".equals(Play.configuration.getProperty("mail.smtp.user"))){
			server.setPort(Integer.parseInt(Play.configuration.getProperty("mail.smtp.port"))); 
			server.start();
			
			// Load all messages 
			if( messages.isEmpty() ){
				messageLock.lock(); 
				File dataDir = dataDir();
				File [] files = dataDir.listFiles(f -> f.getName().endsWith(".eml") && f.getName().startsWith("msg-"));
				
				ArrayList<LocalEmailMessage> all = new ArrayList<>(files.length);
				
				for( File eml : files ){
					LocalEmailMessage msg = LocalEmailMessage.load(eml); 
					messages.put(msg.id, msg);
					all.add(msg); 
				}
				
				all.sort((a,b) -> a.meta.date.compareTo(b.meta.date));
				sortedMessages.addAll(all); 
				messageLock.unlock(); 
			}
		}
    }
    
    private File dataDir(){
    	File dataDir = new File(Play.applicationPath.getAbsolutePath() + "/data/emails" );
    	if( !dataDir.exists() ){
    		log.info("Creating localsmtp data diretory " + dataDir.getAbsolutePath() );
    		if( !dataDir.mkdirs() ){
    			throw new RuntimeException("Couldn't create directory " + dataDir.getAbsolutePath() ); 
    		}
    	}
    	return dataDir; 
    }

    /**
     * Called at application stop (and before each reloading)
     * Time to shutdown stateful things.
     */
    public void onApplicationStop() {
    	if( server != null && server.isRunning() ){
    		server.stop();
    	}
    }

    
    /**
     * Returns the last 100 mails 
     * @return
     */
	public List<LocalEmailMessage> getRecentEmails() {
		messageLock.lock(); 
		int max = 100;  
		ArrayList<LocalEmailMessage> recent = new ArrayList<LocalEmailMessage>(max); 
		Iterator<LocalEmailMessage> it = sortedMessages.descendingIterator();
		while( recent.size() < max && it.hasNext() ){
			recent.add(it.next()); 
		}
		messageLock.unlock(); 
		
		return recent; 
	}

	public LocalEmailMessage getEmailById(String id) {
		messageLock.lock(); 
		LocalEmailMessage result = messages.get(id); 
		messageLock.unlock(); 
		return result; 
	}

}
