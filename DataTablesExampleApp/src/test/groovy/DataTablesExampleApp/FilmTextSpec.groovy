package DataTablesExampleApp

import DataTablesExampleApp.Sakila.FilmText
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class FilmTextSpec extends Specification implements DomainUnitTest<FilmText> {

     void "test domain constraints"() {
        when:
        FilmText domain = new FilmText()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
