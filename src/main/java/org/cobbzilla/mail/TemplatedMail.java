package org.cobbzilla.mail;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

/**
 * (c) Copyright 2013-2016 Jonathan Cobb.
 * This code is available under the Apache License, version 2: http://www.apache.org/licenses/LICENSE-2.0.html
 */
@Accessors(chain=true)
@EqualsAndHashCode(of={"templateName", "locale", "fromName", "fromEmail", "toName", "toEmail", "cc", "bcc", "parameters"})
@ToString         (of={"templateName", "locale", "fromName", "fromEmail", "toName", "toEmail", "cc", "bcc", "parameters"})
public class TemplatedMail implements Comparable<TemplatedMail> {

    @Override public int compareTo(TemplatedMail t) { return Integer.compare(hashCode(), t.hashCode()); }

    @NotNull @Getter @Setter private String templateName;
    @Getter @Setter private String locale;

    @Getter @Setter private String fromName;
    public boolean hasFromName () { return !empty(fromName); }

    @NotNull @Getter @Setter private String fromEmail;
    public boolean hasFromEmail () { return !empty(fromEmail); }

    @Getter @Setter private String toName;
    @NotNull @JsonProperty @Getter @Setter private String toEmail;

    @Getter @Setter private String cc;   // can be a comma-separated list of emails
    public boolean hasCc () { return !empty(cc); }

    @Getter @Setter private String bcc;  // can be a comma-separated list of emails
    public boolean hasBcc () { return !empty(bcc); }

    @NotNull @Getter @Setter private Map<String, Object> parameters;

    public TemplatedMail setParameter (String name, Object value) {
        if (this.parameters == null) this.parameters = new HashMap<>();
        this.parameters.put(name, value);
        return this;
    }
    public TemplatedMail addParameters (Map<String, Object> params) {
        if (this.parameters == null) this.parameters = new HashMap<>();
        this.parameters.putAll(params);
        return this;
    }

    @Getter @Setter private List<SimpleEmailAttachment> attachments;
    public TemplatedMail addAttachment (SimpleEmailAttachment attachment) {
        if (attachments == null) attachments = new ArrayList<>();
        attachments.add(attachment);
        return this;
    }

    @Getter @Setter private List<SimpleEmailImage> images;
    public TemplatedMail addImages(List<SimpleEmailImage> images) {
        if (images == null) return this;
        if (this.images == null) this.images = new ArrayList<>();
        this.images.addAll(images);
        this.images = new ArrayList<>(new HashSet<>(this.images));
        return this;
    }

}
