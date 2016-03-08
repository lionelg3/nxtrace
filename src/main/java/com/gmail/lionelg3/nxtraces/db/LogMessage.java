package com.gmail.lionelg3.nxtraces.db;

import com.gmail.lionelg3.nxtraces.engine.NXTraces;
import org.hibernate.search.annotations.*;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collect mail traces from Unix systems
 *
 * @author Lionel G (lionelg3@gmail.com)
 *         Date: 21/12/15, Time: 14:25
 */
@Indexed
public class LogMessage implements Serializable {

    @Field(name="id", index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    private String filename;
    private byte[] rawData;
    private transient WeakReference<MimeMessage> mimeMessage;
    private Exception exception;

    public LogMessage() {
        super();
    }

    public LogMessage(String filename) {
        this();
        this.filename = filename;
        getMimeMessage();
    }

    public MimeMessage getMimeMessage() {
        this.exception = null;
        if (mimeMessage == null || mimeMessage.get() == null) {
            try {
                if (rawData == null) {
                    Path path = (NXTraces.CONFIGURATION == null) ? Paths.get(filename) : Paths.get(NXTraces.CONFIGURATION.getString("repository.path") + "/" + filename);
                    rawData = Files.readAllBytes(path);
                }
                MimeMessage rawMessage = new MimeMessage(Session.getDefaultInstance(new Properties()), new ByteArrayInputStream(rawData));
                this.mimeMessage = new WeakReference<>(rawMessage);
                rawMessage.getMessageID();
            } catch (MessagingException | IOException e) {
                this.exception = e;
            }
        }
        return this.mimeMessage.get();
    }

    public Exception getException() {
        return exception;
    }

    public boolean hasException() {
        return (this.exception != null);
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getRawData() {
        return rawData;
    }

    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    public String getMessageID() {
        try {
            return getMimeMessage().getMessageID();
        } catch (MessagingException e) {
            return null;
        }
    }

    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    public String getFrom() {
        try {
            Address[] addresses = getMimeMessage().getFrom();
            return addresses != null && addresses.length > 0 ? addresses[0].toString() : null;
        } catch (MessagingException e) {
            return null;
        }
    }

    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    public String getRecipients() {
        List<String> recipientsList = new ArrayList<>();
        recipientsList.addAll(
                getRecipients(Message.RecipientType.TO)
                        .stream()
                        .map(InternetAddress::toString)
                        .collect(Collectors.toList())
        );
        recipientsList.addAll(
                getRecipients(Message.RecipientType.CC)
                        .stream()
                        .map(InternetAddress::toString)
                        .collect(Collectors.toList())
        );
        StringBuffer list = new StringBuffer();
        for (int i = 0; i < recipientsList.size(); i++) {
            list.append(recipientsList.get(i));
            if (recipientsList.size() - 1 != i)
                list.append(", ");
        }
        return list.toString();
    }

    public List<InternetAddress> getRecipients(Message.RecipientType type) {
        try {
            if (getMimeMessage().getRecipients(type) == null) return new ArrayList<>();
            else return Arrays.asList((InternetAddress[]) getMimeMessage().getRecipients(type));
        } catch (MessagingException e) {
            return null;
        }
    }

    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    public String getSubject() {
        try {
            return getMimeMessage().getSubject();
        } catch (MessagingException e) {
            return null;
        }
    }

    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    public Date getSentDate() {
        try {
            return getMimeMessage().getSentDate();
        } catch (MessagingException e) {
            return null;
        }
    }

    public int getSize() {
        try {
            return getMimeMessage().getSize();
        } catch (MessagingException e) {
            return -1;
        }
    }

    public String getContentType() {
        try {
            return getMimeMessage().getContentType();
        } catch (MessagingException e) {
            return null;
        }
    }

    public Enumeration getAllHeaders() {
        try {
            return getMimeMessage().getAllHeaders();
        } catch (MessagingException e) {
            return null;
        }
    }

    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    public String getContent() {
        try {
            Object content = getMimeMessage().getContent();
            return content != null ? content.toString() : null;
        } catch (IOException | MessagingException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        try {
            return "LogMessage{" +
                    "filename='" + filename + '\'' +
                    ", raw='" + new String(rawData) + '\'' +
                    "}";
        } catch (Exception e) {
            return "LogMessage{" +
                    "filename='" + filename + '\'' +
                    "}";
        }
    }
}
