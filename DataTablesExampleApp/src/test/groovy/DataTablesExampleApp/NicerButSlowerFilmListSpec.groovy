package DataTablesExampleApp

import DataTablesExampleApp.Sakila.NicerButSlowerFilmList
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class NicerButSlowerFilmListSpec extends Specification implements DomainUnitTest<NicerButSlowerFilmList> {

     void "test domain constraints"() {
        when:
        NicerButSlowerFilmList domain = new NicerButSlowerFilmList()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
