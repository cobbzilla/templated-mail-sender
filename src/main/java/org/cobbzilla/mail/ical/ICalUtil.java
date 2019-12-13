package org.cobbzilla.mail.ical;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.URI;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

/**
 * (c) Copyright 2013 Jonathan Cobb.
 * This code is available under the Apache License, version 2: http://www.apache.org/licenses/LICENSE-2.0.html
 */
public class ICalUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ICalUtil.class);

    public static final String CONTENT_TYPE = "text/calendar";

    public static Calendar newCalendarEvent(String calendarProdId, ICalEvent event) {

        final Calendar iCalendar = new Calendar();
        final PropertyList calProps = iCalendar.getProperties();

        calProps.add(new ProdId(calendarProdId));
        calProps.add(Version.VERSION_2_0);
        calProps.add(CalScale.GREGORIAN);
        calProps.add(event.getMethod());

        final VEvent vEvent = ICalUtil.createVEvent(event);

        iCalendar.getComponents().add(vEvent);

        return iCalendar;
    }

    public static VEvent createVEvent(ICalEvent event) {
        Date start = new DateTime(event.getStartTime());
        Date end = new DateTime(event.getEndTime());
        VEvent vEvent = new VEvent(start, end, event.getSummary());
        Uid uid = new Uid(event.getUuid());
        vEvent.getProperties().add(uid);
        vEvent.getProperties().add(new Description(event.getDescription()));

        final URI organizerEmailUri = URI.create("mailto:" + event.getOrganizerEmail());

        Organizer organizer = new Organizer(organizerEmailUri);
        vEvent.getProperties().add(organizer);

        for (String attendeeEmail : event.getAttendeeEmails()) {
            final Attendee attendee = new Attendee(URI.create("mailto:"+attendeeEmail));
            attendee.getParameters().add(Role.REQ_PARTICIPANT);
            vEvent.getProperties().add(attendee);
        }

        return vEvent;
    }

    public static byte[] toBytes(final Calendar iCalendar) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            new CalendarOutputter().output(iCalendar, output);
            return output.toByteArray();
        } catch (Exception e) {
            return die("can't convert calendar to bytes: "+e);
        }
    }
}
