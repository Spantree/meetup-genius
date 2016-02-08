package net.spantree.meetupgenius.repository

import net.spantree.meetupgenius.domain.MeetupGroup
import org.springframework.data.neo4j.annotation.Query
import org.springframework.data.neo4j.repository.GraphRepository
import org.springframework.data.repository.query.Param

interface MeetupGroupRepository extends MeetupGraphRepository<MeetupGroup> {
    @Override
    MeetupGroup findByMeetupId(Object meetupId)

    MeetupGroup findByUrlName(String urlName)

    @Query("""
        MATCH (g1:Group { meetupId: {meetupId} })<-[r1:MEMBER_OF]-(m:Member)-[r2:MEMBER_OF]->(n:Group)
        WITH DISTINCT n, COUNT(m) AS commonMembers
        WHERE commonMembers >= {minimumCommonMembers}
        RETURN n
        ORDER BY commonMembers DESC
    """)
    Iterable<MeetupGroup> findGroupsWithCommonMembers(
        @Param("meetupId") Long meetupId,
        @Param("minimumCommonMembers") Integer minimumCommonMembers
    )
}
