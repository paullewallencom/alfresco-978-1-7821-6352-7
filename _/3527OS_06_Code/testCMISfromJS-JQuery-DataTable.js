function getRepoId() {
    var useJSONP = true;
    $.ajax(
    {
        url: "http://localhost:8080/alfresco/cmisbrowser",
        data: null,
        dataType: (useJSONP ? "jsonp" : "json"),
        type: "GET",
        username: "admin",
        password: "admin",
        success: listRepoInfo,
        error: errorHandler,
        timeout: 5000
    }
    );
}

function listRepoInfo(cmisServiceResponseJSON) {
    for (repositoryId in cmisServiceResponseJSON) {
        var repository = cmisServiceResponseJSON[repositoryId];
        $('#repo_id').html("Repository found [id=" + repository.repositoryId +
                           "][name="+ repository.vendorName + "[version=" + repository.productVersion + "]");
        listTopFolderChildren(repository.repositoryId);
    }
}

function listTopFolderChildren(repositoryId) {
    // Get data from { "objects" : [
    //                   { "object" : { "properties" : { "cmis:name" : { "value" : "Data Dictionary",
    var listTopFolderChildrenUrl = "http://localhost:8080/alfresco/cmisbrowser/" + repositoryId + "/root";
    $("#folderContentDataTable").dataTable(
        {
            "bJQueryUI": true,
            "sAjaxSource": listTopFolderChildrenUrl,
            "sAjaxDataProp": "objects",
            "aoColumns": [
                    { "mData": "object.properties.cmis:name.value" },
                    { "mData": "object.properties.cmis:objectTypeId.value" },
                    { "mData": "object.properties.cmis:objectId.value" }
                ],
            "fnServerData": function ( sSource, aoData, fnCallback ) {
                $.ajax( {
                    url: sSource,
                    data: aoData,
                    dataType: "jsonp",
                    type: "GET",
                    username: "admin",
                    password: "admin",
                    success: fnCallback,
                    error: errorHandler,
                    timeout: 5000
                } );
            }
        }
        );
}

function errorHandler(event, jqXHR, settings, exception) {
    alert("CMIS Service call was aborted:" + jqXHR + " : " + event.statusText);
}
