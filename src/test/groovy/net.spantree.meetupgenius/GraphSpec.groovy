package net.spantree.meetupgenius

import groovy.util.logging.Slf4j
import net.spantree.meetupgenius.domain.MeetupGroup
import net.spantree.meetupgenius.repository.MeetupGroupRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import net.spantree.meetupgenius.spring.*
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

@Slf4j
@ContextConfiguration(classes = [GraphConfig])
class GraphSpec extends Specification {
    @Autowired
    MeetupGroupRepository groupRepository

    @Transactional
    def "should connect to neo4j"() {
        when:
        def group = new MeetupGroup(
            groupId: 6705342,
            name: "Chicago Java Users Group",
            urlName: "ChicagoJUG"
        )
        groupRepository.save(group)

        then:
        groupRepository.findByUrlName(group.urlName) == group
    }
}
