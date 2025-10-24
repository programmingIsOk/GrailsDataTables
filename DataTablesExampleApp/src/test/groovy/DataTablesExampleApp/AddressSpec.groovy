package DataTablesExampleApp

import DataTablesExampleApp.Sakila.Address
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class AddressSpec extends Specification implements DomainUnitTest<Address> {

     void "test domain constraints"() {
        when:
        Address domain = new Address()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
