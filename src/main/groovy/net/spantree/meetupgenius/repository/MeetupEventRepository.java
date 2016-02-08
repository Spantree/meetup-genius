package net.spantree.meetupgenius.repository;

import net.spantree.meetupgenius.domain.MeetupEvent;
import net.spantree.meetupgenius.domain.MeetupGroup;

interface MeetupEventRepository  extends MeetupGraphRepository<MeetupEvent> {
    @Override
    MeetupEvent findByMeetupId(Object meetupId);
}
