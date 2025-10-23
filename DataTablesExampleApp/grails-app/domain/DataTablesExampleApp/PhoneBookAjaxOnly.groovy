package DataTablesExampleApp

import org.tupperware.GrailsDataTables.DataTables.tableclass.DTColumn
import org.tupperware.GrailsDataTables.DataTables.tableclass.DTColumnRender
import org.tupperware.GrailsDataTables.DataTables.tableclass.DTRowId
import org.tupperware.GrailsDataTables.DataTables.tableclass.DataTable
import org.tupperware.GrailsDataTables.DataTables.tableclass.DataTableType
import org.tupperware.GrailsDataTables.DataTables.tableclass.DTColumnDataType
import org.tupperware.GrailsDataTables.DataTables.tableclass.DTRenderTypes

import javax.persistence.PersistenceException
import javax.persistence.Transient

@DataTable(type = DataTableType.AJAX_TABLE)
class PhoneBookAjaxOnly implements Serializable {

    @DTRowId
    @DTColumn(name = "Line Number", type = DTColumnDataType.LONG, order = 0, sortable = true, ajaxColumnName = "line_number")
    Long lineNumber

    @DTColumnRender(renderAs = DTRenderTypes.FORM_ACTION, submitButtonCssClass = "btn btn-success", formMethod="GET", controllerName="editor", actionName = "show", colorStyle = "yellow", submitButtonText = "Show the Thingy")
    @DTColumn(name = "Action", type = DTColumnDataType.INTEGER, sortable = false, order = 1, ajaxColumnName = "persons_phone_number")
    private Integer personsNumber2
    public Integer getPersonsNumber2() {
        return this.phoneNumber
    }

    @DTColumn(name = "Persons Name", type = DTColumnDataType.STRING, order = 2, sortable = true, ajaxColumnName = "persons_name")
    String personsName

    @DTColumn(name = "Phone Number", type = DTColumnDataType.INTEGER, order = 3, sortable = true, ajaxColumnName = "persons_phone_number2")
    Integer phoneNumber

    @DTColumn(name = "Date", type = DTColumnDataType.DATE, order = 4, sortable = true, ajaxColumnName = "persons_phone_number_date_field")
    Date phoneNumberDate

    Byte[] phoneBookImage

    @DTColumn(name = "Date Created", type = DTColumnDataType.STRING, order = 5, sortable = true, ajaxColumnName = "persons_phone_number_created")
    Date dateCreated

    @DTColumn(name = "Last Activity", type = DTColumnDataType.STRING, order = 6, sortable = true, ajaxColumnName = "persons_phone_number_activity")
    Date activityTimeStamp

    @DTColumn(name = "Update Count", type = DTColumnDataType.STRING, order = 7, sortable = true, ajaxColumnName = "person_phone_book_record_update_count")
    Integer version

    @DTColumnRender(columnWidth = "400px", columnHeight = "50px")
    @DTColumn(name = "Large Text", type = DTColumnDataType.TEXTAREA, order = 8, sortable = true, ajaxColumnName = "persons_phone_book_large_text")
    String largeText

    @DTColumn(name = "Transient", type = DTColumnDataType.STRING, order=9, sortable=false, ajaxColumnName = "persons_transient")
    String dateCreatedAndActivityTransient

    String getDateCreatedAndActivityTransient() {
        return "Name: ${personsName} Version: ${version}"
    }

    static transients = ['dateCreatedAndActivityTransient']

    //Gorm events
    def beforeValidate() {
        def theTransients = transients
        try {
            this.activityTimeStamp = new Date()
            this.lineNumber = id
        } catch (Exception e) {
            log.error("Exception occurred in gorm event 'beforeValidate()': ${e?.getMessage()}")
            throw new PersistenceException("Failed to do checks before validation")
        }
    }
    def beforeUpdate() {
        try {
            this.activityTimeStamp = new Date()
            this.lineNumber = id
        } catch (Exception e) {
            log.error("Exception occurred in gorm event 'beforeUpdate()': ${e?.getMessage()}")
            throw new PersistenceException("Failed to do checks before update")
        }
    }
    def beforeInsert() {
        try {
            this.activityTimeStamp = new Date()
            this.dateCreated = new Date()
            this.lineNumber = id
        } catch (Exception e) {
            log.error("Exception occurred in gorm event 'beforeInsert()': ${e?.getMessage()}")
            throw new PersistenceException("Failed to do checks before insert")
        }
    }

    static constraints = {
        personsName (nullable: false, unique: true, validator: {
            //Invalidate if there is a number character
            if (it?.size() >= 1)
                for (int i = 0; i < 10; i++ ) {
                    String iStr = Integer.toString(i)
                    int foundIndex = it?.indexOf(iStr)
                    if (foundIndex >= 0) {
                        return ['nameHasNumbers']
                    }
                }
        })
        phoneNumber nullable: false, unique: true, min: 0, max: Integer.MAX_VALUE
        lineNumber nullable: true
        phoneBookImage nullable: true, maxSize: 25000
        phoneNumberDate nullable: false
        largeText minSize: 25, maxSize: 1500, nullable: false
    }
    static mapping = {
        datasource("PhoneBook")
        table("`PhoneBook`")

        id column: "`Id`", sqlType: "int", generator: "increment"
        lineNumber column: "`Id`", sqlType: "int", insertable: false, updateable: false
        version column: "`Version`"

        personsName column: "`PersonsName`"
        phoneNumber column: "`PhoneNumber`"
        phoneNumberDate column: "`PhoneNumberDate`"
        dateCreated column: "`DateCreated`"
        activityTimeStamp column: "`ActivityTimeStamp`"
        phoneBookImage column: "`PhoneImage`", sqlType: "image"
        largeText column: "`LargeText`"
    }

    PhoneBookAjaxOnly(ArrayList[] o ) {}

    boolean equals(o) {
        if (this.is(o)) return true
        if (o == null || getClass() != o.class) return false

        PhoneBookAjaxOnly that = (PhoneBookAjaxOnly) o

        if (org_grails_datastore_gorm_GormValidateable__skipValidate != that.org_grails_datastore_gorm_GormValidateable__skipValidate) return false
        if (activityTimeStamp != that.activityTimeStamp) return false
        if (dateCreated != that.dateCreated) return false
        if (dateCreatedAndActivityTransient != that.dateCreatedAndActivityTransient) return false
        if (id != that.id) return false
        if (largeText != that.largeText) return false
        if (lineNumber != that.lineNumber) return false
        if (org_grails_datastore_gorm_GormValidateable__errors != that.org_grails_datastore_gorm_GormValidateable__errors) return false
        if (org_grails_datastore_mapping_dirty_checking_DirtyCheckable__$changedProperties != that.org_grails_datastore_mapping_dirty_checking_DirtyCheckable__$changedProperties) return false
        if (personsName != that.personsName) return false
        if (personsNumber2 != that.personsNumber2) return false
        if (!Arrays.equals(phoneBookImage, that.phoneBookImage)) return false
        if (phoneNumber != that.phoneNumber) return false
        if (phoneNumberDate != that.phoneNumberDate) return false
        if (version != that.version) return false

        return true
    }

    int hashCode() {
        int result
        result = (lineNumber != null ? lineNumber.hashCode() : 0)
        result = 31 * result + (personsNumber2 != null ? personsNumber2.hashCode() : 0)
        result = 31 * result + (personsName != null ? personsName.hashCode() : 0)
        result = 31 * result + (phoneNumber != null ? phoneNumber.hashCode() : 0)
        result = 31 * result + (phoneNumberDate != null ? phoneNumberDate.hashCode() : 0)
        result = 31 * result + (phoneBookImage != null ? Arrays.hashCode(phoneBookImage) : 0)
        result = 31 * result + (dateCreated != null ? dateCreated.hashCode() : 0)
        result = 31 * result + (activityTimeStamp != null ? activityTimeStamp.hashCode() : 0)
        result = 31 * result + (version != null ? version.hashCode() : 0)
        result = 31 * result + (largeText != null ? largeText.hashCode() : 0)
        result = 31 * result + (dateCreatedAndActivityTransient != null ? dateCreatedAndActivityTransient.hashCode() : 0)
        result = 31 * result + (id != null ? id.hashCode() : 0)
        result = 31 * result + (org_grails_datastore_mapping_dirty_checking_DirtyCheckable__$changedProperties != null ? org_grails_datastore_mapping_dirty_checking_DirtyCheckable__$changedProperties.hashCode() : 0)
        result = 31 * result + (org_grails_datastore_gorm_GormValidateable__skipValidate ? 1 : 0)
        result = 31 * result + (org_grails_datastore_gorm_GormValidateable__errors != null ? org_grails_datastore_gorm_GormValidateable__errors.hashCode() : 0)
        return result
    }
}