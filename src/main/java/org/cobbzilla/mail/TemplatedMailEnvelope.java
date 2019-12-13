package org.cobbzilla.mail;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cobbzilla.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * (c) Copyright 2013 Jonathan Cobb.
 * This code is available under the Apache License, version 2: http://www.apache.org/licenses/LICENSE-2.0.html
 */
public class TemplatedMailEnvelope {

    private static final Logger LOG = LoggerFactory.getLogger(TemplatedMailEnvelope.class);

    public static final String TMAIL_EVENT_TYPE = "queue_tmail";

    protected static final ObjectMapper jsonWriter = JsonUtil.NOTNULL_MAPPER;

    public String toJson () throws IOException { return jsonWriter.writeValueAsString(this); }
    public static TemplatedMailEnvelope fromJson (String json) throws Exception { return JsonUtil.fromJson(json, TemplatedMailEnvelope.class); }

    @JsonIgnore
    public boolean isValid() { return event != null && event.equals(TMAIL_EVENT_TYPE); }

    @JsonProperty
    private String event;
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    @JsonProperty
    private TemplatedMail message;
    public TemplatedMail getMessage() { return message; }
    public void setMessage(TemplatedMail message) { this.message = message; }

    @JsonProperty // ignore/generify for now.
    private JsonNode meta;
    public JsonNode getMeta() { return meta; }
    public void setMeta(JsonNode meta) { this.meta = meta; }

}
