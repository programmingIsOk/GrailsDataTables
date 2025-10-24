package DataTablesExampleApp

import DataTablesExampleApp.Sakila.City
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class CitySpec extends Specification implements DomainUnitTest<City> {

     void "test domain constraints"() {
        when:
        City domain = new City()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
