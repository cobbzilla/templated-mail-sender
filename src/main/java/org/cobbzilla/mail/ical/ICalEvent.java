package org.cobbzilla.mail.ical;

import net.fortuna.ical4j.model.property.Method;

/**
 * (c) Copyright 2013 Jonathan Cobb.
 * This code is available under the Apache License, version 2: http://www.apache.org/licenses/LICENSE-2.0.html
 */
public interface ICalEvent {

    public long getStartTime();
    public long getEndTime();
    public String getSummary();
    public String getUuid();
    public String getDescription();
    public String getOrganizerEmail();
    public String[] getAttendeeEmails();

    public String getProdId();
    public String getIcsName();

    public Method getMethod();

}
