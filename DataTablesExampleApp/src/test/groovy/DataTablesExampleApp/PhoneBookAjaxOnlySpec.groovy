package DataTablesExampleApp

import DataTablesExampleApp.PhoneBookStuff.PhoneBookAjaxOnly
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class PhoneBookAjaxOnlySpec extends Specification implements DomainUnitTest<PhoneBookAjaxOnly> {

     void "test domain constraints"() {
        when:
        PhoneBookAjaxOnly domain = new PhoneBookAjaxOnly()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
