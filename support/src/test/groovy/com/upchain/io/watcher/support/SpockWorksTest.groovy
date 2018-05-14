package com.upchain.io.watcher.support

import spock.lang.Specification

class SpockWorksTest extends Specification {

    def spockIsWorking() {
        when:

        println "hello spock"

        then:

        1 == 1
    }
}
