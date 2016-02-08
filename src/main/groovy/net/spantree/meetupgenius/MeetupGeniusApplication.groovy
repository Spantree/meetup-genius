package net.spantree.meetupgenius

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = "net.spantree.meetupgenius.spring")
class MeetupGeniusApplication {
    static void main(String[] args) {
        SpringApplication.run MeetupGeniusApplication, args
    }
}
