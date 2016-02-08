package net.spantree.meetupgenius.spring

import groovyx.net.http.RESTClient
import net.spantree.meetupgenius.api.MeetupApiService
import net.spantree.meetupgenius.util.MeetupApiRequestThrottler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment

import java.util.concurrent.atomic.AtomicLong

@Configuration
@PropertySource(["classpath:application.properties"])
class ApiConfig {
    @Autowired
    Environment env

    @Bean
    AtomicLong httpRequestCount() {
        def count = new AtomicLong()
        count.incrementAndGet()
        count
    }

    @Bean
    MeetupApiRequestThrottler meetupApiRequestThrottler() {
        new MeetupApiRequestThrottler(
            requestCount: httpRequestCount()
        )
    }

    @Bean
    RESTClient meetingApiClient() {
        def baseUrl = env.getProperty("meetup.api.baseUrl")
        def apiClient = new RESTClient(baseUrl)
        apiClient.client.addRequestInterceptor(meetupApiRequestThrottler())
        apiClient.client.addResponseInterceptor(meetupApiRequestThrottler())
        apiClient
    }

    @Bean
    MeetupApiService meetupApiService() {
        new MeetupApiService(
            env.getProperty("meetup.api.baseUrl"),
            env.getProperty("meetup.api.key")
        )
    }
}
