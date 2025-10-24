package DataTablesExampleApp

import DataTablesExampleApp.Sakila.Category
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class CategorySpec extends Specification implements DomainUnitTest<Category> {

     void "test domain constraints"() {
        when:
        Category domain = new Category()
        //TODO: Set domain props here

        then:
        domain.validate()
     }
}
