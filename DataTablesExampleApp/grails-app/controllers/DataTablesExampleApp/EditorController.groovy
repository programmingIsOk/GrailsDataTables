package DataTablesExampleApp

import DataTablesExampleApp.PhoneBookStuff.PhoneBook
import datatables.PhoneBookDisplay
import grails.converters.JSON
import org.tupperware.GrailsDataTables.Controllers.DataTableContext

/**
 * The controller used to test the WebUI tags
 */
class EditorController extends DataTableContext {

    PhoneBookService phoneBookService

    static allowedMethods = [index: "GET", show: "GET", delete: "POST"]

    def index() {}

    def show(Integer persons_phone_number) {
        PhoneBook entry = phoneBookService.getByPhoneBookNumber(persons_phone_number)
        PhoneBookDisplay entryHtml = new PhoneBookDisplay()
        entryHtml.personsName = entry.personsName
        entryHtml.phoneNumber = entry.phoneNumber
        ArrayList<PhoneBookDisplay> phoneBookList = new ArrayList<>()
        phoneBookList.add(entryHtml)
        [phoneBook: entry, phoneBookList: phoneBookList]
    }

    def delete(Integer persons_phone_number) {

        withTableForm(PhoneBookDisplay, {
            PhoneBook entry = phoneBookService.getByPhoneBookNumber(persons_phone_number)
            Boolean deleted = phoneBookService.deletePhoneBook(entry)
            flash.message = deleted ? "Entry Deleted" : "Failed to Delete Entry"
            redirect(action: "index")
        }).invalidToken {
            flash.message = "Invalid Submission"
            redirect(action: "index")
        }

    }

    def ajax_phonebook() {
        def data = phoneBookService.phoneBooks_serviceAjaxCall(params)
        render data as JSON
    }

}