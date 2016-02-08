package net.spantree.meetupgenius.util

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang.math.NumberUtils
import org.apache.http.HttpException
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpResponse
import org.apache.http.HttpResponseInterceptor
import org.apache.http.protocol.HttpContext
import org.springframework.beans.factory.annotation.Autowired

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

@CompileStatic
@Slf4j
class MeetupApiRequestThrottler implements HttpRequestInterceptor, HttpResponseInterceptor {
    static final String RATE_LIMIT_REMAINING = "X-RateLimit-Remaining"
    static final String RATE_LIMIT_RESET = "X-RateLimit-Reset"

    Integer rateLimitRemaining = null
    Long rateLimitResetTime = null

    ReentrantLock lock = new ReentrantLock()

    @Autowired
    AtomicLong requestCount

    @Override
    void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        lock.lock()
        requestCount.incrementAndGet()
        // default sleep time
        Long sleepTime = 500
        if(rateLimitRemaining != null && rateLimitRemaining < 1) {
            def currentTime = System.currentTimeMillis()
            if(rateLimitResetTime != null && currentTime < rateLimitResetTime) {
                sleepTime = rateLimitResetTime - currentTime
                log.info "Rate limit reached, sleeping ${sleepTime}ms."
            }
        }
        Thread.sleep(sleepTime)
        lock.unlock()
    }

    @Override
    void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        lock.lock()

        rateLimitRemaining = NumberUtils.createInteger(response.getFirstHeader(RATE_LIMIT_REMAINING)?.value)
        def rateLimitResetSeconds = NumberUtils.createInteger(response.getFirstHeader(RATE_LIMIT_RESET)?.value) ?: 60
        rateLimitResetTime = System.currentTimeMillis() + rateLimitResetSeconds*1000
        lock.unlock()
    }
}
