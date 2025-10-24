package DataTablesExampleApp

import DataTablesExampleApp.Sakila.FilmCategory
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class FilmCategorySpec extends Specification implements DomainUnitTest<FilmCategory> {

     void "test domain constraints"() {
        when:
        FilmCategory domain = new FilmCategory()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
