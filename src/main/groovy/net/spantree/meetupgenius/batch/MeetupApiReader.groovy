package net.spantree.meetupgenius.batch

import groovy.util.logging.Slf4j
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import net.spantree.meetupgenius.util.MeetupApiRequestThrottler
import org.apache.commons.lang.math.NumberUtils
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemStream
import org.springframework.batch.item.ItemStreamException
import org.springframework.batch.item.NonTransientResourceException
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.UnexpectedInputException
import org.springframework.beans.factory.annotation.Autowired

import java.util.concurrent.atomic.AtomicLong

@Slf4j
class MeetupApiReader implements ItemReader<Map<String, Object>>, ItemStream {
    @Autowired
    RESTClient apiClient

    @Autowired
    AtomicLong requestCount

    String apiKey
    String path
    Integer pageSize = 100

    Map<String, Object> params
    Integer offset
    boolean lastPage = false

    Integer totalCount = null
    Integer i

    Iterator<Map<String, Object>> iterator

    @Override
    void open(ExecutionContext executionContext) throws ItemStreamException {
        i = 0
        iterator = null
        offset = 0
    }

    @Override
    Map<String, Object> read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if(iterator == null || i < totalCount && !iterator.hasNext()) {
            loadNextPage()
        }
        if(iterator.hasNext()) {
            i++
            return iterator.next()
        } else {
            return null
        }
    }

    boolean hasNext() {
        i < totalCount && (!lastPage || iterator.hasNext())
    }

    @Override
    void update(ExecutionContext executionContext) throws ItemStreamException {

    }

    @Override
    void close() throws ItemStreamException {

    }

    void updateTotalCountAndOffset(HttpResponseDecorator resp) {
        if(resp.data instanceof Map) {
            totalCount = resp.data?.meta?.total_count
            // if using v2 metadata/results approach, increment offset by 1
            offset++
            if(Math.floor(totalCount/pageSize) == offset) {
                lastPage = true
            }
        } else if(resp.getFirstHeader("x-total-count") != null) {
            totalCount = NumberUtils.createInteger(resp.getFirstHeader("x-total-count")?.value)
            // if using v1 result array approach, increment offset by pagesize
            offset += pageSize
            if(offset + pageSize >= totalCount) {
                lastPage = true
            }
        } else {
            log.warn "No total count available for request"
        }
    }

    Collection<Map> loadNextPage() {
        HttpResponseDecorator resp = getNextResult()
        updateTotalCountAndOffset(resp)
        def results = resp.data instanceof Map ? resp.data.results : resp.data
        def requestParams = params.clone()
        requestParams["key"] = apiKey
        requestParams["offset"] = offset
        requestParams["page"] = pageSize
        log.info "Received ${results.size()} results for path: ${path}, params: ${requestParams}"
        iterator = results.iterator()
        results
    }

    private HttpResponseDecorator getNextResult() {
        def requestParams = params.clone()
        requestParams["key"] = apiKey
        requestParams["offset"] = offset
        requestParams["page"] = pageSize
        log.info "Issuing request #${requestCount.get()} for path: ${path}, params: ${requestParams}"
        apiClient.get([path: path, query: requestParams])
    }
}
