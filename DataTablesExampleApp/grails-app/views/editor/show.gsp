<%--
  Created by IntelliJ IDEA.
  User: jacob
  Date: 4/12/2025
  Time: 9:06 PM
--%>

<%@ page import="datatables.PhoneBookDisplay" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Show PhoneBook Entry</title>
    <meta name="layout" content="main"/>
    <asset:stylesheet src="DataTables/datatables.css"/>
    <asset:javascript src="DataTables/datatables.js"/>
</head>

<body>
    <div class="container">
        <div class="row">
            <div class="col-sm-12">
                <div style="background-color: grey; color: wheat; margin: 2%; padding: 2%">
                    <ul>
                        <li>Line Number: ${phoneBook?.lineNumber}</li>
                        <li>Persons Name: ${phoneBook?.personsName}</li>
                        <li>Phone Number: ${phoneBook?.phoneNumber}</li>
                        <li>Date: ${phoneBook?.phoneNumberDate}</li>
                        <li>Date Created: ${phoneBook?.dateCreated}</li>
                        <li>Last Activity: ${phoneBook?.activityTimeStamp}</li>
                        <li>Update Count: ${phoneBook?.version}</li>
                        <li><a style="color: black; text-decoration: underline" href="${createLink(controller: "editor")}">Go Back</a> </li>
                    </ul>
                    <div>
                        <div><h2>DataTableHtml - Simple annotated class</h2></div>
                        <GDTables:dataTableHtml forClass="${PhoneBookDisplay}" data="${phoneBookList}" id="single-phonebook" orderable="false" searchable="false" pageable="false" selectable="false"/>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
