package org.tupperware.GrailsDataTables

import grails.converters.JSON
import grails.util.Environment
import groovy.transform.CompileStatic

/**
 * This is the endpoint that handles Ajax requests from
 * rendered DataTables via the GDTables tags.
 *
 * @author Tupper-Jacob
 * @since 0.1
 */
@CompileStatic
class GrailsDataTablesController {

    GrailsDataTablesService grailsDataTablesService

    static defaultAction = "datatable"

    def datatable() {
        try {
            render grailsDataTablesService.serviceDataTableAjaxCall(params) as JSON
        } catch (Exception e) {
            log.error("Uncaught Exception servicing DataTable Ajax Call", e)
            def errorJson = [:]
            if (!Objects.equals(Environment.current, Environment.DEVELOPMENT)) {
                errorJson.put("error", "\n\nAn error has occurred.")
            } else {
                errorJson.put("error", "\n\nUncaught Exception At Root of Controller: \n\tMessage: '${e.getMessage()}'")
            }
            render errorJson as JSON
        }
    }

}