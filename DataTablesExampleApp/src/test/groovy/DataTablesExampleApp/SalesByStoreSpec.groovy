package DataTablesExampleApp

import DataTablesExampleApp.Sakila.SalesByStore
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class SalesByStoreSpec extends Specification implements DomainUnitTest<SalesByStore> {

     void "test domain constraints"() {
        when:
        SalesByStore domain = new SalesByStore()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
