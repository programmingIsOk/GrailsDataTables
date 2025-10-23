package org.tupperware.GrailsDataTables.DataTables.tableclass;

@DataTable(type = DataTableType.HTML_TABLE)
public class DataTableInfo {

    @DTColumn(name = "Info Name", order = 0, type = DTColumnDataType.STRING)
    public String infoName;

    @DTColumn(name = "Info Value", order = 1, type = DTColumnDataType.STRING)
    public String infoValue;

}
