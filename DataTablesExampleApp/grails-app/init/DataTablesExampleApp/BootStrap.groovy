package DataTablesExampleApp

import DataTablesExampleApp.PhoneBookStuff.PhoneBook
import org.apache.commons.lang.RandomStringUtils

class BootStrap {

    PhoneBookService phoneBookService

    static char numToLetterBySubstr(int i) {
        String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        if (i > 0 && i <= 25) {
            return LETTERS.substring(i, i + 1).charAt(0);
        } else {
            return '?' as char;
        }
    }

    def init = { servletContext ->

        try {

            for (int i = 0; i < 25; i++) {
                String personsName = "Test Name ${numToLetterBySubstr(i + 1)}"
                Integer personsNumber = i + 100
                Date personsDate = new Date()
                String largeText = RandomStringUtils.random(100, "ABCD")
                PhoneBook persistedBook = phoneBookService.createPhoneBook([personsName: personsName, phoneNumber: personsNumber, phoneNumberDate: personsDate, largeText: largeText] as Map)
                if (persistedBook.hasErrors())
                    throw new RuntimeException("Failed to create test data")
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception during boot of example application", e)
        }

    }

    def destroy = {
    }
}