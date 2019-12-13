package org.cobbzilla.mail;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EmailIncludeType {

    file, base64_embed, url_embed, url_link;

    @JsonCreator public static EmailIncludeType fromString (String val) { return valueOf(val.toLowerCase()); }

}
