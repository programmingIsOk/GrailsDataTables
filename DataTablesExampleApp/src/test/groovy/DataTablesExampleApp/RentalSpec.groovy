package DataTablesExampleApp

import DataTablesExampleApp.Sakila.Rental
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class RentalSpec extends Specification implements DomainUnitTest<Rental> {

     void "test domain constraints"() {
        when:
        Rental domain = new Rental()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
