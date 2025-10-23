package org.tupperware.GrailsDataTables

import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Specification

class GrailsDataTablesTagLibSpec extends Specification implements TagLibUnitTest<GrailsDataTablesTagLib> {

     void "test simple tag as method"() {
       expect:
       tagLib.simple()
     }
}
