package datatables;


import org.tupperware.GrailsDataTables.DataTables.tableclass.DTColumn;
import org.tupperware.GrailsDataTables.DataTables.tableclass.DataTable;
import org.tupperware.GrailsDataTables.DataTables.tableclass.DataTableType;

@DataTable(type = DataTableType.HTML_TABLE)
public class BasicInfo {

    @DTColumn(name = "Info", order = 0)
    public String info;

    @DTColumn(name = "Message", order = 1)
    public String message;

    @DTColumn(name = "More Info", order = 2)
    public String moreInfo;

}
