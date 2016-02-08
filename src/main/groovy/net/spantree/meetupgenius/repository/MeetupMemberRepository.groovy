package net.spantree.meetupgenius.repository

import net.spantree.meetupgenius.domain.MeetupGroup
import net.spantree.meetupgenius.domain.MeetupMember

interface MeetupMemberRepository extends MeetupGraphRepository<MeetupMember> {
    @Override
    MeetupMember findByMeetupId(Object meetupId)
}
