package DataTablesExampleApp

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class PhoneBookServiceSpec extends Specification implements ServiceUnitTest<PhoneBookService> {

     void "test something"() {
        expect:
        service.doSomething()
     }
}
