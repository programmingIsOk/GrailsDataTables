package org.tupperware.GrailsDataTables.Controllers

import grails.artefact.Controller
import org.grails.web.servlet.mvc.TokenResponseHandler

abstract class DataTableControllerContext implements Controller {

	abstract TokenResponseHandler withTableForm(Closure callable)

}