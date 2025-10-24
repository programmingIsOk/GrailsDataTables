package DataTablesExampleApp

import DataTablesExampleApp.Sakila.FilmActor
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class FilmActorSpec extends Specification implements DomainUnitTest<FilmActor> {

     void "test domain constraints"() {
        when:
        FilmActor domain = new FilmActor()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
