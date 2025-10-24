package DataTablesExampleApp

import DataTablesExampleApp.Sakila.Store
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class StoreSpec extends Specification implements DomainUnitTest<Store> {

     void "test domain constraints"() {
        when:
        Store domain = new Store()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
