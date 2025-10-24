package DataTablesExampleApp

import DataTablesExampleApp.PhoneBookStuff.PhoneBook
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class PhoneBookSpec extends Specification implements DomainUnitTest<PhoneBook> {

     void "test domain constraints"() {
        when:
        PhoneBook domain = new PhoneBook()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
