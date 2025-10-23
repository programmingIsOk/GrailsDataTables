<%@ page import="datatables.BasicInfo; datatables.DataTableHtmlExample" %>
<!doctype HTML>
<html>
    <head>
        <title>GrailsDataTables</title>
        <meta name="layout" content="main"/>
        <style>
            .demo-container {
                background-color: #5251517d;
                padding-bottom: 1em;
                margin-bottom: 1em;
            }
        </style>
    </head>
    <body>
        <div style="background-color: grey; margin: 25px; padding: 10px">
            <div class="container">
                <div class="row">
                    <div class="col-12">
                        <div class="container-fluid">
                            <div id="row">
                                <div class="">
                                    <h1>Grails Web Application Plugin</h1>
                                    <h2>GrailsDataTables</h2>
                                    <p>
                                        Here is a sort of homepage for the plugin. Here
                                        you will find information and examples regarding
                                        the plugin.
                                        This application is used as part of the testing of
                                        the plugin. See an issue? I'd be happy to hear!
                                    </p>
                                    <div>
                                        <h3>Docs</h3>
                                        <ul>
                                            <li><g:link uri="/GrailsDataTables/docs/groovydoc/index.html" style="color: black">Groovy</g:link></li>
                                            <li><g:link uri="/GrailsDataTables/docs/javadoc/index.html" style="color: black">Java</g:link></li>
                                        </ul>
                                        <h6 class="p-2"><g:link controller="editor" action="index" style="color: goldenrod">DataTables Example</g:link></h6>
                                    </div>
                                </div>
                            </div>
                            <div class="row">
                                <div class="demo-container p-2">
                                    <h2>Plugin Information</h2>
                                    <p>Each table will have a span with a background-color of 'wheat' to show the tag that initialized the rendering; e.g. the tag this text contains would have that information, and that would be:</p>
                                    <span style="background-color: wheat; white-space: pre-wrap">${'<WebUI:dataTableHtml data="${pluginInfo}" id="pluginInfo" sortable="false" orderable="false" searchable="false" pageable="false" selectable="false"/>'}</span>
                                    <WebUI:dataTableHtml forClass="${BasicInfo.class}" data="${pluginInfo}" id="pluginInfo" sortable="false" orderable="false" searchable="false" pageable="false" selectable="false"/>
                                </div>
                            </div>
                            <div class="row">
                                <div class="demo-container">
                                    <div class="p-2">
                                        <h2>Basic Html Table</h2>
                                        <span style="background-color: wheat; white-space: pre-wrap">${'<WebUI:dataTableHtml data="${statesAndCodes}" id="statesAndCodes" sortable="true" orderable="true" searchable="true"/>'}</span>
                                        <p>
                                            This is an example of a WebUI tag displaying a datatable given a list objects,
                                            who's class was annotated with @DataTable and @DTColumn annotations. These
                                            are created during the controllers action before passing these objects to
                                            the renderer. The WebUI:DataTableHtml will parse the objects for data, classes
                                            for the naming conventions and templates for rendering the HTML table.
                                            An uuid is used to initialize this specific table. A callback will remove
                                            the spinner, and show the table.
                                        </p>
                                        <div style="max-width: 100%">
                                            <WebUI:dataTableHtml forClass="${DataTableHtmlExample}" data="${statesAndCodes}" id="statesAndCodes" sortable="true" orderable="true" searchable="true" scrollX="true"/>
                                        </div>
                                        <p>The one below has no data in the list - to test the generation from the attribute forClass</p>
                                        <div style="max-width: 100%">
                                            <WebUI:dataTableHtml forClass="${DataTableHtmlExample}" data="${new ArrayList<DataTableHtmlExample>()}" id="statesAndCodes" sortable="true" orderable="true" searchable="true" scrollX="true"/>
                                        </div>
                                        <p>The one below has the data in the attributes set to null - to assert that the table is still generated</p>
                                        <div style="max-width: 100%">
                                            <WebUI:dataTableHtml forClass="${DataTableHtmlExample}" data="${null}" id="statesAndCodes" sortable="true" orderable="true" searchable="true" scrollX="true"/>
                                        </div>
                                    </div>
                                    
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </body>
</html>
