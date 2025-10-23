package org.tupperware.GrailsDataTables.DataTables.tableclass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DataTable {

    DataTableType type() default DataTableType.HTML_TABLE;

    boolean allowNew() default false;

    boolean allowEdit() default false;

    boolean allowDelete() default false;

    int[] lengthMenu() default {5, 10, 25, 50};

}
