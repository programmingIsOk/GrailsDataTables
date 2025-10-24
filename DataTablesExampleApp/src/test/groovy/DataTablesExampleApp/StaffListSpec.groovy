package DataTablesExampleApp

import DataTablesExampleApp.Sakila.StaffList
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class StaffListSpec extends Specification implements DomainUnitTest<StaffList> {

     void "test domain constraints"() {
        when:
        StaffList domain = new StaffList()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
