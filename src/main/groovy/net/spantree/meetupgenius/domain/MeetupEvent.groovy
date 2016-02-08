package net.spantree.meetupgenius.domain

import groovy.transform.ToString
import org.neo4j.ogm.annotation.GraphId
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Property
import org.neo4j.ogm.annotation.Relationship

@NodeEntity(label = "Event")
@ToString(includePackage = false, includeNames = true, excludes = "group")
class MeetupEvent extends MeetupEntity {
    @Property
    @MeetupProperty(name = "id")
    String meetupId

    @Property
    @MeetupProperty(name = "name")
    String name

    @Property
    @MeetupProperty(name = "description")
    String description

    @Property
    @MeetupProperty(name = "time")
    Long time

    @Property
    @MeetupProperty(name = "utcOffset")
    Long utcOffset

    @Property
    @MeetupProperty(name = "yes_rsvp_count")
    Long yesRsvpCount

    @Property
    @MeetupProperty(name = "waitlist_count")
    Long waitlistCount

    @Property
    @MeetupProperty(name = "rsvp_limit")
    Long rsvpLimit

    @Property
    @MeetupProperty(name = "duration")
    Long duration

    @Relationship(type = "FOR_GROUP", direction = Relationship.OUTGOING)
    MeetupGroup group
}
