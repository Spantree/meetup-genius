package net.spantree.meetupgenius.domain

import org.neo4j.ogm.annotation.GraphId

abstract class MeetupEntity {
    @GraphId
    Long nodeId

    Object meetupId
}
