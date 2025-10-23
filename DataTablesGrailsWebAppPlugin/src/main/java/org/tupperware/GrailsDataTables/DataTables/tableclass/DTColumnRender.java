package org.tupperware.GrailsDataTables.DataTables.tableclass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DTColumnRender {

    DTRenderTypes renderAs() default DTRenderTypes.NO_OP;

    String colorStyle() default "inherit";

    String columnWidth() default "inherit";

    String columnHeight() default "inherit";

    String controllerName() default "";

    String actionName() default "";

    String formMethod() default "POST";

    String formParamName() default "";

    boolean useToken() default false;

    String submitButtonText() default "submit";

    String submitButtonCssClass() default "btn btn-primary";

}