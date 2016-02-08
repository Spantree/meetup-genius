package net.spantree.meetupgenius.spring

import org.neo4j.ogm.session.Session
import org.neo4j.ogm.session.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.core.env.Environment
import org.springframework.data.neo4j.config.Neo4jConfiguration
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories
import org.springframework.data.neo4j.server.Neo4jServer
import org.springframework.data.neo4j.server.RemoteServer
import org.springframework.data.neo4j.template.Neo4jTemplate
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableNeo4jRepositories(basePackages = "net.spantree.meetupgenius.repository")
@EnableTransactionManagement
@PropertySource(["classpath:application.properties"])
class GraphConfig extends Neo4jConfiguration {
    @Autowired
    Environment env

    @Bean
    public Neo4jServer neo4jServer() {
        def server = new RemoteServer(
            env.getProperty("neo4j.url"),
            env.getProperty("neo4j.username"),
            env.getProperty("neo4j.password")
        )
    }



    @Bean
    public SessionFactory getSessionFactory() {
        // with domain entity base package(s)
        return new SessionFactory("net.spantree.meetupgenius.domain");
    }

//    // needed for session in view in web-applications
//    @Bean
//    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
//    public Session getSession() throws Exception {
//        return super.getSession();
//    }
}
