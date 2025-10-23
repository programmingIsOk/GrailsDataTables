<%--
  Created by IntelliJ IDEA.
  User: jacob
  Date: 3/20/2025
  Time: 9:13 PM
--%>

<%@ page import="DataTablesExampleApp.PhoneBookAjaxOnly; DataTablesExampleApp.PhoneBook" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Editor</title>
    <meta name="layout" content="main"/>
    <asset:stylesheet src="DataTables/datatables.css"/>
    <asset:javascript src="DataTables/datatables.js"/>
</head>

<body>
<div style=" margin: 2%; padding: 2%">
    <div class="container">
        <g:if test="${flash?.message}">
            <div class="row">
                <div class="col">
                    <h2 style="    background-color: black;
                    text-align: center;
                    max-width: 50%;
                    margin-right: auto;
                    margin-left: auto;" class="message">${flash.message}</h2>
                </div>
            </div>
        </g:if>
        <div class="row" style="background-color: grey;">
            <div class="col">
                <div style="padding: 8px">
                    <h1>DataTable Editor Example</h1>
                    <g:link style="color: black" controller="index">GoTo Index Controller</g:link>
                </div>
            </div>
        </div>
        <div class="row" style="background-color: grey;">
                <div class="col p-1 m-1" style="overflow-x: auto">
                    <div><h2>DataTableAjaxEditable - Using Gorm Domain Classes, all Grails</h2></div>
                    <div>
                        <GDTables:dataTableAjaxEditable data="${PhoneBook}" id="phonebook-info" wantedExports="${['csv'] as List<String>}" scrollX="400px"/>
                    </div>
                </div>
        </div>
        <div class="row" style="background-color: grey">
            <div class="col-sm-12">
                <h2>AjaxTable</h2>
                <GDTables:dataTableAjax data="${PhoneBookAjaxOnly}" id="phonebook-info"/>
            </div>
        </div>
    </div>
</div>
</body>
</html>