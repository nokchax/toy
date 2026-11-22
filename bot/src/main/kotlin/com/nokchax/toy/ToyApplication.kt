package com.nokchax.toy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ToyApplication

fun main(args: Array<String>) {
    runApplication<ToyApplication>(*args)
}
