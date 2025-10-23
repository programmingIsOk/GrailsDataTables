package org.tupperware.GrailsDataTables

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class GrailsDataTablesServiceSpec extends Specification implements ServiceUnitTest<GrailsDataTablesService> {

     void "test something"() {
        expect:
        service.doSomething()
     }
}
