package de.trautwig.fax.mail2fax;

import gnu.hylafax.HylaFAXClient;
import gnu.hylafax.Job;
import gnu.inet.ftp.ServerResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SpringBootApplication
@Import(HylafaxProperties.class)
public class Mail2faxApplication implements CommandLineRunner, SimpleMessageListener {

	public static void main(String[] args) {
		SpringApplication.run(Mail2faxApplication.class, args);
	}

	private final Logger log = LoggerFactory.getLogger(Mail2faxApplication.class);

	@Value("${smtp.port:25}")
	int smtpPort;

	@Autowired
	HylafaxProperties hylafaxProperties;

	@Override
	public void run(String... args) throws Exception {
		SMTPServer server = new SMTPServer(new SimpleMessageListenerAdapter(this));
		server.setSoftwareName("Mail2Fax Gateway");
		server.setPort(smtpPort);
		server.start();
	}

	@Override
	public boolean accept(String from, String recipient) {
		try {
			FaxAddress destination = new FaxAddress(recipient);
			log.info("Accepting mail from: {} for: {}", from, recipient);
			return true;
		} catch (Exception e) {
			log.warn("Rejecting mail from: {} for: {}", from, recipient);
			return false;
		}
	}

	@Override
	public void deliver(String from, String recipient, InputStream data) throws TooMuchDataException, IOException {
		FaxAddress destination = new FaxAddress(recipient);
		log.info("Delivering mail from: {} for: {}", from, destination.getGlobalPhone());
		try {
			MimeMessage msg = new MimeMessage(null, data);
			List<Part> attachments = extractAttachments(msg);
			if (attachments.isEmpty()) {
				throw new RejectException("Unsupported message type");
			}

			HylaFAXClient client = new HylaFAXClient();
			try {
				client.open(hylafaxProperties.getServer(), hylafaxProperties.getPort());
				if (StringUtils.hasText(hylafaxProperties.getUsername())) {
					client.user(hylafaxProperties.getUsername());
					if (StringUtils.hasText(hylafaxProperties.getPassword())) {
						client.pass(hylafaxProperties.getPassword());
					}
				}

				Job job = client.createJob();
				job.setDialstring(destination.getGlobalPhone());
				if (StringUtils.hasText(hylafaxProperties.getSender())) {
					job.setFromUser(hylafaxProperties.getSender());
				}
				job.setPageChop(Job.CHOP_DEFAULT);
				job.setPageWidth(Paper.A4.getWidth());
				job.setPageLength(Paper.A4.getHeight());
				job.setVerticalResolution(Job.RESOLUTION_LOW);
				job.setSendTime("NOW");

				client.mode(HylaFAXClient.MODE_STREAM);
				client.type(HylaFAXClient.TYPE_IMAGE);
				for (Part attachment : attachments) {
					try (InputStream in = attachment.getInputStream()) {
						String faxDoc = client.putTemporary(in);
						job.addDocument(faxDoc);
					}
				}

				client.submit(job);
			} catch (SocketTimeoutException e) {
				throw new IOException("Unable to deliver fax", e);
			} catch (ServerResponseException e) {
				throw new IOException("Unable to deliver fax", e);
			} finally {
				try {
					client.quit();
				} catch (ServerResponseException e) {
					// ignore
				}
			}
		} catch (MessagingException e) {
			throw new IOException(e);
		}
	}


	List<Part> extractAttachments(MimeMessage msg) throws MessagingException, IOException {
		if (msg.getContentType().startsWith("image/tiff")) {
			return Collections.singletonList(msg);
		}

		if (msg.getContent() instanceof MimeMultipart) {
			MimeMultipart multipart = (MimeMultipart) msg.getContent();
			List<Part> result = new ArrayList<>();
			int parts = multipart.getCount();
			for (int i=0; i < parts; i++) {
				BodyPart part = multipart.getBodyPart(i);
				if (isTiffAttachment(part)) {
					result.add(part);
				}
			}
			return result;
		}

		return Collections.emptyList();
	}

	private boolean isTiffAttachment(Part part) throws MessagingException {
		String contentType = part.getContentType();
		int sep = contentType.indexOf(';');
		if (sep != -1) {
			contentType = contentType.substring(0, sep);
		}
		return "image/tiff".equalsIgnoreCase(contentType);
	}
}
