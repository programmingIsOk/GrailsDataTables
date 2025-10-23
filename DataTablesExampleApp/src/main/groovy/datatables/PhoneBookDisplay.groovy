package datatables

import org.tupperware.GrailsDataTables.DataTables.tableclass.DTColumn
import org.tupperware.GrailsDataTables.DataTables.tableclass.DTColumnRender
import org.tupperware.GrailsDataTables.DataTables.tableclass.DataTable
import org.tupperware.GrailsDataTables.DataTables.tableclass.DTRenderTypes
import org.tupperware.GrailsDataTables.DataTables.tableclass.DataTableType

@DataTable(type = DataTableType.HTML_TABLE)
class PhoneBookDisplay {

    @DTColumn(name = "Persons Name", order = 0)
    String personsName

    @DTColumnRender(renderAs = DTRenderTypes.FORM_ACTION, formMethod = "POST", submitButtonText="Delete", submitButtonCssClass = "btn btn-danger", controllerName="editor", actionName = "delete", formParamName = "persons_phone_number", useToken = true, colorStyle = "yellow")
    @DTColumn(name = "Phone Number", order = 1, ajaxColumnName = "persons_phone")
    Integer phoneNumber

}