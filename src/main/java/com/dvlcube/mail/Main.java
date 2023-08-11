package com.dvlcube.mail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;

/**
 * https://github.com/ulisseslima/bash-utils/blob/master/sendmail.sh
 * 
 * @since 11 de ago de 2023
 * @author ulisses
 */
public class Main {
	public static String forbidden = ".*\\.sh|.*\\.exe|.*\\.bat|.*\\.jar|.*\\~";
	public static boolean debug = false;

	public static final FilenameFilter filter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return !name.matches(Main.forbidden);
		}
	};

	public static void main(String[] args) throws Exception {
		MultiPartEmail email;

		boolean html = getBool("html", false);
		String server = get("server", "smtp.gmail.com");
		int port = getInt("port", 465);
		String user = get("user");
		String password = get("password");
		boolean ssl = getBool("ssl", true);
		boolean tls = getBool("tls", false);
		String from = get("from", "me");
		String[] to = getArray("to");
		String[] cc = getArray("cc");
		String[] bcc = getArray("bcc");
		String subject = get("subject");
		String message = get("message");
		File[] attachments = getAttachments();
		validateAttachments(attachments);

		debug = getBool("debug", false);

		if (html) {
			debug("+ html");
			email = new HtmlEmail();
			((HtmlEmail) email).setHtmlMsg(message);
		} else {
			debug("+ multipart");
			email = new MultiPartEmail();
			email.setMsg(message);
		}
		email.setHostName(server);
		email.setSmtpPort(port);
		email.setAuthenticator(new DefaultAuthenticator(user, password));
		email.setSSLOnConnect(ssl);
		if (tls) {
			debug("+ tls");
			email.setStartTLSEnabled(tls);
			email.setStartTLSRequired(tls);
		}
		email.setFrom(from);
		email.setSubject(subject);
		email.addTo(to);
		email.addReplyTo(to[0]);
		if (cc != null) {
			debug("cc: %s", Arrays.toString(cc));
			email.addCc(cc);
		}

		if (bcc != null) {
			debug("bcc: %s", Arrays.toString(bcc));
			email.addBcc(bcc);
		}

		for (File file : attachments) {
			EmailAttachment attachment = new EmailAttachment();
			attachment.setDisposition("attachment");
			attachment.setPath(file.getAbsolutePath());
			attachment.setDescription(file.getPath());
			attachment.setName(file.getName());
			email.attach(attachment);
		}

		email.setDebug(debug);
		email.send();
	}

	private static void validateAttachments(File[] attachments) throws FileNotFoundException {
		for (File file : attachments) {
			if (!file.exists())
				throw new FileNotFoundException(file.getAbsolutePath());

			debug("attachment validated: %s", file);
		}
	}

	public static String get(String prop, String defaultValue) {
		String property = System.getProperty(prop, defaultValue);
		debug("> %s=%s [%s]", prop, property, defaultValue);
		return property;
	}

	public static String get(String prop) {
		return get(prop, "unspecified");
	}

	public static String[] getArray(String prop) {
		String string = get(prop, null);
		if (string == null)
			return null;

		if (string.contains(";"))
			return string.split(";");

		return string.split(",");
	}

	public static int getInt(String prop, int defaultValue) {
		return Integer.parseInt(get(prop, String.valueOf(defaultValue)));
	}

	public static boolean getBool(String prop, boolean defaultValue) {
		return Boolean.valueOf(get(prop, String.valueOf(defaultValue))).booleanValue();
	}

	private static File[] getAttachments() {
		String attachParam = get("attach");
		if (attachParam == null || attachParam.length() < 1) {
			debug("no attachments");
			return new File[0];
		}

		String[] filenames = attachParam.split(";");
		List<File> files = new ArrayList<File>(filenames.length);
		for (int i = 0; i < filenames.length; i++) {
			File file = new File(filenames[i]);
			debug("attaching: %s", file);
			if (!file.exists()) {
				debug("ignoring file not found: %s", file);
				continue;
			}

			if (file.getName().matches(forbidden)) {
				debug("forbidden extension, attachment will be skipped: %s", file);
				continue;
			}

			if (file.isDirectory()) {
				debug("- as directory: %s", file);
				files.addAll(Arrays.asList(file.listFiles(filter)));
			} else {
				debug("- as file: %s", file);
				files.add(file);
			}
		}
		return files.<File>toArray(new File[0]);
	}

	private static void debug(String message, Object... args) {
		if (debug) {
			System.out.println(String.format(message, args));
		}
	}
}
