%{--
    DataTable on empty DIV tag with data pulled with ajax call.
    Read only.
--}%
<g:set var="grailsDataTablesService" bean="grailsDataTablesService"/>
<div data-webui="ajax-table-loading-${uuid}" class="data-table-loading d-flex justify-content-center">
    <div class="spinner-border m-5" role="status">
%{--        <span class="visually-hidden">Loading...</span>--}%
    </div>
</div>
<div data-webui="ajax-table-${uuid}" style="display: none">
    <table id="webui-ajax-datatable-${uuid}-${id}" class="display">
        <thead>
        <tr>
            <g:each in="${columns}" var="columnName">
                <th>${columnName}</th>
            </g:each>
        </tr>
        </thead>
        <tfoot>
        <tr>
            <g:each in="${columns}" var="columnName">
                <th>${columnName}</th>
            </g:each>
        </tr>
        </tfoot>
    </table>

</div>

<script>
    window.addEventListener("load", function () {

            let x = $("div[data-webui|='ajax-table-${uuid}']")
            let y = $("div[data-webui|='ajax-table-loading-${uuid}']")

            if (y.length !== x.length)
                return

            for (let i = 0; i < x.length; i++) {
                let thisEntry = x[i]
                let table = thisEntry.getElementsByTagName("table")
                if (table != null) {
                    new DataTable(table, {
                        pageLength: ${lengthMenu?.get(0)},
                        select: ${selectable},
                        searching: ${searchable},
                        ordering: ${orderable},
                        paging: ${pageable}, <g:if test="${Objects.nonNull(scrollY)}">
                        scrollY: '${scrollY}', </g:if>
                        scrollX: ${scrollX},
                        scrollCollapse: ${scrollCollapse},
                        columns:
                            [
                                <g:each in="${ajaxColumnNames}" var="ajaxColumnName" status="i">
                                {
                                    data: "${grailsDataTablesService.gspBase64(ajaxColumnNames.get(i))}",
                                    width: '${columnRenderInfos?.get(ajaxColumnName)?.get("webUi_width") ?: 'inherit'}',
                                    height: '${columnRenderInfos?.get(ajaxColumnName)?.get("webUi_height") ?: 'inherit'}',
                                    color: '${columnRenderInfos?.get(ajaxColumnName)?.get("webUi_color") ?: 'inherit'}',
                                },

                                </g:each>
                            ],
                        columnDefs: [
                            <g:each in="${ajaxColumnNames}" var="ajaxColumnName" status="i">
                            {
                                orderable: ${columnRenderInfos?.get(ajaxColumnName)?.get("webUi_orderable") ?: 'false'},
                                targets: ${i},
                                width: '${columnRenderInfos?.get(ajaxColumnName)?.get("webUi_width") ?: 'inherit'}',
                                height: '${columnRenderInfos?.get(ajaxColumnName)?.get("webUi_height") ?: 'inherit'}',
                                color: '${columnRenderInfos?.get(ajaxColumnName)?.get("webUi_color") ?: 'inherit'}',
                                <g:if test="${columnRenderInfos?.get(ajaxColumnName)?.get("webUi_renderAs") == "GLINK"}">
                                render: function (data, type, row, meta) {
                                    if (type === "display") {
                                        data = "<a style='color: ${columnRenderInfos?.get(ajaxColumnName)?.get("webUi_color")}' href='${columnRenderInfos?.get(ajaxColumnName)?.get("webUi_link")}/?${ajaxColumnNames.get(i)}=" + encodeURIComponent(data) + "'>" + data + "</a>"
                                    }
                                    return data
                                },
                                </g:if>
                                <g:elseif test="${columnRenderInfos?.get(ajaxColumnName)?.get("webUi_renderAs") == "FORM_ACTION"}">
                                render: function (data, type, row, meta) {
                                    if (type === "display") {
                                        data = '${raw(g.render(
                                        template: "/GrailsDataTables/templates/DT_DTColumnRender_formActions",
                                        model: [__webUiCellField: columnRenderInfos?.get(ajaxColumnName)],
                                        encodeAs: "raw"
                                        )?.toString()?.replace('\n', '')?.replace('\r', '')?.replace('\r\n', ''))}'.replace("${columnRenderInfos?.get(ajaxColumnName)?.get("webUi_formUuid")}", encodeURIComponent(data))
                                    }
                                    return data
                                },
                                </g:elseif>
                            },
                            </g:each>
                        ],
                        ajax: {
                            url: "${ajaxUrl}",
                            type: "POST",
                            dataSrc: "data",
                        },
                        layout: {
                            topStart: {
                                pageLength: {
                                    menu: ${lengthMenu}
                                },
                                buttons: [
                                    <g:if test="${wantedExports?.size() >= 1}">
                                    {
                                        extend: 'collection',
                                        text: 'Export',
                                        buttons: [
                                            <g:if test="${wantedExports?.contains("copy")}">
                                            'copy',
                                            </g:if>
                                            <g:if test="${wantedExports?.contains("excel")}">
                                            'excel',
                                            </g:if>
                                            <g:if test="${wantedExports?.contains("csv")}">
                                            'csv',
                                            </g:if>
                                            <g:if test="${wantedExports?.contains("pdf")}">
                                            'pdf',
                                            </g:if>
                                            <g:if test="${wantedExports?.contains("print")}">
                                            'print',
                                            </g:if>
                                        ]
                                    }
                                    </g:if>
                                ]
                            }
                        },
                        initComplete: function (settings, json) {
                            $(y[i]).remove()
                            $(x[i]).css({'display': 'inherit'})
                        },
                        serverSide: true,
                        processing: true
                    })
                }

            }
    })

</script>