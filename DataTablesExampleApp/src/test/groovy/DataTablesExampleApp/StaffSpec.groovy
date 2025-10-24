package DataTablesExampleApp

import DataTablesExampleApp.Sakila.Staff
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class StaffSpec extends Specification implements DomainUnitTest<Staff> {

     void "test domain constraints"() {
        when:
        Staff domain = new Staff()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
