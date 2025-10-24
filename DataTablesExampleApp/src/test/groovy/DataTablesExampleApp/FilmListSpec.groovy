package DataTablesExampleApp

import DataTablesExampleApp.Sakila.FilmList
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class FilmListSpec extends Specification implements DomainUnitTest<FilmList> {

     void "test domain constraints"() {
        when:
        FilmList domain = new FilmList()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
