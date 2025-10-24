package DataTablesExampleApp

import DataTablesExampleApp.Sakila.Country
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class CountrySpec extends Specification implements DomainUnitTest<Country> {

     void "test domain constraints"() {
        when:
        Country domain = new Country()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
