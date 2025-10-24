package DataTablesExampleApp

import DataTablesExampleApp.Sakila.Actor
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class ActorSpec extends Specification implements DomainUnitTest<Actor> {

     void "test domain constraints"() {
        when:
        Actor domain = new Actor()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
