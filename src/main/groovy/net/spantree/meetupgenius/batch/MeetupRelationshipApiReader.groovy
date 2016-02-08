package net.spantree.meetupgenius.batch

import groovyx.net.http.RESTClient
import net.spantree.meetupgenius.domain.MeetupEntity
import net.spantree.meetupgenius.repository.MeetupGraphRepository
import net.spantree.meetupgenius.util.MeetupApiRequestThrottler
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemStream
import org.springframework.batch.item.ItemStreamException
import org.springframework.batch.item.NonTransientResourceException
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.UnexpectedInputException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.neo4j.template.Neo4jOperations
import org.springframework.data.neo4j.template.Neo4jTemplate

import java.util.concurrent.atomic.AtomicLong

class MeetupRelationshipApiReader implements ItemReader<Map<String, Object>>, ItemStream {
    @Autowired
    RESTClient apiClient

    @Autowired
    AtomicLong requestCount

    String apiKey
    Integer pageSize = 100

    MeetupGraphRepository drivingRepository
    Neo4jOperations neo4jTemplate
    String drivingIdParamField
    String drivingEntityRelationshipProperty
    Map additionalParams = [:]

    String path
    String drivingQuery

    MeetupApiReader resultReader = null
    Iterator<MeetupEntity> drivingEntityIterator
    private MeetupEntity currentDrivingEntity

    @Override
    void open(ExecutionContext executionContext) throws ItemStreamException {
        drivingEntityIterator = drivingRepository.findAll().iterator()
    }

    @Override
    void update(ExecutionContext executionContext) throws ItemStreamException {
    }

    @Override
    void close() throws ItemStreamException {
    }

    @Override
    Map<String, Object> read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if(drivingEntityIterator.hasNext()) {
            while(resultReader == null || resultReader.totalCount == 0 || !resultReader.hasNext()) {
                getNextReader()
            }
        }
        if(resultReader.iterator != null && resultReader.hasNext()) {
            def result = resultReader.read()
            def rels = [:]
            rels[drivingEntityRelationshipProperty] = currentDrivingEntity
            result[MeetupEntityWriter.RELATIONSHIP_MAP_KEY] = rels
            return result
        } else if (!drivingEntityIterator.hasNext()) {
            null
        }
    }

    void getNextReader() {
        if(drivingEntityIterator.hasNext()) {
            currentDrivingEntity = drivingEntityIterator.next()
            def params = new HashMap<String, Object>(additionalParams)
            params[drivingIdParamField] = currentDrivingEntity.meetupId
            resultReader = new MeetupApiReader(
                path: path,
                apiClient: apiClient,
                apiKey: apiKey,
                pageSize: pageSize,
                requestCount: requestCount,
                params: params
            )
            resultReader.open(null)
            resultReader.loadNextPage()
        }
    }
}
