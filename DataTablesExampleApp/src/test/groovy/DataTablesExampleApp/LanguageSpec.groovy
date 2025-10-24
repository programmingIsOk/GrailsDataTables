package DataTablesExampleApp

import DataTablesExampleApp.Sakila.Language
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class LanguageSpec extends Specification implements DomainUnitTest<Language> {

     void "test domain constraints"() {
        when:
        Language domain = new Language()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
