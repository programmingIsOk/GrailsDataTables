package DataTablesExampleApp

import DataTablesExampleApp.Sakila.ActorInfo
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class ActorInfoSpec extends Specification implements DomainUnitTest<ActorInfo> {

     void "test domain constraints"() {
        when:
        ActorInfo domain = new ActorInfo()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
