package org.tupperware.GrailsDataTables

/**
 * An interceptor that simply checks if the params are at minimum, not default.
 * Will be used later for expandability.
 * Felt like this MAY/WILL be needed, so having it here just for in mind.
 *
 * @author Tupper-Jacob
 * @since 0.1
 */
class GrailsDataTablesInterceptor {

    private static final String actionNameKey = "action"
    private static final String requiredActionName = "datatable"

    private static final String controllerNameKey = "controller"
    private static final String requiredControllerName = "grailsDataTables"

    boolean before() {

        String actionOnParams = params.getOrDefault(actionNameKey, null)
        String controllerOnParams = params.getOrDefault(controllerNameKey, null)

        if (!Objects.equals(actionOnParams, requiredActionName) || !Objects.equals(controllerOnParams, requiredControllerName) || params.size() == 2) {
            log.warn("Something has gone seriously wrong - action and controller not matching!")
            return false
        } else {
            return true
        }
    }

    boolean after() { true }

    void afterView() {
    // no-op
    }

    GrailsDataTablesInterceptor() {
        match(controller: "grailsDataTables")
    }

}