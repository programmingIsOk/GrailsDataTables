package org.tupperware.GrailsDataTables.Controllers

import grails.web.mapping.LinkGenerator
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.grails.web.servlet.mvc.TokenResponseHandler
import org.springframework.beans.BeansException

import org.tupperware.GrailsDataTables.DataTables.tableclass.DTColumnRender
import org.tupperware.GrailsDataTables.GrailsDataTablesService

import java.lang.reflect.Field

import org.tupperware.GrailsDataTables.DataTables.tableclass.DataTable
import org.tupperware.GrailsDataTables.DataTables.tableclass.DTRenderTypes

/**
 * This class is more of an experiment on how to streamline integrating the plugin without writing a bunch of boiler code
 */
abstract class DataTableContext implements DataTableControllerContextImp {

	private GrailsDataTablesService getGrailsDataTablesService() {
		try {
			grailsApplication.getMainContext().getBean(GrailsDataTablesService.class) as GrailsDataTablesService
		} catch (BeansException ignore) {return null}
	}
	LinkGenerator getGrailsLinkGenerator() {
		try {
			grailsApplication.getMainContext().getBean("grailsLinkGenerator", LinkGenerator) as LinkGenerator
		} catch (BeansException ignore) {return null}
	}

	private void doResetOfToken(Class tableClass) {
		SynchronizerTokensHolder holder = SynchronizerTokensHolder.store(session)
		DataTable tableInfo = tableClass.getAnnotation(DataTable.class) as DataTable
		assert tableInfo != null
		Field[] fields = tableClass.getDeclaredFields()
		for (Field field : fields) {
			DTColumnRender renderInfo = field.getAnnotation(DTColumnRender.class)
			if (renderInfo?.renderAs() == DTRenderTypes.FORM_ACTION) {
				String _actionName = renderInfo.actionName()
				String _controllerName = renderInfo.controllerName()

				if (actionName == _actionName && controllerName == _controllerName) {
					String renderedUrlLocation = grailsDataTablesService.getSyncUrl(tableClass.name, field.name)
					if (Objects.nonNull(renderedUrlLocation))
						holder.resetToken(renderedUrlLocation)
				}

			}
		}
	}

	TokenResponseHandler withTableForm(Class tableClass, Closure validClosure) {

		TokenResponseHandler withFormResult = withForm(validClosure)
		if (!withFormResult.wasInvalidToken())
			doResetOfToken(tableClass)
		return withFormResult

	}

}