package DataTablesExampleApp

import DataTablesExampleApp.Sakila.Inventory
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class InventorySpec extends Specification implements DomainUnitTest<Inventory> {

     void "test domain constraints"() {
        when:
        Inventory domain = new Inventory()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
