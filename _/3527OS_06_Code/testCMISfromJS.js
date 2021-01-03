function getRepoId() {
    callCmisServer(
        "http://localhost:8080/alfresco/cmisbrowser",
        function (cmisServiceResponseJSON) {
			for (repositoryId in cmisServiceResponseJSON) {
				var repository = cmisServiceResponseJSON[repositoryId];
				document.getElementById('repo_id').innerHTML = "Repository found [id=" + repository.repositoryId +
				                    "][name="+ repository.vendorName + "[version=" + repository.productVersion + "]";
			}
        }
    );
}

function callCmisServer(cmisServiceUrl, callback) {
    var httpRequest = new XMLHttpRequest();
    var asynchronousRequest = true;
    var alfrescoUsername = "admin";
    var alfrescoPwd = "admin";
    httpRequest.callback = callback;
    httpRequest.open("GET", cmisServiceUrl, asynchronousRequest, alfrescoUsername, alfrescoPwd);
    httpRequest.onreadystatechange = handleCmisServiceCallResponse;
    httpRequest.send(null);
}

function handleCmisServiceCallResponse() {
    var responseContentFinishedLoading = 4;
    var responseStatusCodeOk = 200;
    if (this.readyState == responseContentFinishedLoading &&
            this.status == responseStatusCodeOk) {
        var cmisServiceResponseJSON = JSON.parse(this.responseText);
        this.callback(cmisServiceResponseJSON);
    } 
}
