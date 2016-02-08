package net.spantree.meetupgenius.domain

import groovy.transform.Canonical
import groovy.transform.ToString
import org.neo4j.ogm.annotation.GraphId
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Property
import org.neo4j.ogm.annotation.Relationship

@NodeEntity(label = "Member")
@ToString(includePackage = false, includeNames = true, excludes = "groups")
class MeetupMember extends MeetupEntity {
    @Property
    @MeetupProperty(name = "id")
    Long meetupId

    @Property
    @MeetupProperty(name = "name")
    String name

    @Property
    @MeetupProperty(name = "bio")
    String bio

    @Property
    @MeetupProperty(name = "city")
    String city

    @Property
    @MeetupProperty(name = "state")
    String state

    @Property
    @MeetupProperty(name = "country")
    String country

    @Relationship(type = "MEMBER_OF", direction = Relationship.OUTGOING)
    Set<MeetupGroup> groups = new HashSet<>();
}
