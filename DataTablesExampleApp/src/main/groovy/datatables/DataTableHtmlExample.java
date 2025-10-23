package datatables;

import org.tupperware.GrailsDataTables.DataTables.tableclass.DTColumn;
import org.tupperware.GrailsDataTables.DataTables.tableclass.DTColumnDataType;
import org.tupperware.GrailsDataTables.DataTables.tableclass.DataTable;
import org.tupperware.GrailsDataTables.DataTables.tableclass.DataTableType;

@DataTable(type = DataTableType.HTML_TABLE)
public class DataTableHtmlExample {

    @DTColumn(type = DTColumnDataType.STRING, name = "State's Name", order = 0, sortable = true)
    public String stateName;

    @DTColumn(type = DTColumnDataType.STRING, name = "State's 2 Alphabetic Code", order=1, sortable = true)
    public String stateCode;

    @DTColumn(type = DTColumnDataType.STRING, name = "State Capital", order=2, sortable=true)
    public String stateCapital;

    @DTColumn(type = DTColumnDataType.STRING, name = "State Nick Name", order=3)
    public String stateNickName;


}
