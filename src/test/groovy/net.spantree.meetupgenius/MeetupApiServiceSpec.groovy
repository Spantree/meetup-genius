package net.spantree.meetupgenius

import net.spantree.meetupgenius.api.MeetupApiService
import net.spantree.meetupgenius.spring.ApiConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(classes = [ApiConfig])
class MeetupApiServiceSpec extends Specification {
    @Autowired
    MeetupApiService apiService

    def "should get groups"() {
        when:
        def result = apiService.getGroupByUrl(urlName)

        then:
        result.id == groupId

        where:
        urlName      | groupId
        "ChicagoJUG" | 6705342
    }
}
