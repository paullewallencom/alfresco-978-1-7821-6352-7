function getRepoId() {
    var serviceCallUrl = "http://localhost:8080/alfresco/cmisbrowser?callback=listRepoInfo";
    var listRepoInfoScript = document.createElement('script');
    listRepoInfoScript.setAttribute('src', serviceCallUrl);
    listRepoInfoScript.setAttribute('type', 'text/javascript');
    document.body.appendChild(listRepoInfoScript);
}

function listRepoInfo(cmisServiceResponseJSON) {
    for (repositoryId in cmisServiceResponseJSON) {
        var repository = cmisServiceResponseJSON[repositoryId];
        document.getElementById('repo_id').innerHTML = "Repository found [id=" + repository.repositoryId +
                            "][name="+ repository.vendorName + "[version=" + repository.productVersion + "]";
    }
}

