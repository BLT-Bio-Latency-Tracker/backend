package com.medilux.blt

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class BltApplication

fun main(args: Array<String>) {
    runApplication<BltApplication>(*args)
}
