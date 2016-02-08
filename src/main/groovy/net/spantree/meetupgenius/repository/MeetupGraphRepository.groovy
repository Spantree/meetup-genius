package net.spantree.meetupgenius.repository

import net.spantree.meetupgenius.domain.MeetupEntity
import net.spantree.meetupgenius.domain.MeetupGroup
import org.springframework.data.neo4j.repository.GraphRepository

interface MeetupGraphRepository<T extends MeetupEntity> extends GraphRepository<T> {
    MeetupEntity findByMeetupId(Object meetupId)
}
