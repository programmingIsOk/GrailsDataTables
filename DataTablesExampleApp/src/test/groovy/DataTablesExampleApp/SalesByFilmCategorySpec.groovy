package DataTablesExampleApp

import DataTablesExampleApp.Sakila.SalesByFilmCategory
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class SalesByFilmCategorySpec extends Specification implements DomainUnitTest<SalesByFilmCategory> {

     void "test domain constraints"() {
        when:
        SalesByFilmCategory domain = new SalesByFilmCategory()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
