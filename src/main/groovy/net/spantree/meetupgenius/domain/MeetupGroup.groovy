package net.spantree.meetupgenius.domain

import groovy.transform.Canonical
import groovy.transform.ToString
import org.neo4j.ogm.annotation.GraphId
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Property
import org.neo4j.ogm.annotation.Relationship

@NodeEntity(label = "Group")
@ToString(includePackage = false, includeNames = true, excludes = ["events", "members"])
class MeetupGroup extends MeetupEntity {
    @Property
    @MeetupProperty(name = "id")
    Long meetupId

    @Property
    @MeetupProperty(name = "name")
    String name

    @Property
    @MeetupProperty(name = "urlname")
    String urlName

    @Property
    @MeetupProperty(name = "members")
    Integer memberCount

    @Relationship(type = "FOR_GROUP", direction = Relationship.INCOMING)
    Set<MeetupEvent> events;

    @Relationship(type = "MEMBER_OF", direction = Relationship.INCOMING)
    Set<MeetupMember> members = new HashSet<>();
}
