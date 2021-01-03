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
    }
}

function errorHandler(event, jqXHR, settings, exception) {
    alert("CMIS Service call was aborted:" + jqXHR);
}