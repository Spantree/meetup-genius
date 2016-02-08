package net.spantree.meetupgenius

import groovyx.net.http.RESTClient
import net.spantree.meetupgenius.batch.MeetupApiReader
import net.spantree.meetupgenius.batch.MeetupEntityWriter
import net.spantree.meetupgenius.spring.ApiConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(classes = [ApiConfig])
class MeetupApiSandboxSpec extends Specification {
    @Autowired
    RESTClient apiClient

    @Autowired
    Environment env

    MeetupApiReader getReader(String path, Map params) {
        new MeetupApiReader(
            apiClient: apiClient,
            apiKey: env.getProperty("meetup.api.key"),
            path: path,
            params: params
        )
    }

    def "should extract total count"() {
        when:
        def reader = getReader(path, params)
        reader.open(null)
        def nextPage = reader.loadNextPage()

        then:
        nextPage.size() > 0
        reader.totalCount >= expectedCount

        where:
        path           | params                             | expectedCount
        "/2/groups"    | [category_id: 34]                  | 28386
        "/find/groups" | [text: "docker", radius: "global"] | 574
    }

    def "should read all results"() {
        when:
        def reader = getReader(path, params)
        reader.open(null)
        def results = []
        Map r = null
        while(r = reader.read()) {
            results << r
        }

        then:
        results.size() == reader.totalCount

        where:
        path           | params
        "/2/groups"    | [category_id: 34, city: "Chicago", state: "IL", country: "US"]
        "/find/groups" | [text: "docker", radius: "global"]
    }
}
