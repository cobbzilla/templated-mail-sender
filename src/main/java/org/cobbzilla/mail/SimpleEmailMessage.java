package org.cobbzilla.mail;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

/**
 * (c) Copyright 2013-2016 Jonathan Cobb.
 * This code is available under the Apache License, version 2: http://www.apache.org/licenses/LICENSE-2.0.html
 */
public class SimpleEmailMessage {

    @Getter @Setter private String fromName = null;
    @Getter @Setter private String fromEmail;
    public boolean hasFromEmail () { return !empty(fromEmail); }

    @Getter @Setter private String toName;
    @Getter @Setter private String toEmail;

    @Getter @Setter private String cc;
    public void addCc(String cc) { this.cc = empty(this.cc) ? cc : this.cc + "," + cc; }

    @Getter @Setter private String bcc;
    public void addBcc(String bcc) { this.bcc = empty(this.bcc) ? bcc : this.bcc + "," + bcc; }

    @Getter @Setter private String subject;
    public boolean hasSubject () { return !empty(subject); }

    @Getter @Setter private String message;
    @Getter @Setter private String htmlMessage;
    @JsonIgnore public boolean getHasHtmlMessage() { return htmlMessage != null && htmlMessage.length() > 0; }
    public boolean hasHtmlMessage () { return getHasHtmlMessage(); }

    @JsonIgnore public String getTextContent () { return getMessage(); }
    @JsonIgnore public String getHtmlContent () { return getHtmlMessage(); }

    @Getter @Setter private List<SimpleEmailImage> images;
    public boolean hasImages() { return !empty(images); }

    @Getter @Setter private List<SimpleEmailAttachment> attachments;
    public boolean hasAttachments() { return !empty(attachments); }
    public void addAttachment (SimpleEmailAttachment a) {
        if (attachments == null) attachments = new ArrayList<>();
        attachments.add(a);
    }

    @Override
    public String toString() {
        return "SimpleEmailMessage{" +
                "\nfromName='" + fromName + "'" +
                "\n fromEmail='" + fromEmail + "'" +
                "\n toName='" + toName + "'" +
                "\n toEmail='" + toEmail + "'" +
                "\n cc='" + cc + "'" +
                "\n bcc='" + bcc + "'" +
                "\n subject='" + subject + "'" +
                "\n message=" + ((message == null) ? "0" : message.length()) + " chars" +
                "\n htmlMessage=" + (htmlMessage == null ? 0 : htmlMessage.length()) + " chars" +
                "\n images=" + (images == null ? 0 : images.size()) +
                "\n attachments=" + (attachments == null ? 0 : attachments.size()) +
                "\n}";
    }

}
