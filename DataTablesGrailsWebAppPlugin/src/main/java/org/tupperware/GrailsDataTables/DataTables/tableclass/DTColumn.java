package org.tupperware.GrailsDataTables.DataTables.tableclass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DTColumn {

    DTColumnDataType type() default DTColumnDataType.STRING;

    String name() default "";

    int order() default 0;

    String ajaxColumnName() default "";

    boolean sortable() default false;

    boolean editable() default false;

    @Deprecated
    boolean allowNulls() default false;

}
