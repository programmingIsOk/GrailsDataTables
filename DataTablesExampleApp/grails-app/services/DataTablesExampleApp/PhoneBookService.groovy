package DataTablesExampleApp

import DataTablesExampleApp.PhoneBookStuff.PhoneBook
import grails.gorm.transactions.Transactional
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.transform.CompileStatic
import org.hibernate.SessionFactory
import org.tupperware.GrailsDataTables.GrailsDataTablesService

@Transactional("PhoneBook")
@CompileStatic
class PhoneBookService {

    GrailsDataTablesService grailsDataTablesService
    def sessionFactory_PhoneBook

    HashMap<String, Object> phoneBooks_serviceAjaxCall(GrailsParameterMap params) {
        return grailsDataTablesService.serviceDataTableAjaxCall(PhoneBook.class, params as GrailsParameterMap, sessionFactory_PhoneBook as SessionFactory)
    }

    PhoneBook getByPhoneBookNumber(Integer number) {
        PhoneBook book = PhoneBook.findWhere([phoneNumber: number])
        return book
    }

    boolean deletePhoneBook(PhoneBook toDelete) {
        try {
            toDelete = PhoneBook.find(toDelete)
            toDelete.delete(flush: true, failOnError: true)
            return true
        } catch (Exception ignore) {
            return false
        }

    }

    PhoneBook createPhoneBook(Map properties) {
        PhoneBook newBook = new PhoneBook(properties)
        newBook.save(flush: true)
        return newBook
    }

}