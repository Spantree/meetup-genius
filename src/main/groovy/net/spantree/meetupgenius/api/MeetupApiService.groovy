package net.spantree.meetupgenius.api

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.RESTClient
import net.spantree.meetupgenius.util.MeetupApiRequestThrottler

class MeetupApiService {
    HTTPBuilder api
    final String apiKey

    MeetupApiService(final String baseUrl, final String apiKey) {
        api = new RESTClient(baseUrl)
        this.apiKey = apiKey
        def interceptor = new MeetupApiRequestThrottler()
        api.client.addRequestInterceptor(interceptor)
        api.client.addResponseInterceptor(interceptor)
    }

    Map getGroupByUrl(String groupUrl) {
        def resp = api.get([
            path: "groups",
            query: [
                key: apiKey,
                group_urlname: groupUrl
            ]
        ])
        resp.responseData?.results?.first()
    }
}
