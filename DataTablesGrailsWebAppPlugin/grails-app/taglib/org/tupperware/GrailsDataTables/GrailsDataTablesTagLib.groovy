package org.tupperware.GrailsDataTables

import org.tupperware.GrailsDataTables.DataTables.tableclass.DTColumn
import org.tupperware.GrailsDataTables.DataTables.tableclass.DTColumnRender
import org.tupperware.GrailsDataTables.DataTables.tableclass.DataTable
import org.tupperware.GrailsDataTables.DataTables.tableclass.DTRenderTypes
import org.tupperware.GrailsDataTables.DataTables.tableclass.DataTableType
import org.tupperware.GrailsDataTables.DataTables.tableclass.DTColumnDataType

import org.grails.taglib.GrailsTagException

import java.lang.annotation.Annotation
import java.lang.reflect.Field

/**
 * The GSP taglib for the GrailsDataTables, and is the rendering entry.
 */
class GrailsDataTablesTagLib {

    GrailsDataTablesService grailsDataTablesService

    static namespace = "GDTables"

    private static final String datatableHtmlName = "DataTableHtml"
    private static final String datatableAjaxName = "DataTableAjax"
    private static final String datatableAjaxEditableName = "DataTableAjaxEditable"

    def DataTableHtml = { attrs, body ->

        //Id and UUID
        def id = attrs?.id
        if (Objects.isNull(id))
            throw new GrailsTagException("$datatableHtmlName id null")
        if (!(id instanceof String))
            throw new GrailsTagException("$datatableHtmlName id is not an instance of String.class")
        String uuid = UUID.randomUUID().toString()
        if (Objects.isNull(uuid) || uuid?.length() < 36) {
            throw new GrailsTagException("$datatableHtmlName failed to get a usable uuid")
        }

        //Make sure the passed in object is a list that is not null
        def data = attrs?.data
        if (data == null)
            data = new ArrayList()
        //throw new GrailsTagException("$datatableHtmlName data null")
        if (!(data instanceof List))
            data = new ArrayList()
        //throw new GrailsTagException("$datatableHtmlName data is not a list")

        ArrayList<String> columnNames = new ArrayList<>()
        ArrayList<HashMap<Integer, String>> columnDatas = new ArrayList<>()
        HashMap<String, HashMap<String, Object>> columnRenderInfos = new HashMap<>()

        Class tableClass = attrs?.forClass as Class
        if (Objects.isNull(tableClass))
            throw new GrailsTagException("DataTableHtml cannot have forClass attribute null")

        DataTable tableInfo = grailsDataTablesService.getDataTableInfo(tableClass, DataTableType.HTML_TABLE)
        ArrayList<Field> tableInfoFields = grailsDataTablesService.getFieldsWithDTColumn(tableClass)

        for (Field field : tableInfoFields) {

            DTColumn columnInfo = (DTColumn) field.getAnnotation(DTColumn)
            columnNames.add(columnInfo.name())

            DTColumnRender renderInfo = field.getAnnotation(DTColumnRender.class)
            String columnFieldName = field.getAnnotation(DTColumn.class).name()
            Integer wantedIndex = field.getAnnotation(DTColumn.class).order()

            Integer curIndex = columnNames.indexOf(columnFieldName)
            if (curIndex != wantedIndex) {
                String temp = columnNames.get(wantedIndex)
                columnNames.set(curIndex, temp)
                columnNames.set(wantedIndex, columnFieldName)
            }

            //Set the map used for adding custom rendering to columns in datatable
            HashMap<String, Object> colDefInfo = new HashMap<>()
            colDefInfo.put("webUi_orderable", columnInfo.sortable())
            if (Objects.nonNull(renderInfo)) {
                //Custom renders
                if (renderInfo.renderAs() == DTRenderTypes.GLINK) {
                    String __controllerName = renderInfo.controllerName()
                    String __actionName = renderInfo.actionName()
                    Object __link = null
                    if (__controllerName?.length() >= 1 && __actionName?.length() >= 1) {
                        __link = g.createLink([controller: __controllerName, action: __actionName])
                    } else if (__actionName?.length() >= 1) {
                        __link = g.createLink([action: __actionName])
                    } else {
                        throw new GrailsTagException("Field ${field.name} of class ${tableClass?.toString()} has a render modifier of GLINK. Annotation has controller or name as defaults.")
                    }
                    if (Objects.nonNull(__link)) {
                        colDefInfo.put("webUi_renderAs", "GLINK")
                        colDefInfo.put("webUi_link", __link)
                        if (columnInfo.ajaxColumnName() == null || columnInfo.ajaxColumnName()?.length() <= 0) {
                            throw new GrailsTagException("$datatableHtmlName has field ${field.name} to be used as GLINK, but no ajaxColumnName is set! ajaxColumnName is used for the ID parameter name of HTML renders!")
                        } else {
                            colDefInfo.put("webUi_link_id_name", columnInfo.ajaxColumnName())
                        }
                    }
                } else if (renderInfo.renderAs() == DTRenderTypes.FORM_ACTION) {
                    String __controllerName = renderInfo.controllerName()
                    String __actionName = renderInfo.actionName()
                    String __submitButtonText = renderInfo.submitButtonText()
                    String __submitButtonCssClass = renderInfo.submitButtonCssClass()
                    String __formMethod = renderInfo.formMethod()
                    String __formParamName = renderInfo.formParamName()
                    Object __link = null
                    if (__controllerName?.length() >= 1 && __actionName?.length() >= 1) {
                        __link = g.createLink([controller: __controllerName, action: __actionName])
                    } else if (__actionName?.length() >= 1) {
                        __link = g.createLink([action: __actionName])
                    } else {
                        throw new GrailsTagException("Field ${field.name} of class ${theClass?.toString()} has a render modifier of FORM_ACTION. Annotation has controller or name as defaults.")
                    }

                    if (Objects.isNull(__submitButtonText) || __submitButtonText?.length() <= 0)
                        throw new GrailsTagException("Field ${field.name} of class ${tableClass?.toString()} has a render modifier of FORM_ACTION. Submit button is required to have text.")
                    if (Objects.isNull(__formMethod) || !(["POST", "GET"]?.contains(__formMethod)))
                        throw new GrailsTagException("Field ${field.name} of class ${tableClass?.toString()} has a render modifier of FORM_ACTION but no usable http method. Value that was to be used: ${__formMethod}")
                    if (Objects.isNull(columnInfo.ajaxColumnName()) || columnInfo.ajaxColumnName().length() <= 0)
                        throw new RuntimeException("Field ${field.name} of class ${tableClass?.toString()} has a render modifier of FORM_ACTION but ajaxColumnName is not defined")

                    if (Objects.nonNull(__link)) {
                        colDefInfo.put("webUi_renderAs", "FORM_ACTION")
                        colDefInfo.put("webUi_link", __link)
                        colDefInfo.put("webUi_submitButtonText", __submitButtonText)
                        colDefInfo.put("webUi_submitButtonCssClass", __submitButtonCssClass)
                        colDefInfo.put("webUi_formUuid", (uuid + UUID.randomUUID()))
                        colDefInfo.put("webUi_formMethod", __formMethod)
                        colDefInfo.put("webUi_formAction", __actionName)
                        colDefInfo.put("webUi_formController", __controllerName)

                        //The property formParamName cannot be null when base64 is enabled due to ajaxColumnName being used as the id 'name' value for the form submission.
                        if (__formParamName == null && grailsDataTablesService.isBase64ParamMapConversionEnabled)
                            throw new RuntimeException("DTColumnRender FORM_ACTION violated for Table '${tableClass.simpleName}' and field '${field}'.")

                        String fieldNameForToken = field.name
                        grailsDataTablesService.putSyncTokenRenderingLocation(actionName, controllerName, fieldNameForToken, tableClass)

                        colDefInfo.put("webUi_formToken", renderInfo.useToken())
                        colDefInfo.put("webUi_formParamName", __formParamName ?: columnInfo.ajaxColumnName())

                        if (Objects.isNull(__controllerName))
                            throw new GrailsTagException("Cannot produce table due to DTColumnRender.renderAs 'FORM'. This render type requires controllers due to forms parameters.")

                        colDefInfo.put("webUi_ajaxColumnName", columnInfo.ajaxColumnName())
                        if (columnInfo.ajaxColumnName() == null || columnInfo.ajaxColumnName()?.length() <= 0) {
                            throw new GrailsTagException("$datatableHtmlName has field ${field.name} to be used as FORM_ACTION, but no ajaxColumnName is set! ajaxColumnName is used for the hidden input name of the form!")
                        } else {
                            colDefInfo.put("webUi_link_id_name", columnInfo.ajaxColumnName())
                        }
                    }
                }
                //Common
                colDefInfo.put("webUi_color", renderInfo.colorStyle())
                colDefInfo.put("webUi_width", renderInfo.columnWidth())
                colDefInfo.put("webUi_height", renderInfo.columnHeight())
            }
            columnRenderInfos.put(columnInfo.name(), colDefInfo)


        }


        try {
            for (Object it : data) {

                if (Objects.isNull(it))
                    continue

                DataTable objInstanceTableInfo = grailsDataTablesService.getDataTableInfo(it?.class, DataTableType.HTML_TABLE)

                //Get and set data
                HashMap<Integer, String> fieldsData = new HashMap<>()
                for (Field field : tableInfoFields) {
                    Integer order = field.getAnnotation(DTColumn.class).order()
                    String fieldValue = (it."$field.name")
                    fieldsData.put(order, fieldValue)
                }

                columnDatas.add(fieldsData)

            }

            //Options - default all to true
            Boolean searchable = attrs?.searchable != "false"
            Boolean selectable = attrs?.selectable != "false"
            Boolean orderable = attrs?.orderable != "false"
            Boolean pageable = attrs?.pageable != "false"
            String scrollY = (attrs?.scrollY instanceof String && attrs?.scrollY?.length() > 0) ? attrs.scrollY : null
            Boolean scrollX = attrs?.scrollX != "false"

            ArrayList<Integer> lengthMenu = tableInfo.lengthMenu()

            Boolean scrollCollapse
            if (attrs.scrollCollapse == "true")
                scrollCollapse = true
            else if (attrs.scrollCollapse == "false")
                scrollCollapse = false
            else if (scrollY)
                scrollCollapse = true
            else scrollCollapse = false

            //Wanted exports
            ArrayList<String> wantedExports = new ArrayList<>()
            if (attrs?.wantedExports instanceof List<String>) {
                attrs?.wantedExports?.each { String key ->
                    if (key == "copy")
                        wantedExports.add("copy")
                    if (key == "excel")
                        wantedExports.add("excel")
                    if (key == "csv")
                        wantedExports.add("csv")
                    if (key == "pdf")
                        wantedExports.add("pdf")
                    if (key == "print")
                        wantedExports.add("print")
                }
            }

            out << render(
                    template: "/GrailsDataTables/templates/DT_HTML_RD",
                    model: [
                            columns: columnNames,
                            datas: columnDatas,
                            id: id,
                            uuid: uuid,
                            searchable: searchable,
                            selectable: selectable,
                            orderable: orderable,
                            pageable: pageable,
                            scrollY: scrollY,
                            scrollX: scrollX,
                            scrollCollapse: scrollCollapse,
                            columnRenderInfos: columnRenderInfos,
                            lengthMenu: lengthMenu
                    ],
                    plugin: "grailsDataTables"
            )

        } catch(NullPointerException e) {
            throw new GrailsTagException("Null pointer exception during DataTableHtml: ${e?.getMessage()}")
        }
        catch (Exception e) {
            throw new RuntimeException("Exception during DataTableHtml: ${e.getMessage()}")
        }
    }

    private static final ArrayList<String> loggedParamnames = new ArrayList<>()

    private String getAjaxEndpointUri(Class thisClass, DataTable tableInfo) {
        String __resourceName = grailsDataTablesService.encryptParamString(thisClass.getName()?.toString())
        String __resourceVersion = grailsDataTablesService.encryptParamString(tableInfo.version()?.toString())
        __resourceName = grailsDataTablesService.gspBase64Uri(__resourceName)
        __resourceVersion = grailsDataTablesService.gspBase64Uri(__resourceVersion)
        String __id = grailsDataTablesService.gspBase64Uri("{resourceName=${__resourceName}, resourceVersion=${__resourceVersion}}", true)
        return (createLink(controller: "grailsDataTables", params: [id : __id])) as String
    }

    def DataTableAjax = { attrs, body ->

        /*
            Parameter section
            Logic here is to get and check data of the attribute values coming from the DataTableAjaxEditable tag
            from the gsp. id, data, url, selectable, etc.
         */

        //Id and UUID
        def id = attrs?.id
        if (Objects.isNull(id))
            throw new GrailsTagException("$datatableAjaxName id null")
        if (!(id instanceof String))
            throw new GrailsTagException("$datatableAjaxName id is not an instance of String.class")
        String uuid = UUID.randomUUID().toString()
        if (Objects.isNull(uuid) || uuid?.length() < 36) {
            throw new GrailsTagException("$datatableAjaxName failed to get a usable uuid")
        }


        //Options - default all to true
        Boolean searchable = attrs?.searchable != "false"
        Boolean selectable = attrs?.selectable != "false"
        Boolean orderable = attrs?.orderable != "false"
        Boolean pageable = attrs?.pageable != "false"
        String scrollY = (attrs?.scrollY instanceof String && attrs?.scrollY?.length() > 0) ? attrs.scrollY : null
        Boolean scrollX = attrs?.scrollX != "false"
        ArrayList<Integer> lengthMenu = null

        Boolean scrollCollapse
        if (attrs.scrollCollapse == "true")
            scrollCollapse = true
        else if (attrs.scrollCollapse == "false")
            scrollCollapse = false
        else if (scrollY)
            scrollCollapse = true
        else scrollCollapse = false

        //Make sure the passed in object is a list that is not null
        def data = attrs?.data
        if (Objects.isNull(data))
            throw new GrailsTagException("$datatableAjaxName passed data class is null")
        if (!(data instanceof Class))
            throw new GrailsTagException("$datatableAjaxName passed data is not an instance of a class")

        Class thisClass = (Class) data
        DataTable tableInfo = (DataTable) thisClass.getAnnotation(DataTable.class)
        if (Objects.isNull(tableInfo))
            throw new GrailsTagException("$datatableAjaxName passed data class not annotated with DataTable")
        if (tableInfo.type() != DataTableType.AJAX_TABLE)
            throw new GrailsTagException("$datatableAjaxName passed data class not of AJAX_TABLE")

        def url = getAjaxEndpointUri(thisClass, tableInfo)

        lengthMenu = tableInfo.lengthMenu()

        //Wanted exports
        ArrayList<String> wantedExports = new ArrayList<>()
        if (attrs?.wantedExports instanceof List<String>) {
            attrs?.wantedExports?.each { String key ->
                if (key == "copy")
                    wantedExports.add("copy")
                if (key == "excel")
                    wantedExports.add("excel")
                if (key == "csv")
                    wantedExports.add("csv")
                if (key == "pdf")
                    wantedExports.add("pdf")
                if (key == "print")
                    wantedExports.add("print")
            }
        }

        /*
            The remaining data and logic below is working on the annotations of the passed class.
         */

        ArrayList<String> columnNames = null
        ArrayList<String> ajaxColumnNames = null
        HashMap<String, HashMap<String, Object>> columnRenderInfos = null
        try {

            if (!(data instanceof Class))
                throw new GrailsTagException("$datatableAjaxName passed data is not a class")

            Class theTableClazz = (Class) data
            DataTable dt = theTableClazz.getAnnotation(DataTable.class) as DataTable
            if (dt == null)
                throw new GrailsTagException("$datatableAjaxName passed data does not have annotation DataTable")



            //Get all of the declared fields of the object
            ArrayList<Field> itsFields = new ArrayList<>()
            itsFields.addAll(theTableClazz.getDeclaredFields())

            //Find all fields annotated with DTColumn
            ArrayList<Field> fieldsWithDTColumn = new ArrayList<>()
            itsFields.each { aField ->
                //Get all annotations for a field. Loop through to find DTColumn annotation,
                //add it and break sub-loop to continue to next field
                Annotation needed = aField.getAnnotation(DTColumn.class)
                if (needed != null)
                    fieldsWithDTColumn.add(aField)
            }

            //Initialize the column names, and the order they are to be

            if (columnNames == null) {
                ajaxColumnNames = new ArrayList<>()
                columnNames = new ArrayList<>()
                columnRenderInfos = new HashMap<>()
                for (Field field : fieldsWithDTColumn) {

                    //Prelude get info
                    columnNames.add(field.getAnnotation(DTColumn.class).name())
                    DTColumn columnInfo = field.getAnnotation(DTColumn.class)
                    DTColumnRender renderInfo = field.getAnnotation(DTColumnRender)


                    //Check ajax names
                    String itsAjaxColumnName = columnInfo.ajaxColumnName()

                    if (Objects.isNull(itsAjaxColumnName) || Objects.equals(itsAjaxColumnName, ""))
                        throw new GrailsTagException("The field ${field?.name} of class ${theTableClazz?.toString()} has a default ajaxColumnName")
                    else if (Objects.equals(itsAjaxColumnName, field.name)) {
                        throw new GrailsTagException("The field ${field.name} of class ${theTableClazz?.toString()} has its ajaxColumnName set to its field name")
                    } else {


                        String outputAjaxParameterNameLog = "Adding ajax parameter name ${itsAjaxColumnName} for the field ${field.name}. This will not be logged again."
                        if (!grailsDataTablesService.hasDebugMessageBeenLogged(outputAjaxParameterNameLog))
                            log.debug(outputAjaxParameterNameLog)
                            ajaxColumnNames.add(itsAjaxColumnName)
                    }


                    //Set the map used for adding custom rendering to columns in datatable
                    HashMap<String, Object> colDefInfo = new HashMap<>()
                    colDefInfo.put("webUi_orderable", columnInfo.sortable())
                    if (Objects.nonNull(renderInfo)) {
                        //Custom renders
                        if (renderInfo.renderAs() == DTRenderTypes.GLINK) {
                            String __controllerName = renderInfo.controllerName()
                            String __actionName = renderInfo.actionName()
                            Object __link = null
                            if (__controllerName?.length() >= 1 && __actionName?.length() >= 1) {
                                __link = g.createLink([controller: __controllerName, action: __actionName])
                            } else if (__actionName?.length() >= 1) {
                                __link = g.createLink([action: __actionName])
                            } else {
                                throw new GrailsTagException("Field ${field.name} of class ${theTableClazz?.toString()} has a render modifier of GLINK. Annotation has controller or name as defaults.")
                            }
                            if (Objects.nonNull(__link)) {
                                colDefInfo.put("webUi_renderAs", "GLINK")
                                colDefInfo.put("webUi_link", __link)
                            }
                        } else if (renderInfo.renderAs() == DTRenderTypes.FORM_ACTION) {
                            String __controllerName = renderInfo.controllerName()
                            String __actionName = renderInfo.actionName()
                            String __submitButtonText = renderInfo.submitButtonText()
                            String __submitButtonCssClass = renderInfo.submitButtonCssClass()
                            String __formMethod = renderInfo.formMethod()
                            String __formParamName = renderInfo.formParamName()
                            Object __link = null
                            if (__controllerName?.length() >= 1 && __actionName?.length() >= 1) {
                                __link = g.createLink([controller: __controllerName, action: __actionName])
                            } else if (__actionName?.length() >= 1) {
                                __link = g.createLink([action: __actionName])
                            } else {
                                throw new GrailsTagException("Field ${field.name} of class ${theTableClazz?.toString()} has a render modifier of FORM_ACTION. Annotation has controller or name as defaults.")
                            }

                            if (Objects.isNull(__submitButtonText) || __submitButtonText?.length() <= 0)
                                throw new GrailsTagException("Field ${field.name} of class ${theTableClazz?.toString()} has a render modifier of FORM_ACTION. Submit button is required to have text.")
                            if (Objects.isNull(__formMethod) || !(["POST", "GET"]?.contains(__formMethod)))
                                throw new GrailsTagException("Field ${field.name} of class ${theTableClazz?.toString()} has a render modifier of FORM_ACTION but no usable http method. Value that was to be used: ${__formMethod}")


                            if (Objects.nonNull(__link)) {
                                colDefInfo.put("webUi_renderAs", "FORM_ACTION")
                                colDefInfo.put("webUi_link", __link)
                                colDefInfo.put("webUi_submitButtonText", __submitButtonText)
                                colDefInfo.put("webUi_submitButtonCssClass", __submitButtonCssClass)
                                colDefInfo.put("webUi_formUuid", (uuid + UUID.randomUUID()))
                                colDefInfo.put("webUi_formMethod", __formMethod)
                                colDefInfo.put("webUi_formAction", __actionName)
                                colDefInfo.put("webUi_formController", __controllerName)

                                //The property formParamName cannot be null when base64 is enabled due to ajaxColumnName being used as the id 'name' value for the form submission.
                                if (__formParamName == null && grailsDataTablesService.isBase64ParamMapConversionEnabled)
                                    throw new RuntimeException("DTColumnRender FORM_ACTION violated for Table '${theTableClazz.simpleName}' and field '${field}'.")

                                String fieldNameForToken = field.name
                                grailsDataTablesService.putSyncTokenRenderingLocation(actionName, controllerName, fieldNameForToken, theTableClazz)

                                colDefInfo.put("webUi_formToken", renderInfo.useToken())
                                colDefInfo.put("webUi_formParamName", __formParamName ?: columnInfo.ajaxColumnName())

                                if (Objects.isNull(__controllerName))
                                    throw new GrailsTagException("Cannot produce table due to DTColumnRender.renderAs 'FORM'. This render type requires controllers due to forms parameters.")

                                if (columnInfo.ajaxColumnName() == null || columnInfo.ajaxColumnName()?.length() <= 0) {
                                    throw new GrailsTagException("$datatableAjaxName has field ${field.name} to be used as FORM_ACTION, but no ajaxColumnName is set! ajaxColumnName is used for the hidden input name of the form!")
                                } else {
                                    colDefInfo.put("webUi_link_id_name", columnInfo.ajaxColumnName())
                                }
                            }
                        }
                        //Common
                        colDefInfo.put("webUi_color", renderInfo.colorStyle())
                        colDefInfo.put("webUi_width", renderInfo.columnWidth())
                        colDefInfo.put("webUi_height", renderInfo.columnHeight())
                    }
                    columnRenderInfos.put(itsAjaxColumnName, colDefInfo)
                }

                for (Field field : fieldsWithDTColumn) {
                    String columnFieldName = field.getAnnotation(DTColumn.class).name()
                    String ajaxColumnName = field.getAnnotation(DTColumn.class).ajaxColumnName()
                    Integer wantedIndex = field.getAnnotation(DTColumn.class).order()
                    Integer curIndex = columnNames.indexOf(columnFieldName)
                    if (curIndex != wantedIndex) {
                        String temp = columnNames.get(wantedIndex)
                        String tempAjax = ajaxColumnNames.get(wantedIndex)
                        columnNames.set(curIndex, temp)
                        ajaxColumnNames.set(curIndex, tempAjax)
                        columnNames.set(wantedIndex, columnFieldName)
                        ajaxColumnNames.set(wantedIndex, ajaxColumnName)
                    }
                }
            }

            out << render(
                    template: "/GrailsDataTables/templates/DT_AJAX_RD",
                    model: [
                            columns: columnNames,
                            id: id,
                            uuid: uuid,
                            searchable: searchable,
                            selectable: selectable,
                            orderable: orderable,
                            pageable: pageable,
                            scrollY: scrollY,
                            scrollX: scrollX,
                            scrollCollapse: scrollCollapse,
                            ajaxUrl: url,
                            ajaxColumnNames: ajaxColumnNames,
                            columnRenderInfos: columnRenderInfos,
                            wantedExports: wantedExports,
                            lengthMenu: lengthMenu
                    ],
                    plugin: "grailsDataTables"
            )

        } catch(NullPointerException e) {
            throw new GrailsTagException("Null pointer exception during DataTableAjax: ${e?.getMessage()}")
        }
        catch (Exception e) {
            throw new RuntimeException("Exception during DataTableAjax: ${e.getMessage()}")
        }
    }

    def DataTableAjaxEditable = { attrs, body ->

        /*
            Parameter section
            Logic here is to get and check data of the attribute values coming from the DataTableAjaxEditable tag
            from the gsp. id, data, url, selectable, etc.
         */

        //Id and UUID
        def id = attrs?.id
        if (Objects.isNull(id))
            throw new GrailsTagException("$datatableAjaxEditableName id null")
        if (!(id instanceof String))
            throw new GrailsTagException("$datatableAjaxEditableName id is not an instance of String.class")
        String uuid = UUID.randomUUID().toString()
        if (Objects.isNull(uuid) || uuid?.length() < 36) {
            throw new GrailsTagException("$datatableAjaxEditableName failed to get a usable uuid")
        }


        //Options - default all to true
        Boolean searchable = attrs?.searchable != "false"
        Boolean selectable = attrs?.selectable != "false"
        Boolean orderable = attrs?.orderable != "false"
        Boolean pageable = attrs?.pageable != "false"
        String scrollY = (attrs?.scrollY instanceof String && attrs?.scrollY?.length() > 0) ? attrs.scrollY : null
        Boolean scrollX = attrs?.scrollX != "false"
        ArrayList<Integer> lengthMenu = null


        Boolean scrollCollapse
        if (attrs.scrollCollapse == "true")
            scrollCollapse = true
        else if (attrs.scrollCollapse == "false")
            scrollCollapse = false
        else if (scrollY)
            scrollCollapse = true
        else scrollCollapse = false

        //Make sure the passed in object is a list that is not null
        def data = attrs?.data
        if (Objects.isNull(data))
            throw new GrailsTagException("$datatableAjaxEditableName passed data class is null")
        if (!(data instanceof Class))
            throw new GrailsTagException("$datatableAjaxEditableName passed data is not an instance of a class")

        Class thisClass = (Class) data
        DataTable tableInfo = (DataTable) thisClass.getAnnotation(DataTable.class)
        if (Objects.isNull(tableInfo))
            throw new GrailsTagException("$datatableAjaxEditableName passed data class not annotated with DataTable")
        if (tableInfo.type() != DataTableType.AJAX_TABLE_EDITABLE)
            throw new GrailsTagException("$datatableAjaxEditableName passed data class not of AJAX_TABLE_EDITABLE")

        def url = getAjaxEndpointUri(thisClass, tableInfo)

        lengthMenu = tableInfo.lengthMenu()



        //Wanted exports to add onto the editors configuration in template
        ArrayList<String> wantedCrud = new ArrayList<>()
        if (tableInfo.allowNew())
            wantedCrud.add("new")
        if (tableInfo.allowEdit())
            wantedCrud.add("edit")
        if (tableInfo.allowDelete())
            wantedCrud.add("delete")
        ArrayList<String> wantedExports = new ArrayList<>()
        if (attrs?.wantedExports instanceof List<String>) {
            attrs?.wantedExports?.each { String key ->
                if (key == "copy")
                    wantedExports.add("copy")
                if (key == "excel")
                    wantedExports.add("excel")
                if (key == "csv")
                    wantedExports.add("csv")
                if (key == "pdf")
                    wantedExports.add("pdf")
                if (key == "print")
                    wantedExports.add("print")
            }
        }

        /*
            The remaining data and logic below is working on the annotations of the passed class.
         */
        ArrayList<String> columnNames = null
        ArrayList<String> ajaxColumnNames = null
        ArrayList<String> editableAjaxColumnNames = null
        HashMap<String, HashMap<String, Object>> columnRenderInfos = null
        HashMap<String, Object> editableAjaxColumnFieldTypes = null
        try {

            if (!(data instanceof Class))
                throw new GrailsTagException("$datatableAjaxEditableName passed data is not a class")

            Class theTableClazz = (Class) data
            Annotation dt = theTableClazz.getAnnotation(DataTable.class)
            if (dt == null)
                throw new GrailsTagException("$datatableAjaxEditableName passed data does not have annotation DataTable")


            //Get all of the declared fields of the object
            ArrayList<Field> itsFields = new ArrayList<>()
            itsFields.addAll(theTableClazz.getDeclaredFields())

            //Find all fields annotated with DTColumn
            ArrayList<Field> fieldsWithDTColumn = new ArrayList<>()
            itsFields.each { aField ->
                //Get all annotations for a field. Loop through to find DTColumn annotation,
                //add it and break sub-loop to continue to next field
                Annotation needed = aField.getAnnotation(DTColumn.class)
                if (needed != null)
                    fieldsWithDTColumn.add(aField)
            }

            //Initialize the column names, and the order they are to be

            if (columnNames == null) {
                ajaxColumnNames = new ArrayList<>()
                columnNames = new ArrayList<>()
                editableAjaxColumnNames = new ArrayList<>()
                editableAjaxColumnFieldTypes = new HashMap<>()
                columnRenderInfos = new HashMap<>()
                for (Field field : fieldsWithDTColumn) {

                    //Prelude get info
                    columnNames.add(field.getAnnotation(DTColumn.class).name())
                    DTColumn columnInfo = field.getAnnotation(DTColumn.class)
                    DTColumnRender renderInfo = field.getAnnotation(DTColumnRender)


                    //Check ajax names
                    String itsAjaxColumnName = columnInfo.ajaxColumnName()

                    if (Objects.isNull(itsAjaxColumnName) || Objects.equals(itsAjaxColumnName, ""))
                        throw new GrailsTagException("The field ${field?.name} of class ${theTableClazz?.toString()} has a default ajaxColumnName")
                    else if (Objects.equals(itsAjaxColumnName, field.name)) {
                        throw new GrailsTagException("The field ${field.name} of class ${theTableClazz?.toString()} has its ajaxColumnName set to its field name")
                    } else {
                        String outputAjaxParameterNameLog = "Adding ajax parameter name ${itsAjaxColumnName} for the field ${field.name}. This will not be logged again."
                        if (!grailsDataTablesService.hasDebugMessageBeenLogged(outputAjaxParameterNameLog))
                            log.debug(outputAjaxParameterNameLog)
                        ajaxColumnNames.add(itsAjaxColumnName)
                    }


                    //Set the map used for adding custom rendering to columns in datatable
                    HashMap<String, Object> colDefInfo = new HashMap<>()
                    colDefInfo.put("webUi_orderable", columnInfo.sortable())
                    if (Objects.nonNull(renderInfo)) {
                        //Custom renders
                        if (renderInfo.renderAs() == DTRenderTypes.GLINK) {
                            String __controllerName = renderInfo.controllerName()
                            String __actionName = renderInfo.actionName()
                            Object __link = null
                            if (__controllerName?.length() >= 1 && __actionName?.length() >= 1) {
                                __link = g.createLink([controller: __controllerName, action: __actionName])
                            } else if (__actionName?.length() >= 1) {
                                __link = g.createLink([action: __actionName])
                            } else {
                                throw new GrailsTagException("Field ${field.name} of class ${theTableClazz?.toString()} has a render modifier of GLINK. Annotation has controller or name as defaults.")
                            }
                            if (Objects.nonNull(__link)) {
                                colDefInfo.put("webUi_renderAs", "GLINK")
                                colDefInfo.put("webUi_link", __link)
                            }
                        } else if (renderInfo.renderAs() == DTRenderTypes.FORM_ACTION) {
                            String __controllerName = renderInfo.controllerName()
                            String __actionName = renderInfo.actionName()
                            String __submitButtonText = renderInfo.submitButtonText()
                            String __submitButtonCssClass = renderInfo.submitButtonCssClass()
                            String __formMethod = renderInfo.formMethod()
                            String __formParamName = renderInfo.formParamName()

                            Object __link = null
                            if (__controllerName?.length() >= 1 && __actionName?.length() >= 1) {
                                __link = g.createLink([controller: __controllerName, action: __actionName])
                            } else if (__actionName?.length() >= 1) {
                                __link = g.createLink([action: __actionName])
                            } else {
                                throw new GrailsTagException("Field ${field.name} of class ${theTableClazz?.toString()} has a render modifier of FORM_ACTION. Annotation has controller or name as defaults.")
                            }

                            if (Objects.isNull(__submitButtonText) || __submitButtonText?.length() <= 0)
                                throw new GrailsTagException("Field ${field.name} of class ${theTableClazz?.toString()} has a render modifier of FORM_ACTION. Submit button is required to have text.")
                            if (Objects.isNull(__formMethod) || !(["GET", "POST"]?.contains(__formMethod)))
                                throw new GrailsTagException("Field ${field.name} of class ${theTableClazz?.toString()} has a render modifier of FORM_ACTION but no usable http method. Value that was to be used: ${__formMethod}")

                            if (Objects.nonNull(__link)) {
                                colDefInfo.put("webUi_renderAs", "FORM_ACTION")
                                colDefInfo.put("webUi_link", __link)
                                colDefInfo.put("webUi_submitButtonText", __submitButtonText)
                                colDefInfo.put("webUi_submitButtonCssClass", __submitButtonCssClass)
                                colDefInfo.put("webUi_formUuid", (uuid + UUID.randomUUID()))
                                colDefInfo.put("webUi_formMethod", __formMethod)
                                colDefInfo.put("webUi_formAction", __actionName)
                                colDefInfo.put("webUi_formController", __controllerName)

                                //The property formParamName cannot be null when base64 is enabled due to ajaxColumnName being used as the id 'name' value for the form submission.
                                if (__formParamName == null && grailsDataTablesService.isBase64ParamMapConversionEnabled)
                                    throw new RuntimeException("DTColumnRender FORM_ACTION violated for Table '${theTableClazz.simpleName}' and field '${field}'.")

                                String fieldNameForToken = field.name
                                grailsDataTablesService.putSyncTokenRenderingLocation(actionName, controllerName, fieldNameForToken, theTableClazz)

                                colDefInfo.put("webUi_formToken", renderInfo.useToken())
                                colDefInfo.put("webUi_formParamName", __formParamName ?: columnInfo.ajaxColumnName())

                                if (Objects.isNull(__controllerName))
                                    throw new GrailsTagException("Cannot produce table due to DTColumnRender.renderAs 'FORM'. This render type requires controllers due to forms parameters.")


                                if (columnInfo.ajaxColumnName() == null || columnInfo.ajaxColumnName()?.length() <= 0) {
                                    throw new GrailsTagException("$datatableAjaxEditableName has field ${field.name} to be used as FORM_ACTION, but no ajaxColumnName is set! ajaxColumnName is used for the hidden input name of the form!")
                                } else {
                                    colDefInfo.put("webUi_link_id_name", columnInfo.ajaxColumnName())
                                }
                            }
                        }
                        //
                        colDefInfo.put("webUi_color", renderInfo.colorStyle())
                        colDefInfo.put("webUi_width", renderInfo.columnWidth())
                        colDefInfo.put("webUi_height", renderInfo.columnHeight())
                    }
                    columnRenderInfos.put(itsAjaxColumnName, colDefInfo)

                    //Set the map used for adding rendering options to fields on datatable and editor
                    if (columnInfo.editable() && ajaxColumnNames.contains(itsAjaxColumnName)) {
                        String fieldTypeName = null
                        switch (columnInfo.type()) {
                            case DTColumnDataType.STRING:
                                fieldTypeName = "autocomplete"
                                break
                            case DTColumnDataType.TEXTAREA:
                                fieldTypeName = "textarea"
                                break
                            case DTColumnDataType.INTEGER:
                                fieldTypeName = "autocomplete"
                                break
                            case DTColumnDataType.LONG:
                                fieldTypeName = "autocomplete"
                                break
                            case DTColumnDataType.MONEY_US:
                                fieldTypeName = "autocomplete"
                                break
                            case DTColumnDataType.MONEY_POUND:
                                fieldTypeName = "autocomplete"
                                break
                            case DTColumnDataType.DATE:
                                fieldTypeName = "datetime"
                                break
                                //Images have to be resolved through server - way more complicated. I means resolving way more than just single requests
                            case DTColumnDataType.SINGLE_IMAGE:
                                fieldTypeName = "autocomplete"//fieldTypeName = "img-upload"
                        }
                        editableAjaxColumnFieldTypes.put(itsAjaxColumnName, fieldTypeName)
                        editableAjaxColumnNames.add(itsAjaxColumnName)
                    }
                }

                for (Field field : fieldsWithDTColumn) {
                    String columnFieldName = field.getAnnotation(DTColumn.class).name()
                    String ajaxColumnName = field.getAnnotation(DTColumn.class).ajaxColumnName()
                    Integer wantedIndex = field.getAnnotation(DTColumn.class).order()
                    Integer curIndex = columnNames.indexOf(columnFieldName)
                    if (curIndex != wantedIndex) {
                        String temp = columnNames.get(wantedIndex)
                        String tempAjax = ajaxColumnNames.get(wantedIndex)
                        columnNames.set(curIndex, temp)
                        ajaxColumnNames.set(curIndex, tempAjax)
                        columnNames.set(wantedIndex, columnFieldName)
                        ajaxColumnNames.set(wantedIndex, ajaxColumnName)
                    }
                }
            }



            //Set the columns that are editable (should show up in the edit form)

            out << render(
                    template: "/GrailsDataTables/templates/DT_AJAX_RW",
                    model: [
                            columns: columnNames,
                            id: id,
                            uuid: uuid,
                            searchable: searchable,
                            selectable: selectable,
                            orderable: orderable,
                            pageable: pageable,
                            scrollY: scrollY,
                            scrollX: scrollX,
                            scrollCollapse: scrollCollapse,
                            ajaxUrl: url,
                            ajaxColumnNames: ajaxColumnNames,
                            editableAjaxColumnNames: editableAjaxColumnNames,
                            editableAjaxColumnFieldTypes: editableAjaxColumnFieldTypes,
                            columnRenderInfos: columnRenderInfos,
                            wantedExports: wantedExports,
                            wantedCred: wantedCrud,
                            lengthMenu: lengthMenu
                    ],
                    plugin: "grailsDataTables"
            )

        } catch(NullPointerException e) {
            throw new GrailsTagException("Null pointer exception during $datatableAjaxEditableName: ${e?.getMessage()}")
        }
        catch (Exception e) {
            throw new RuntimeException("Exception during $datatableAjaxEditableName: ${e.getMessage()}")
        }
    }

}