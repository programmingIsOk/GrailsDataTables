package DataTablesExampleApp

import DataTablesExampleApp.Sakila.CustomerList
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class CustomerListSpec extends Specification implements DomainUnitTest<CustomerList> {

     void "test domain constraints"() {
        when:
        CustomerList domain = new CustomerList()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
