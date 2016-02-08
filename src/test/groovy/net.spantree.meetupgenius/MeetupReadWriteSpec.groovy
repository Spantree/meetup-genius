package net.spantree.meetupgenius

import com.google.common.collect.Lists
import groovyx.net.http.RESTClient
import net.spantree.meetupgenius.batch.MeetupApiReader
import net.spantree.meetupgenius.batch.MeetupEntityWriter
import net.spantree.meetupgenius.batch.MeetupRelationshipApiReader
import net.spantree.meetupgenius.domain.MeetupEntity
import net.spantree.meetupgenius.domain.MeetupEvent
import net.spantree.meetupgenius.domain.MeetupGroup
import net.spantree.meetupgenius.domain.MeetupMember
import net.spantree.meetupgenius.repository.MeetupEventRepository
import net.spantree.meetupgenius.repository.MeetupGraphRepository
import net.spantree.meetupgenius.repository.MeetupGroupRepository
import net.spantree.meetupgenius.repository.MeetupMemberRepository
import net.spantree.meetupgenius.spring.ApiConfig
import net.spantree.meetupgenius.spring.GraphConfig
import net.spantree.meetupgenius.util.MeetupApiRequestThrottler
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.data.neo4j.repository.GraphRepository
import org.springframework.data.neo4j.template.Neo4jOperations
import org.springframework.data.neo4j.template.Neo4jTemplate
import org.springframework.test.context.ContextConfiguration
import spock.lang.IgnoreRest
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicLong

@ContextConfiguration(classes = [ApiConfig, GraphConfig])
class MeetupReadWriteSpec extends Specification {
    @Autowired
    MeetupGroupRepository groupRepository

    @Autowired
    MeetupMemberRepository memberRepository

    @Autowired
    MeetupEventRepository eventRepository

    @Autowired
    Neo4jOperations neo4jTemplate

    @Autowired
    RESTClient apiClient

    @Autowired
    AtomicLong requestCount

    @Autowired
    Environment env

    Integer chunkSize = 100

    def setupIndexes() {
        def commands = [
            "CREATE CONSTRAINT ON (g:Group) ASSERT g.meetupId IS UNIQUE",
            "CREATE INDEX ON :Group(name)",
            "CREATE CONSTRAINT ON (m:Member) ASSERT m.meetupId IS UNIQUE",
            "CREATE INDEX ON :Member(name)",
            "CREATE CONSTRAINT ON (e:Event) ASSERT e.meetupId IS UNIQUE"
        ]
        commands.each { command ->
            neo4jTemplate.execute(command)
        }
    }

    def setup() {
        setupIndexes()
    }

    MeetupApiReader getGroupReader(Map params) {
        new MeetupApiReader(
            apiClient: apiClient,
            apiKey: env.getProperty("meetup.api.key"),
            path: "/2/groups",
            requestCount: requestCount,
            params: params
        )
    }

    MeetupEntityWriter getEntityWriter(Class<? extends MeetupEntity> entityClass) {
        new MeetupEntityWriter(
            entityClass: entityClass,
            neo4jTemplate: neo4jTemplate
        )
    }

    MeetupRelationshipApiReader getRelationshipReader(
        String path,
        MeetupGraphRepository repository,
        String drivingIdParamField,
        String drivingEntityProperty,
        Map additionalParams = [:],
        boolean open = true
    ) {
        def reader = new MeetupRelationshipApiReader(
            apiClient: apiClient,
            apiKey: env.getProperty("meetup.api.key"),
            path: path,
            drivingRepository: repository,
            drivingIdParamField: drivingIdParamField,
            drivingEntityRelationshipProperty: drivingEntityProperty,
            requestCount: requestCount,
            additionalParams: additionalParams,
            pageSize: 100
        )
        if(open) {
            reader.open(null)
        }
        reader
    }

    void writeInChunks(List<Map> items, ItemWriter writer) {
        Lists.partition(items, chunkSize).each { chunk ->
            writer.write(chunk)
        }
    }

    void writeInChunks(ItemReader reader, ItemWriter writer) {
        def results = []
        def result
        while(result = reader.read()) {
            results << result
            if(results.size() == chunkSize) {
                writer.write(results)
                results = []
            }
        }
        if(results.size() > 0) {
            writer.write(results)
        }
    }

    private List<MeetupGroup> getGroupsByUrlName(List<String> groupUrlNames) {
        List<MeetupGroup> groups = []
        for (String groupUrlName : groupUrlNames) {
            def groupReader = getGroupReader([group_urlname: groupUrlName])
            groupReader.open(null)
            groups.add(groupReader.read())
        }
        return groups
    }

    List<Map> getAllReaderResults(ItemReader reader) {
        List<Map> results = []
        Map result
        while(result = reader.read()) {
            results << result
        }
        results
    }

    def "should write groups to graph"() {
        when:
        def reader = getGroupReader(params)
        reader.open(null)
        def writer = getEntityWriter(MeetupGroup.class, groupRepository)

        then:
        Map r
        while(r = reader.read()) {
            writer.write([r])
        }

        where:
        params << [category_id: 34, city: "Chicago", state: "IL", country: "US"]
    }

//    @IgnoreRest
    // Meetup returns 99 results instead of 10 on the 9th page of events
    // for this meetup group for some reason
    def "should handle wonky pagination"() {
        setup:
        def groupWriter = getEntityWriter(MeetupGroup)
        def eventWriter = getEntityWriter(MeetupEvent)
        def groupResults = getGroupsByUrlName(["GetVolunteering"])
        def groupMeetupId = groupResults.first().id
        groupWriter.write(groupResults)
        neo4jTemplate.query(
            "MATCH (g:Group { meetupId: {meetupId} })-[r]-(e:Event) DELETE e, r",
            [meetupId: groupMeetupId]
        )

        when:
        def groupEventReader = getRelationshipReader("/2/events", groupRepository, "group_id", "group", [status: 'past'], [:], false)
        groupEventReader.drivingEntityIterator = [groupRepository.findByMeetupId(groupMeetupId)].iterator()

        then:
        writeInChunks(groupEventReader, eventWriter)
    }

    @IgnoreRest
    def "should write groups and read members"() {
        setup:
        def groupWriter = getEntityWriter(MeetupGroup)
        def memberWriter = getEntityWriter(MeetupMember)
        def eventWriter = getEntityWriter(MeetupEvent)

        when:
        def groupResults = getGroupsByUrlName(groupUrlNames)
        def firstGroupMeetupId = groupResults.first().id

        then:
        groupWriter.write(groupResults)

        when:
        def groupMemberReader = getRelationshipReader("/2/members", groupRepository, "group_id", "groups", [:], false)
        def groups = groupResults.collect { result -> groupRepository.findByMeetupId(result.id) }
        groupMemberReader.drivingEntityIterator = groups.iterator()

//        and:
////        def groupMemberResults = getAllReaderResults(groupMemberReader)
////        def size = groupMemberResults.size()
////        size == groups*.memberCount.sum()
//
//        when:
        writeInChunks(groupMemberReader, memberWriter)
//
        and:
        def memberGroupReader = getRelationshipReader("/2/groups", memberRepository, "member_id", "members")
//
        then:
        writeInChunks(memberGroupReader, groupWriter)
//
//        when:
//        def groupsWithCommonMembers = groupRepository.findGroupsWithCommonMembers(firstGroupMeetupId, 5)
//
//        def pastGroupEventReader = getRelationshipReader("/2/events", groupRepository, "group_id", "group", [status: 'past'], false)
//        pastGroupEventReader.drivingEntityIterator = groupsWithCommonMembers.iterator()
//
//        def upcomingGroupEventReader = getRelationshipReader("/2/events", groupRepository, "group_id", "group", [status: 'upcoming'], false)
//        upcomingGroupEventReader.drivingEntityIterator = groupsWithCommonMembers.iterator()
//
//        then:
//        writeInChunks(pastGroupEventReader, eventWriter)
//        writeInChunks(upcomingGroupEventReader, eventWriter)

        where:
        groupUrlNames << [["Docker-meetups"]]
    }


}
