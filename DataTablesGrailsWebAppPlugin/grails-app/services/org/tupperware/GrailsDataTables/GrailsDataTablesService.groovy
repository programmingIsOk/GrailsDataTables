package org.tupperware.GrailsDataTables

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.orm.HibernateCriteriaBuilder
import grails.util.Environment
import grails.web.mapping.LinkGenerator
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.transform.CompileDynamic
import net.bytebuddy.description.annotation.AnnotationDescription
import org.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.procedure.ParameterMisuseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.validation.FieldError
import org.tupperware.GrailsDataTables.DataTables.tableclass.DTColumn
import org.tupperware.GrailsDataTables.DataTables.tableclass.DTRowId
import org.tupperware.GrailsDataTables.DataTables.tableclass.DataTable
import org.tupperware.GrailsDataTables.DataTables.tableclass.DataTableType

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.persistence.EntityManager
import javax.persistence.EntityTransaction
import javax.persistence.PersistenceException
import java.lang.annotation.Annotation
import java.lang.annotation.AnnotationFormatError
import java.lang.reflect.Field
import java.lang.reflect.Proxy
import java.text.SimpleDateFormat
import java.util.regex.Pattern

/**
 * The sole service, used for essentially everything.
 *
 * @since 0.1
 */
@CompileDynamic
class GrailsDataTablesService {

    static scope = "singleton"

    GrailsApplication grailsApplication
    LinkGenerator grailsLinkGenerator
    MessageSource messageSource

    //I want to obscure the info - encryption not for security
    private static String encrypt(String plainText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private String decrypt(String encryptedText, SecretKey key) throws Exception {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            log.error("Failed to decrypt param string", e)
            return null
        }
    }

    private static SecretKey tempParamKey = null
    private String encryptOrDecrypt(String string, Boolean encryptDecrypt) {
        if (tempParamKey == null) {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            tempParamKey = keyGen.generateKey()
        }
        if (encryptDecrypt) {
            String encrypted = encrypt(string, tempParamKey);
            return encrypted
        } else {
            String decrypted = decrypt(string, tempParamKey);
            return decrypted
        }
    }
    String encryptParamString(String string) {
        return encryptOrDecrypt(string, true)
    }
    String decryptParamString(String string) {
        return encryptOrDecrypt(string, false)
    }


    private static final ArrayList<String> loggedDebugMessaged = new ArrayList<>()
    static Boolean hasDebugMessageBeenLogged(String message) {
        try {
            if (loggedDebugMessaged.contains(message))
                return true
            else {
                loggedDebugMessaged.add(message)
                return false
            }
        } catch (Exception ignore) {
            return null
        }
    }

    private static String genericErrorMsg = "A system error has occurred"

    private static Logger logger = LoggerFactory.getLogger(this.class?.simpleName)

    static ArrayList<Field> getFieldsWithDTColumn(Class dataTableClass) {

        ArrayList<Field> fieldsWithDTColumnAnnotations = new ArrayList<>()
        for (Field field : dataTableClass.getDeclaredFields()) {
            try {
                DTColumn columnInfo = (DTColumn) field.getAnnotation(DTColumn)
                if (Objects.nonNull(columnInfo))
                    fieldsWithDTColumnAnnotations.add(field)
            } catch (NoSuchFieldException ignore) {
            }
        }

        /*
            TODO
                Below is to enforce the orders to be correct.
                I ended up using the order for a lot more than
                intended. So this makes sure places to check of order
                are were not taken into consideration.
         */

        ArrayList<Integer> orderedOrders = new ArrayList<>()
        for (Field field : fieldsWithDTColumnAnnotations) {
            orderedOrders.add(field.getAnnotation(DTColumn.class).order())
        }
        orderedOrders.sort()
        if (orderedOrders[0] != 0)
            throw new RuntimeException("DTColumn orders must start at 0, and be sequential!")
        for (int i = 0; i < orderedOrders.size(); i++) {
            if (orderedOrders[i] < 0)
                throw new RuntimeException("DTColumn order cannot be less than 0")
        }
        for (int x = 0; x < orderedOrders.size(); x++) {
            if (x != orderedOrders[x])
                throw new RuntimeException("DTColumn order at ${x} for ${dataTableClass} is not sequential")
        }

        return fieldsWithDTColumnAnnotations
    }

    private static final HashMap<String, HashMap<String, String>> tableClassFormRenderingLocations = new HashMap<>()
    private static final HashMap<String, String> tableFieldsFormRenderingLocations = new HashMap<>()

    String getSyncUrl(String tableName, String fieldName) {
        return (tableClassFormRenderingLocations[tableName])[fieldName]
    }

    void putSyncTokenRenderingLocation(String _actionName, String _controllerName, String fieldName, Class tableClass) {
        DataTable tableInfo = tableClass.getAnnotation(DataTable.class) as DataTable
        assert tableInfo != null
        assert _actionName != null
        assert _controllerName != null
        String urlForAction = grailsLinkGenerator.link(url: [action: _actionName, controller: _controllerName])?.toString()

        HashMap<String, HashMap<String, String>> tableLocations = tableClassFormRenderingLocations.get(tableClass.name)
        if (tableLocations == null) {
            HashMap<String, String> fieldAndUrl = new HashMap<>()
            fieldAndUrl.put(fieldName, urlForAction)
            tableClassFormRenderingLocations.put(tableClass.name, fieldAndUrl)
        } else {
            if (!Objects.equals(tableLocations.get(fieldName), urlForAction))
                throw new RuntimeException("Field Form rendered in two locations. This should not occur for CSRF protection")
        }

    }

    static DataTable getDataTableInfo(Class objClass, DataTableType adheresTo) {
        try {
            DataTable tableInfo = (DataTable) objClass.getAnnotation(DataTable)
            if (Objects.isNull(tableInfo))
                throw new RuntimeException("Object not annotated with DataTable")
            else {

                DataTableType thisTableTyep = null
                switch (tableInfo.class) {
                    case DataTable.class:
                        thisTableTyep = tableInfo.type()
                        break
                    case Proxy.class:
                        thisTableTyep = tableInfo?.memberValues?.type
                        break
                    case AnnotationDescription.AnnotationInvocationHandler:
                        thisTableTyep = tableInfo?.memberValues?.type
                        break
                }

                if (Objects.isNull(thisTableTyep))
                    throw new RuntimeException("DataTable forClass does not adhere to a type")
                else if (thisTableTyep != adheresTo)
                    throw new RuntimeException("DataTable forClass does not adhere to the type '${adheresTo}'. It adheres to '${thisTableTyep}'")
                else return tableInfo
            }
        } catch (Exception e) {
            logger.error("Exception was thrown getting DataTable class information", e)
            return null
        }
    }

    static Boolean isDataTableOfType(Class objClass, DataTableType type) {
        DataTable tableInfo = (DataTable) objClass.getAnnotation(DataTable)
        if (Objects.isNull(tableInfo))
            throw new RuntimeException("The object's class is not annotated with DataTable annotation")
        else {
            return type == tableInfo.type()
        }
    }
    static Boolean isDataTableOfType(Object objClass, DataTableType type) {
        logger.debug("Who be passing an object for this?")
        return isDataTableOfType(objClass.class, type)
    }

    private static List<HashMap<String, Object>> getMapForAjaxCall(List<Object> objects) {

        if (objects == null || objects.isEmpty())
            return new ArrayList<HashMap<String, Object>>()


        //map that will be returned
        ArrayList<HashMap<String, Object>> paramMap = new ArrayList<>()

        //Go ahead and check that it has DataTable annotation and DTColumn, etc.
        //Each object will be checked anyways but the information is needed
        //to smooth this out
        Class firstClass = objects.get(0).getClass()
        DataTable required = firstClass.getAnnotation(DataTable.class)
        if (required == null)
            throw new AnnotationFormatError("All classes in the list need to have the DataTable annotation")
        if (required.type() != DataTableType.AJAX_TABLE && required.type() != DataTableType.AJAX_TABLE_EDITABLE)
            throw new AnnotationFormatError("All classes in the list need to have the DataTableType of AJAX_TABLE")

        List<Field> dtFields = firstClass.getDeclaredFields() as List<Field>
        ArrayList<Field> wantedFields = new ArrayList<>()
        dtFields.each { Field it ->
            Annotation needed = it.getAnnotation(DTColumn.class)
            if (needed == null) {
                logger.debug("Field of class annotated with DataTable has a field not annotated with DTColumn, used in ajax. Skipping field")
            } else {
                if (needed.ajaxColumnName() == null || needed.ajaxColumnName().length() <= 0) {
                    logger.warn("Field of class annotated with DataTable of type Ajax, has a field annotated with DTColumn that has a default ajaxColumnName. Skipping field")
                } else {
                    wantedFields.add(it)
                }
            }
        }

        //we now have a list of fields of the class that will be used to insert the entries, and ajaxColumnName and value can be retrieved
        for (Object x : objects) {
            //Make sure that all objects are of the same class
            Class xClass = x.getClass()
            if (xClass != firstClass)
                throw new ParameterMisuseException("Not all objects are of the same class")
            //if so, make sure it has the annotation for being a datatable(idk if it can change in runtime?)
            DataTable xDataTableMeta = xClass.getAnnotation(DataTable.class)
            if (xDataTableMeta == null)
                throw new ParameterMisuseException("Not all object's classes have the annotation DataTable!")
            //make sure the datatable type is a ajax
            if (xDataTableMeta.type() != DataTableType.AJAX_TABLE && xDataTableMeta.type() != DataTableType.AJAX_TABLE_EDITABLE)
                throw new AnnotationFormatError("Not all object's DataTable annotation is not of DataTableType.AJAX_TABLE type")

            HashMap<String, Object> objParamMap = new HashMap<>()
            for (Field ajaxField : wantedFields) {
                DTColumn ajaxFieldMeta = ajaxField.getAnnotation(DTColumn.class)
                assert (ajaxFieldMeta.ajaxColumnName().length() >= 1 && ajaxFieldMeta.ajaxColumnName() != ajaxField.name)
                String newParamKey = ajaxFieldMeta.ajaxColumnName()
                Object newParamValue = x.properties.get(ajaxField.name)
                objParamMap.put(newParamKey, newParamValue.toString())
            }
            Field idRow = getDTRowIdField(firstClass)
            String stringyId = null
            try {
                switch (idRow.type) {
                    case String.class:
                        stringyId = x.properties.get(idRow.name)
                        break
                    case Integer.class:
                        stringyId = Integer.toString((Integer) x.properties.get(idRow.name))
                        break
                    case Long.class:
                        stringyId = Long.toString((Long) x.properties.get(idRow.name))
                        break
                }
            } catch (Exception e) {
                logger.error("Failed to get usable ID for the entity. Exception: ${e.getMessage()}")
                return null
            }

            if (Objects.isNull(stringyId)) {
                throw new RuntimeException("No usable id that can be cast to string!")
            }
            objParamMap.put("DT_RowId", stringyId)
            //paramMap.put("options", []) //idk what that is yet
            paramMap.add(objParamMap)
        }

        return (paramMap as List<HashMap<String, Object>>)

    }

    private static HashMap<String, Object> getAjaxSortingBys(GrailsParameterMap gParams) {

        //Data for DataTables
        HashMap<String, Object> returningMap = new HashMap<>()
        Integer offset
        Integer limit
        Integer draw
        try {
            offset = Integer.parseInt(gParams.get("start", "0"))
            limit = Integer.parseInt(gParams.get("length", "10"))
            draw = Integer.parseInt(gParams.get("draw", "0")) + 1
        } catch (Exception e) {
            logger.error("Exception getting DataTable default parameters! Setting to defaults")
            logger.error(e.getMessage())
            offset = 0
            limit = 10
            draw = 0
        }
        returningMap.put("draw", draw)
        returningMap.put("offset", offset)
        returningMap.put("limit", limit)


        ArrayList<HashMap<String, String>> columnNamesPresent = new ArrayList<>()
        for (int i = 0; i < gParams.keySet().size(); i++) {


            String key = gParams.keySet()[i]

            //If the parameter entry matches this regex, then do action.
            //Regex is for datatable parameter on post request, which has the columns ajaxColumnName

            //Intellij is being very rude, ignore him if he says the escape characters are not needed.
            if (Pattern.matches("columns\\[\\d]\\[data]", key)) {

                //Breaking everything in try blocks because it breaking and error no good :(

                //This contains the sorting info for this field.
                //which has all the information required for hibernate
                HashMap<String, String> columnInfo = new HashMap<>()

                String ajaxColumnName
                try {
                    ajaxColumnName = gParams.get(key, null)
                } catch (Exception e) {
                    ajaxColumnName = null
                    logger.error("Exception occurred attempting to get a column sorting name", e)
                }
                if (Objects.isNull(ajaxColumnName)) {
                    logger.warn("DataTable parameter for sorting is unusable, skipping column for sorting.")
                    continue
                } else {
                    columnInfo.put("ajaxName", ajaxColumnName)
                }

                Integer order
                try {
                    String orderStr = key.substring(
                            (key.indexOf("[")+1),
                            key.indexOf("]")
                    )
                    order = Integer.parseInt(
                            orderStr
                    )
                } catch (Exception e) {
                    order = null
                    logger.error("Exception occurred getting columns order information.", e)
                    //logger.error(e.stackTrace)
                }
                if (Objects.isNull(order)) {
                    logger.warn("Order information not gathered. Column will be skipped for sorting.")
                    continue
                } else {
                    columnInfo.put("order", order.toString())
                }

                Boolean searchable
                try {
                    searchable = gParams.get("columns[${order}][searchable]") == "true"
                } catch (Exception e) {
                    logger.debug("Failed to query for searchable parameter from parameters for column ordering. Searchable defaulting to false", e)
                    searchable = false
                }
                if (Objects.isNull(searchable)) {
                    logger.warn("Unable to determine searchable information. Column will be skipped for sorting.")
                    continue
                } else {
                    columnInfo.put("searchable", searchable.toString())
                }

                Boolean orderable
                try {
                    String orderableParamKey = "columns[${order}][orderable]"
                    String orderableParam = gParams.get(orderableParamKey)
                    orderable = Objects.equals(orderableParam, "true")
                } catch (Exception e) {
                    orderable = false
                    logger.error("Exception occurred determining if column is orderable or not. Defaulting to false", e)
                }

                columnInfo.put("orderable", orderable?.toString())

                String searchValue = null
                try {
                    if (gParams.get("columns[${order}][search][value]") != null)
                        searchValue = gParams.get("columns[${order}][search][value]", null)
                } catch (Exception e) {
                    searchValue = null
                    logger.error("Exception occurred getting search value for column.", e)
                }
                if (Objects.isNull(searchValue)) {
                    logger.debug("Search value is null but non-failure for searching. Defaulting to '' String")
                    searchValue = ""
                }

                columnInfo.put("searchValue", searchValue)

                String orderDir
                try {
                    //if columns[x][orderable] == "true" then it is in the ascending state
                    String xkey = "order[0][dir]"
                    Object orderDirVal = gParams.get(xkey)
                    if (orderDirVal == "desc")
                        columnInfo.put("dir", "desc")
                    else columnInfo.put("dir", "asc")
                } catch (Exception e) {
                    logger.error("Exception occurred getting orderDir", e)
                    columnInfo.put("dir", "asc")
                }

                //Is this the primary sorting column?
                Object mainColumnSort = gParams.get("order[0][column]")
                if (Objects.nonNull(mainColumnSort)) {
                    String mainColumnSortStr = (String) mainColumnSort
                    Integer mainColumnIndex = Integer.parseInt(mainColumnSortStr)
                    if (mainColumnIndex == order)
                        columnInfo.put("isPrimary", "true")
                    else columnInfo.put("isPrimary", "false")

                }

                columnNamesPresent.add(columnInfo)
            }
        }


        returningMap.put("primaryOrder", gParams.get("order[0][column]"))
        returningMap.put("columnSortInfo", columnNamesPresent)
        String searchValue = gParams.get("search[value]")
        String searchRegex = gParams.get("search[regex]")
        returningMap.put("searchValue", searchValue)
        returningMap.put("searchRegex", searchRegex)
        return returningMap

    }

    @Transactional
    private HashMap<String, Object> getSortedPagedDataTable(Object forClass, GrailsParameterMap gParams, SessionFactory sessionFactory) {

        HashMap<String, Object> params = null
        try {
            params = getAjaxSortingBys(gParams)
        } catch (Exception e) {
            logger.error("Exception getting DataTable http variables from params: ${e.getMessage()}")
        }
        if (Objects.isNull(params)) {
            logger.warn("Unable to get records without DataTable http variables. NULL will be returned.")
            return null
        }

        if (!(forClass instanceof Class))
            throw new RuntimeException("forClass is not a class")
        Class thisClass = (Class) forClass

        if (Objects.isNull(thisClass.getAnnotation(DataTable.class)))
            throw new AnnotationFormatError("This class does not have DataTable annotation")


        Integer _offset = null
        Integer _limit = null
        try {
            _offset = (Integer) params.get("offset")
            _limit = (Integer) params.get("limit")
            assert Objects.nonNull(_offset)
            assert Objects.nonNull(_limit)
        } catch (Exception e) {
            logger.error("Limit ${_limit} or offset ${_offset} unusable. e: ${e.getMessage()}")
            return  null
        }

        //Get columns to order by
        ArrayList<HashMap<String, String>> columnSortInfo = (ArrayList<HashMap<String, String>>) params.get("columnSortInfo")

        /**
         * This one will contain the criteria ordering info.
         * This one will have the field name, direction, and order number
         * Arraylist will be sorted on that.
         */
        ArrayList<HashMap<String, String>> orderSortInfo = new ArrayList<>()
        for (HashMap<String, String> dtColumnSortInfo : columnSortInfo) {
            if (dtColumnSortInfo.get("orderable", null) == "true") {

                //get field for this info
                Field fieldWithThisAjaxName = thisClass.getDeclaredFields()?.find { Field field ->
                    (field.getAnnotation(DTColumn.class)?.ajaxColumnName()) == dtColumnSortInfo.get("ajaxName")
                }
                if (Objects.isNull(fieldWithThisAjaxName))
                    continue

                ArrayList transientFields = null
                try {
                    Field transientFieldsField = thisClass.getDeclaredField("transients")
                    transientFieldsField.setAccessible(true)
                    transientFields = transientFieldsField.get(null) as ArrayList
                } catch (NoSuchFieldException e) {
                    logger.debug("Domain class has no transient field", e)
                }
                if (transientFields?.contains(fieldWithThisAjaxName.name))
                    continue

                HashMap<String, String> fieldClassSortInfo = new HashMap<>()
                fieldClassSortInfo.put("order", dtColumnSortInfo.get("order"))
                fieldClassSortInfo.put("dir", dtColumnSortInfo.get("dir"))
                fieldClassSortInfo.put("field", fieldWithThisAjaxName.name)
                fieldClassSortInfo.put("isPrimary", dtColumnSortInfo.get("isPrimary"))
                fieldClassSortInfo.put("dataType", fieldWithThisAjaxName.type.toString())
                orderSortInfo.add(fieldClassSortInfo)

            }
        }

        ArrayList<HashMap<String, String>> validOrders = new ArrayList<>()
        orderSortInfo.each { HashMap<String, String> it ->
            String isPrimary = it.get("isPrimary")
            String orderStr = it.get("order")
            if (Objects.nonNull(orderStr) && isPrimary == "true") {
                validOrders.add(it)
            }
        }

        String globalLikeString = (String) params.get("searchValue")
        Integer globalLikeStringInt
        Long globalLikeStringLong
        try {
            globalLikeStringInt = Integer.parseInt(globalLikeString)
        } catch (Exception ignore) {
            globalLikeStringInt = null
        }
        try {
            globalLikeStringLong = Long.parseLong(globalLikeString)
        } catch (Exception ignore) {
            globalLikeStringLong = null
        }

        HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) new HibernateCriteriaBuilder(thisClass, sessionFactory)

        Long totalCount
        try {
            totalCount = new HibernateCriteriaBuilder(thisClass, sessionFactory).count({})
        } catch (Exception e) {
            logger.error("Exception occurred. Failed to query for total count of records: ${e.getMessage()}\n${e.getStackTrace()}")
            totalCount = null
        }
        if (Objects.isNull(totalCount)) {
            logger.warn("With no total count, unable to query or supply datatables with accurate information. NULL will be returned")
            throw new RuntimeException("Query Count Failed")
        }

        Long resultsCount
        def results
        if (validOrders.size() == 1) {
            HashMap<String, String> orderInfo = validOrders.get(0)
            resultsCount = cb.count({
                if (orderInfo.get("dataType") == "class java.lang.String")
                    cb.like(orderInfo.get("field"), ("%"+globalLikeString+"%"))
                else if (orderInfo.get("dataType") == "class java.lang.Integer") {
                    if (globalLikeString != "" && Objects.isNull(globalLikeStringInt)) {
                        cb.isNotNull(orderInfo.get("field")) & {
                            cb.isNull(orderInfo.get("field"))
                        }
                    } else if (globalLikeString != "") {
                        cb.eq(orderInfo.get("field"), globalLikeStringInt)
                    }
                } else if (orderInfo.get("dataType") == "class java.lang.Long") {
                    if (globalLikeString != "" && Objects.isNull(globalLikeStringLong)) {
                        cb.isNotNull(orderInfo.get("field")) & {
                            cb.isNull(orderInfo.get("field"))
                        }
                    } else if (globalLikeString != "") {
                        cb.eq(orderInfo.get("field"), globalLikeStringLong)
                    }
                }
            })
            results = (new HibernateCriteriaBuilder(thisClass, sessionFactory)).list ({
                order(orderInfo.get("field"), orderInfo.get("dir"))
                if (orderInfo.get("dataType") == "class java.lang.String")
                    like(orderInfo.get("field"), ("%"+globalLikeString+"%"))
                else if (orderInfo.get("dataType") == "class java.lang.Integer") {
                    if (globalLikeString != "" && Objects.isNull(globalLikeStringInt)) {
                        isNotNull(orderInfo.get("field")) & {
                            isNull(orderInfo.get("field"))
                        }
                    } else if (globalLikeString != "") {
                        eq(orderInfo.get("field"), globalLikeStringInt)
                    }
                } else if (orderInfo.get("dataType") == "class java.lang.Long") {
                    if (globalLikeString != "" && Objects.isNull(globalLikeStringLong)) {
                        isNotNull(orderInfo.get("field")) & {
                            isNull(orderInfo.get("field"))
                        }
                    } else if (globalLikeString != "") {
                        eq(orderInfo.get("field"), globalLikeStringLong)
                    }
                }
                setMaxResults(_limit)
                setFirstResult(_offset)
            })
        } else {
            resultsCount = (new HibernateCriteriaBuilder(thisClass, sessionFactory)).count({})
            results = (new HibernateCriteriaBuilder(thisClass, sessionFactory)).list({
                setMaxResults(_limit)
                setFirstResult(_offset)
            })
        }

        HashMap<String, Object> resultMap = new HashMap<>()
        Object draw = params.get("draw")
        if (draw instanceof String)
            resultMap.put("draw", Integer.parseInt(params.get("draw") as String))
        else if (draw instanceof Integer)
            resultMap.put("draw", (Integer) draw)
        else {
            logger.error("Draw property unusable. NULL will be returned.")
            return null
        }
        resultMap.put("recordsTotal", totalCount)
        resultMap.put("recordsFiltered", (totalCount - (totalCount - resultsCount)))


        if (Objects.equals(thisClass.getMethod("hashCode").getDeclaringClass(), Object.class))
            throw new RuntimeException("TableClass must override its hashCode")
        resultMap.put("resourceName", thisClass.name)
        resultMap.put("resourceVersion", thisClass.hashCode())

        ArrayList<HashMap<String, Object>> paramMap = new ArrayList<>()

        ArrayList<Object> objects = new ArrayList<>()
        results.each {objects.add(it)}
        int insertCount = 0
        for (Object x : objects) {
            Class xClass = x.getClass()
            DataTable xDataTableMeta = xClass.getAnnotation(DataTable.class)
            if (xDataTableMeta == null)
                throw new ParameterMisuseException("Not all object's classes have the annotation DataTable!")
            //make sure the datatable type is a ajax
            if (xDataTableMeta.type() != DataTableType.AJAX_TABLE && xDataTableMeta.type() != DataTableType.AJAX_TABLE_EDITABLE)
                throw new AnnotationFormatError("Not all object's DataTable annotation is not of DataTableType.AJAX_TABLE type")

            List<Field> dtFields = thisClass.getDeclaredFields() as List<Field>
            ArrayList<Field> wantedFields = new ArrayList<>()
            dtFields.each { Field it ->
                Annotation needed = it.getAnnotation(DTColumn.class)
                if (needed == null) {
                    logger.debug("Field of class annotated with DataTable has a field not annotated with DTColumn, used in ajax. Skipping field")
                } else {
                    if (needed.ajaxColumnName() == null || needed.ajaxColumnName().length() <= 0) {
                        logger.warn("Field of class annotated with DataTable of type Ajax, has a field annotated with DTColumn that has a default ajaxColumnName. Skipping field")
                    } else {
                        wantedFields.add(it)
                    }
                }
            }

            HashMap<String, Object> objParamMap = new HashMap<>()
            Field columnForTracking = null
            for (Field ajaxField : wantedFields) {
                DTColumn ajaxFieldMeta = ajaxField.getAnnotation(DTColumn.class)
                DTRowId fieldForTracking = ajaxField.getAnnotation(DTRowId.class)
                if (Objects.nonNull(fieldForTracking)) {
                    columnForTracking = ajaxField
                }
                assert (ajaxFieldMeta.ajaxColumnName().length() >= 1 && ajaxFieldMeta.ajaxColumnName() != ajaxField.name)
                String newParamKey = ajaxFieldMeta.ajaxColumnName()
                Object newParamValue = x.properties.get(ajaxField.name)
                if (Objects.isNull(newParamValue) && (ajaxFieldMeta.type() == DTColumnDataType.STRING || ajaxFieldMeta.type() == DTColumnDataType.TEXTAREA))
                    newParamValue = ""

                //Get the ID used for DataTable tracking
                String dtRowId = null
                try {
                    if (Objects.nonNull(columnForTracking)) {
                        switch (columnForTracking.type) {
                            case String.class:
                                dtRowId = x.properties.get(columnForTracking.name)
                                break
                            case Integer.class:
                                dtRowId = Integer.toString((Integer) x.properties.get(columnForTracking.name))
                                break
                            case Long.class:
                                dtRowId = Long.toString((Long) x.properties.get(columnForTracking.name))
                                break
                            case Date.class:
                                dtRowId = Long.toString(((Date) (x.properties.get(columnForTracking.name))).time)
                                break
                        }
                    }
                } catch (Exception e) {
                    logger.error("Exception occurred getting entities DT_RowId: ${e?.getMessage()}")
                    return sendErrorOrGeneric("Failure to get tracking data for load action: ${e?.getMessage()}", false)
                }
                if (Objects.nonNull(dtRowId)) {
                    objParamMap.put("DT_RowId", dtRowId)
                } else {
                    objParamMap.put("DT_RowId", (insertCount++)?.toString())
                }

                //Prevent data from the users from causing an attack
                objParamMap.put(newParamKey, newParamValue.toString().encodeAsHTML())
            }


            paramMap.add(objParamMap)

        }

        resultMap.put("data", paramMap)
        if (isBase64ParamMapConversionEnabled)
            return doBase64OfResultMapParams(resultMap)
        else return resultMap

    }

    String gspRBase64Uri(String o, Boolean overrideConfig) {
        try {
            if (isBase64ParamMapConversionEnabled || overrideConfig) {
                return new String(Base64.decoder.decode(o.bytes))
            }
            else return o?.toString()
        } catch (Exception e) {
            logger.error(e)
            return null
        }
    }
    String gspRBase64Uri(String o) {
        try {
            if (isBase64ParamMapConversionEnabled)
                return new String(Base64.decoder.decode(o.bytes))
            else return o?.toString()
        } catch (Exception e) {
            logger.error(e)
            return null
        }
    }

    String gspBase64Uri(String o, Boolean overrideConfig) {
        try {
            if (isBase64ParamMapConversionEnabled || overrideConfig)
                return new String(Base64.encoder.encode(o.bytes))
            else return o
        } catch (Exception e) {
            logger.error(e)
            return null
        }

    }
    String gspBase64Uri(String o) {
        try {
            if (isBase64ParamMapConversionEnabled )
                return new String(Base64.encoder.encode(o.bytes))
            else return o
        } catch (Exception e) {
            logger.error(e)
            return null
        }

    }

    String gspBase64(def o) {
        try {
            if (isBase64ParamMapConversionEnabled)
                return new String(Base64.encoder.encode(o.bytes))
            else return o
        } catch (Exception e) {
            logger.error(e)
            return null
        }

    }

    String gspRBase64(def o) {
        try {
            if (isBase64ParamMapConversionEnabled)
                return new String(Base64.encoder.encode(o.bytes))
            else return o
        } catch (Exception e) {
            logger.error(e)
            return null
        }

    }


    boolean getIsBase64ParamMapConversionEnabled() {
        String warningMessage = "getIsBase64ParamMapConversionEnabled - Base64 conversion for requests not implemented. Defaulting to 'false'. This message will not be shown again."
        if (!hasDebugMessageBeenLogged(warningMessage))
            log.warn(warningMessage)
        return false
    }

    private static HashMap<String, Object> doBase64OfResultMapParams(HashMap<String, Object> resultMap) {
        HashMap<String, Object> base64ResultMap = new HashMap()
        for (String resultKey : resultMap.keySet()) {
            Object resultEntry = resultMap.get(resultKey)

            if (!(resultEntry instanceof Collection)) {
                base64ResultMap.put(resultKey.encodeAsBase64(), resultEntry/*.encodeAsBase64*/) //do values later
            } else {
                ArrayList<Object> resultEntryRecords = new ArrayList<>()
                for (Object resultRecordEntry : resultEntry as Collection) {
                    if (resultRecordEntry instanceof Map) {
                        HashMap<String, String> recordEntryBased64 = new HashMap<>()
                        for (String recordEntryKey : resultRecordEntry.keySet()) {
                            recordEntryBased64.put(recordEntryKey.encodeAsBase64(), resultRecordEntry.get(recordEntryKey))
                        }
                        resultEntryRecords.add(recordEntryBased64)
                    }
                }
                base64ResultMap.put(resultKey.encodeAsBase64(), resultEntryRecords)
            }
        }
        return base64ResultMap
    }

    private static Boolean isADataTable(Object forClass, DataTableType type) {
        Class theClass
        if (forClass instanceof Class)
            theClass = forClass
        else {
            logger.error("Passed forClass is not a object of Class")
            return false
        }

        DataTable tableInfo = (DataTable) theClass.getAnnotation(DataTable.class)
        if (Objects.isNull(tableInfo)) {
            logger.error("Passed forClass not annotated with DataTable")
            return null
        } else {
            return tableInfo.type() == type
        }
    }

    private static List<Field> getDTClassColumnFields(Object forClass, DataTableType type) {
        try {
            Class theClass = null
            if (forClass instanceof Class)
                theClass = forClass

            ArrayList<Field> dtColumnFields = new ArrayList<>()

            if (isADataTable(forClass, type)) {
                for (Field field : theClass.getDeclaredFields()) {
                    if (Objects.nonNull(field.getAnnotation(DTColumn.class))) {
                        dtColumnFields.add(field)
                    }
                }
            } else {
                logger.error("Passed forClass not annotated with DataTable.class or is not of type ${type}")
                return null
            }

            return dtColumnFields

        } catch (Exception e) {
            logger.error("Failed to get DTColumns for class. Exception: ${e?.getMessage()}")
            return null
        }
    }

    private static List<Field> getDTClassAjaxColumnFields(Object forClass, Boolean editable, DataTableType type) {
        try {
            ArrayList<Field> returningList = new ArrayList<>()
            List<Field> dtFields = getDTClassColumnFields(forClass, type)

            if (Objects.isNull(dtFields) || dtFields?.size() <= 0)
                return dtFields
            if (Objects.nonNull(dtFields) && Objects.isNull(editable))
                return dtFields
            else {
                for (Field field : dtFields) {
                    DTColumn columnInfo = field.getAnnotation(DTColumn.class)
                    Boolean allowUpdates = columnInfo.editable()
                    if (editable == allowUpdates)
                        returningList.add(field)
                }
            }

            return returningList

        } catch (Exception e) {
            logger.error("Failed to get DTColumns Ajaxables. Exception: ${e?.getMessage()}")
            return null
        }
    }

    private static Field getDTRowIdField(Object forClass) {

        Class theClass
        if (isADataTable(forClass, DataTableType.AJAX_TABLE_EDITABLE)) {
            theClass = (Class) forClass
        } else {
            logger.error("Passed forClass not a datatable of ajax")
            return null
        }

        ArrayList<Field> founds = new ArrayList<>()
        List<Field> itsFields = theClass.getDeclaredFields()
        for (Field field : itsFields) {
            DTRowId needed = field.getAnnotation(DTRowId.class)
            if (Objects.nonNull(needed))
                founds.add(field)
        }
        if (founds.size() == 1) {
            return founds.get(0)
        } else {
            logger.error("More than one field of the class has a DTRowId Annotation!")
            return null
        }
    }

    private HashMap<String, Object> doValidate(Object domain) {
        if (domain) {
            HashMap<String, Object> jsonError = new HashMap<>()
            //bru this will break? It'd be way better to get the collection of messages, rather than appending to a big string...
            String error = "Validation Errors: \n\t"
            domain?.errors?.allErrors?.each { FieldError it ->

                String resolvedMessage = null
                for (String code : it?.codes) {
                    try {
                        if (messageSource != null) {
                            resolvedMessage = messageSource.getMessage(code, null, LocaleContextHolder.getLocale())
                            if (Objects.nonNull(resolvedMessage))
                                break
                        } else {
                            logger.warn("MessageSource bean is null, cannot get formatted validator message")
                            break
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to resolve error code ${it}: ${e?.getMessage()}")
                        resolvedMessage = it.defaultMessage
                    }
                }

                Boolean defaultMessageUsed = false
                if (Objects.equals(resolvedMessage, "Property [{0}] of class [{1}] cannot be null")) {
                    defaultMessageUsed = true
                    resolvedMessage = resolvedMessage.replace("of class", "of")
                }

                //I have no idea if this should be a loop getting until no more arguments.
                //The three populated arguments come from errors API, aka additional is custom, the
                //first three are always "Property (the field)" The class/domain object and the rejected value
                if (resolvedMessage?.contains("{0}")) {

                    if (defaultMessageUsed) {
                        String fieldActualName = null
                        try {
                            fieldActualName = getDTClassAjaxColumnFields(domain.class, true, domain.class.getAnnotation(DataTable).type())?.find { Field dataTableField ->
                                dataTableField.name == ((String) it.arguments[0])
                            }?.getAnnotation(DTColumn)?.name()
                        } catch (Exception e) {
                            logger.debug("Error getting the actual name of the property being updated", e)
                        }

                        if (Objects.nonNull(fieldActualName))
                            resolvedMessage = resolvedMessage.replace("{0}", fieldActualName)
                        else resolvedMessage = resolvedMessage.replace("{0}", (String) it.arguments[0])

                    } else resolvedMessage = resolvedMessage.replace("{0}", (String) it.arguments[0])
                }
                if (resolvedMessage?.contains("{1}")) {
                    try {
                        resolvedMessage = resolvedMessage.replace("{1}", (String) ((Class) it.arguments[1]).simpleName)
                    } catch (Exception e) {
                        logger.debug("Argument thrown casting argument.", e)
                        resolvedMessage = resolvedMessage.replace("{1}", (String) it.arguments[1])
                    }
                }
                if (resolvedMessage?.contains("{2}"))
                    resolvedMessage = resolvedMessage.replace("{2}", (String) ((it.arguments[2])?.toString()))
                if (resolvedMessage?.contains("{3}"))
                    resolvedMessage = resolvedMessage.replace("{3}", (String) ((it.arguments[3])?.toString()))
                error += resolvedMessage + "\n\t"

            }
            jsonError.put("error", error)
            if (!Objects.equals(error, "Validation Errors: \n\t"))
                return jsonError
        }
        return null
    }

    private static HashMap<String, Object> doReturnOfAttached(Object attached) {
        try {
            HashMap<String, Object> paramMap = new HashMap<>()
            ArrayList updated = new ArrayList()
            updated.add(attached)
            paramMap.put("data", getMapForAjaxCall(updated))
            paramMap.put("options", [])
            return paramMap
        } catch (Exception e) {
            //Lets just hope this dont fail...
            logger.error("Exception returning attached object: ${e?.getMessage()}")
            return sendErrorOrGeneric("Unknown error occurred", true)
        }
    }

    @Transactional
    private HashMap<String, Object> serviceEditAction(Object forClass, GrailsParameterMap gParams, SessionFactory sessionFactory) {

        HashMap<String, Object> jsonData = [:]

        String id = null
        for (String key : gParams.keySet()) {
            try {
                id = key.substring((key.indexOf("[")+1), key.indexOf("]"))
                if (id != null)
                    break
            } catch (Exception e) {
                logger.error(e.getMessage())
            }
        }

        if (Objects.isNull(id)) {
            jsonData.put("error", "No usable ID")
            return jsonData
        }

        //Get DTColumns that are editable

        List<Field> updatingFields = getDTClassAjaxColumnFields(forClass, true, DataTableType.AJAX_TABLE_EDITABLE)
        Field rowIdField = getDTRowIdField(forClass)

        if (updatingFields.size() == 0) {
            logger.warn("There are no field to update for the class!")
            return null
        }
        if (Objects.isNull(rowIdField)) {
            throw new RuntimeException("There is no field for rowId. DataTable cannot track it.")
        }

        Class thisClass = (Class) forClass

        List result = null
        switch (rowIdField.type) {
            case String.class:
                result = (new HibernateCriteriaBuilder(thisClass, sessionFactory)).list({
                    eq(rowIdField.name, id)
                }) as List
                break
            case Integer.class:
                result = (new HibernateCriteriaBuilder(thisClass, sessionFactory)).list({
                    eq(rowIdField.name, Integer.parseInt(id))
                }) as List
                break
            case Long.class:
                result = (new HibernateCriteriaBuilder(thisClass, sessionFactory)).list({
                    eq(rowIdField.name, Long.parseLong(id))
                }) as List
                break
            case Date.class:
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
                Date toFind = sdf.parse(id)
                result = (new HibernateCriteriaBuilder(thisClass, sessionFactory)).list({
                    eq(rowIdField.name, toFind)
                }) as List
                break
        }
        if (Objects.isNull(result)) {
            logger.error("Could not find object by query")
            return null
        }
        if (result.size() != 1) {
            logger.error("Returning query for object to update returned more than 1")
            return null
        }

        Object resultToUpdate = result.get(0)

        HashMap<String, Object> updateParams = new HashMap<>()
        for (Field field : updatingFields) {
            DTColumn columnInfo = field.getAnnotation(DTColumn.class)
            String paramKey = "data[${id}][${columnInfo.ajaxColumnName()}]"
            updateParams.put(field.name, gParams.getOrDefault(paramKey, null))
        }

        if (Objects.nonNull(resultToUpdate)) {



            Object attachedToUpdate = null
            def validateException = null
            def savedResult = null
            try {

                Session rootSession = sessionFactory.openSession()
                if (!rootSession.isOpen()) {
                    return sendErrorOrGeneric("Session not open, unable to persist updated object", false)
                }


                def theDomainClass = grailsApplication.getDomainClass(thisClass.name)?.clazz
                theDomainClass."withTransaction" {
                    attachedToUpdate = theDomainClass."findWhere"([id: resultToUpdate."id"])
                    for (Field field : updatingFields) {
                        DTColumn columnInfo = field.getAnnotation(DTColumn.class)
                        String paramKey = "data[${id}][${columnInfo.ajaxColumnName()}]"
                        Object newValue = gParams.get(paramKey)
                        //Value from the datatables for the new value must be present.
                        assert newValue != null
                        if (((String) newValue)?.length() >= 1) {
                            try {
                                if (Objects.equals(newValue, ""))
                                    continue
                                logger.debug("Editable DataTable Column param.value not null, attempting to parse for the field's datatype : '${field?.getType()?.class?.toString()}'")
                                switch (field.type) {
                                    case String.class:
                                        attachedToUpdate."$field.name" = (String) newValue
                                        break
                                    case Integer.class:
                                        //Cast to a long first to see if it fails length-wise
                                        Long tempVal = Long.parseLong((String) newValue)
                                        if (tempVal > Integer.MAX_VALUE) {
                                            return sendErrorOrGeneric("Validation Errors: ${columnInfo.name()} has a number that's too large", true)
                                        } else if (tempVal < Integer.MIN_VALUE) {
                                            return sendErrorOrGeneric("Validation Errors: ${columnInfo.name()} has a number that's too small", true)
                                        }
                                        attachedToUpdate."$field.name" = Integer.parseInt((String) newValue)
                                        break
                                    case Long.class:
                                        attachedToUpdate."$field.name" = Long.parseLong((String) newValue)
                                        break
                                    case Date.class:
                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
                                        Date newDateValue = sdf.parse((String) newValue)
                                        attachedToUpdate."$field.name" = newDateValue
                                        break
                                }
                            } catch (NumberFormatException e) {
                                logger.warn(e?.getMessage())
                                return sendErrorOrGeneric("Validation Errors: ${columnInfo?.name()} rejected the value '${newValue?.toString()}'", true)
                            } catch (Exception e) {
                                logger.error("Failed to parse params for this datatype.", e)
                                return sendErrorOrGeneric("Validation Errors: ${columnInfo?.name()} rejected the value '${newValue?.toString()}'", true)
                            }
                        } else if (Objects.equals(newValue, ""))
                            attachedToUpdate."$field.name" = null
                    }

                    attachedToUpdate?."validate"()
                    validateException = doValidate(attachedToUpdate)

                    if (Objects.isNull(validateException)) {
                        savedResult = attachedToUpdate?."save"()
                        log.debug("serviceEditAction has called ${thisClass.simpleName} save method")
                    }

                }

            } catch (Exception e) {
                logger.error("Exception calling validate on domain", e)
                return sendErrorOrGeneric("Exception during validation call: ${e?.getMessage()}", true)
            }

            if (Objects.nonNull(validateException)) {
                return validateException
            } else {
                if (savedResult)
                    return doReturnOfAttached(attachedToUpdate)
                else return sendErrorOrGeneric("Not Saved", true)
            }

        } else {
            logger.error("Found no entity based on DT_RowId")
            return null
        }

    }

    @Transactional
    private HashMap<String, Object> serviceCreateAction(Object forClass, GrailsParameterMap gParams, SessionFactory sessionFactory) {

        HashMap<String, Object> jsonData = [:]

        String id = null
        for (String key : gParams.keySet()) {
            try {
                id = key.substring((key.indexOf("[")+1), key.indexOf("]"))
                if (id != null)
                    break
            } catch (Exception e) {
                logger.error(e.getMessage())
            }
        }

        if (Objects.isNull(id)) {
            jsonData.put("error", "No usable ID")
            return jsonData
        }

        //Get DTColumns that are editable

        List<Field> updatingFields = getDTClassAjaxColumnFields(forClass, true, DataTableType.AJAX_TABLE_EDITABLE)
        Field rowIdField = getDTRowIdField(forClass)

        if (updatingFields.size() == 0) {
            throw new RuntimeException("No field to update for the class, in create action.")
        }
        if (Objects.isNull(rowIdField)) {
            throw new RuntimeException("There is no field for rowId. DataTable cannot track it.")
        }

        Class thisClass = (Class) forClass
        List result = null
        switch (rowIdField.type) {
            case String.class:
                result = (new HibernateCriteriaBuilder(thisClass, sessionFactory)).list({
                    eq(rowIdField.name, id)
                }) as List
                break
            case Integer.class:
                result = (new HibernateCriteriaBuilder(thisClass, sessionFactory)).list({
                    eq(rowIdField.name, Integer.parseInt(id))
                }) as List
                break
            case Long.class:
                result = (new HibernateCriteriaBuilder(thisClass, sessionFactory)).list({
                    eq(rowIdField.name, Long.parseLong(id))
                }) as List
                break
            case Date.class:
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
                Date dateToUse = sdf.parse(id)
                result = (new HibernateCriteriaBuilder(thisClass, sessionFactory)).list({
                    eq(rowIdField.name, dateToUse)
                }) as List
                break
        }
        if (Objects.nonNull(result)) {
            if (result instanceof Collection<?>) {
                if (result?.size() >= 1)
                    sendErrorOrGeneric("The field ${rowIdField.name} will cause a unique constraint error", true)
            } else {
                sendErrorOrGeneric("The field ${rowIdField.name} will cause a unique constraint error", true)
            }
        }

        Object resultToUpdate = thisClass.newInstance()

        HashMap<String, Object> updateParams = new HashMap<>()
        for (Field field : updatingFields) {
            DTColumn columnInfo = field.getAnnotation(DTColumn.class)
            String paramKey = "data[${id}][${columnInfo.ajaxColumnName()}]"
            updateParams.put(field.name, gParams.getOrDefault(paramKey, null))
        }

        if (Objects.nonNull(resultToUpdate)) {

            Session rootSession = sessionFactory.openSession()
            if (!rootSession.isOpen()) {
                logger.error("Session not open, unable to persist updated object")
                return null
            }

            EntityManager em = sessionFactory.createEntityManager()
            Object attachedToUpdate = resultToUpdate
            //Object attachedToUpdate = rootSession.find(thisClass, resultToUpdate."id")

            for (Field field : updatingFields) {
                DTColumn columnInfo = field.getAnnotation(DTColumn.class)
                String paramKey = "data[${id}][${columnInfo.ajaxColumnName()}]"
                Object newValue = gParams.get(paramKey)
                assert newValue != null
                try {
                    //No need to set like the edit action
                    if (((String) newValue)?.length() >= 1)
                        switch (field.type) {
                            case String.class:
                                attachedToUpdate."$field.name" = (String) newValue
                                break
                            case Integer.class:
                                Long tempVal = Long.parseLong((String) newValue)
                                if (tempVal > Integer.MAX_VALUE) {
                                    return sendErrorOrGeneric("Validation Errors: ${columnInfo.name()} has a number that's too large", true)
                                } else if (tempVal < Integer.MIN_VALUE) {
                                    return sendErrorOrGeneric("Validation Errors: ${columnInfo.name()} has a number that's too small", true)
                                }
                                attachedToUpdate."$field.name" = Integer.parseInt((String) newValue)
                                break
                            case Long.class:
                                attachedToUpdate."$field.name" = Long.parseLong((String) newValue)
                                break
                            case Date.class:                                        //TODO add custom date formats provided through application.yml - if not preset, have it be the defaults datatables does 'yyyy-MM-dd'
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
                                Date newValueDate = sdf.parse((String) newValue)
                                attachedToUpdate."$field.name" = newValueDate
                                break
                        }
                } catch (Exception e) {
                    logger.error("Failed to parse params for this datatype. Exception: ${e.getMessage()}")
                }

            }

            def savedResult = null
            String error
            def validateException = null
            try {
                def theDomainClass = grailsApplication.getDomainClass(thisClass.name)?.clazz
                theDomainClass."withTransaction" {
                    attachedToUpdate = theDomainClass.newInstance(attachedToUpdate.properties)
                    attachedToUpdate."validate"()

                     validateException = doValidate(attachedToUpdate)

                    if (Objects.isNull(validateException)) {
                        savedResult = attachedToUpdate."save"()
                    }

                }

            } catch (Exception e) {
                logger.error("Exception occurred rolling back transaction for create action")
                return sendErrorOrGeneric("Exception occurred: ${e?.getMessage()}", false)
            }

            if (Objects.nonNull(validateException))
                return validateException
            else {
                if (savedResult)
                    return doReturnOfAttached(attachedToUpdate)
                else return sendErrorOrGeneric("Did Saved", true)
            }
        } else {
            return sendErrorOrGeneric("Found no entity based on DT_RowID", false)
        }
    }

    private static HashMap<String, Object> sendErrorOrGeneric(String message, Boolean anyone) {

        try {
            logger.error(message)
        } catch (Exception e) {
            logger.error(e?.getMessage())
        }

        HashMap<String, Object> jsonError = new HashMap<>()
        if (Objects.isNull(message)) {
            jsonError.put("error", genericErrorMsg)
        } else {
            if (Environment.current != Environment.DEVELOPMENT && (!anyone || message.length() <= 0)) {
                message = genericErrorMsg
            }
            jsonError.put("error", message)
        }
        return jsonError
    }

    private HashMap<String, Object> _serviceDataTableAjaxCall(Object forClass, GrailsParameterMap gParams, SessionFactory sessionFactory) {

        if (Objects.isNull(sessionFactory)) {
            return sendErrorOrGeneric("Cannot connect. No Session.", false)
        }

        if (!(forClass instanceof Class)) {
            return sendErrorOrGeneric("No entity defines the table", false)
        }

        Class thisClass = (Class) forClass
        DataTable tableInfo
        try {
            tableInfo = (DataTable) thisClass.getAnnotation(DataTable.class)
        } catch (Exception e) {
            logger.error("Exception while getting DataTable annotation for info: ${e?.getMessage()}")
            return sendErrorOrGeneric("Exception getting table annotation information", false)
        }
        if (Objects.isNull(tableInfo))
            return sendErrorOrGeneric("Entity defining table has no table annotation information", false)

        String dataTableAction = gParams.getOrDefault("webui_editor_action", null)
        if (Objects.isNull(dataTableAction)) {
            logger.debug("Servicing datatable ajax: loading data")
            return getSortedPagedDataTable(forClass, gParams, sessionFactory)
        } else {
            //All reject messages are obscured on why it is not allowed - because those buttons shouldn't be visible
            switch (dataTableAction) {
                case "edit":
                    logger.debug("Servicing datatable ajax: edit")
                    if (!tableInfo.allowEdit()) {
                        return sendErrorOrGeneric("edit action not allowed", false)
                    }
                    return serviceEditAction(forClass, gParams, sessionFactory)
                    break
                case "create":
                    logger.debug("Servicing datatable ajax: creating data")
                    if (!tableInfo.allowNew()) {
                        return sendErrorOrGeneric("create action not allowed", false)
                    }
                    return serviceCreateAction(forClass, gParams, sessionFactory)
                case "remove":
                    if (!tableInfo.allowDelete()) {
                        return sendErrorOrGeneric("remove action not allowed", false)
                    }
                    return sendErrorOrGeneric("Remove action not implemented", false)
                    break
                default:
                    return getSortedPagedDataTable(forClass, gParams, sessionFactory)
            }
        }
    }

    HashMap<String, Object> serviceDataTableAjaxCall(Object forClass, GrailsParameterMap gParams, SessionFactory sessionFactory) {

        try {

            try {
                return _serviceDataTableAjaxCall(forClass, gParams, sessionFactory)
            } catch (Exception e) {
                return sendErrorOrGeneric("Exception during service call: ${e?.getMessage()}, cause: ${e?.getCause()}", false)
            }

        } catch (Exception e) {
            logger.error("DataTableAjaxCall Exception", e)
            HashMap<String, Object> jsonError = new HashMap<>()
            jsonError.put("error", genericErrorMsg)
            return jsonError
        }


    }

    @Transactional
    HashMap<String, Object> serviceDataTableAjaxCall(GrailsParameterMap params) {

        String resourcesIds = params.getOrDefault("id", null) as String
        resourcesIds = gspRBase64Uri(resourcesIds, true)

        Integer indexOfResourceNameStart = resourcesIds.indexOf("resourceName=") + 13
        Integer indexOfResourceNameEnd =  resourcesIds.indexOf(", resourceVersion=")
        String resourceName = resourcesIds.substring(
                indexOfResourceNameStart, indexOfResourceNameEnd
        )
        resourceName = decryptParamString(resourceName)
        if (resourceName == null)
            return sendErrorOrGeneric("Failed to decrypt resourceName for request", false)
        resourceName = gspRBase64Uri(resourceName)

        Integer indexOfResourceVersionStart = resourcesIds.indexOf(", resourceVersion=") + 18
        Integer indexOfResourceVersionEnd = resourcesIds.length() - 1
        String resourceVersion = resourcesIds.substring(
                indexOfResourceVersionStart, indexOfResourceVersionEnd
        )
        resourceVersion = decryptParamString(resourceVersion)
        if (resourceVersion == null)
            return sendErrorOrGeneric("Failed to decrypt resourceVersion for request", false)
        resourceVersion = gspRBase64Uri(resourceVersion)

        Class<?> tableClass = Class.forName(resourceName)
        DataTable tableInfo = tableClass.getAnnotation(DataTable.class) as DataTable
        if (tableInfo == null)
            throw new RuntimeException("The class specified by the request is not annotated with DataTables")

        if (tableInfo.version() == 0L) {
            throw new RuntimeException("The resource '${resourceName}' has a default version of '0'")
        }

        if (!Objects.equals(Long.parseLong(resourceVersion), tableInfo.version()))
            throw new RuntimeException("The requested resource on the HttpRequest did not match the requested version of the resource")
        else {

            List<String> mappedResources = GrailsDomainBinder.getMapping(tableClass)?.getDatasources()

            if (mappedResources?.size >=  1) {
                String mappedResource = mappedResources.get(0)
                SessionFactory sessionFactoryDependency = grailsApplication.getMainContext().getBean("sessionFactory_" + mappedResource)
                assert sessionFactoryDependency != null
                return serviceDataTableAjaxCall(tableClass, params, sessionFactoryDependency)
            } else {
                throw new RuntimeException("No mapped resources")
            }


        }

    }
}