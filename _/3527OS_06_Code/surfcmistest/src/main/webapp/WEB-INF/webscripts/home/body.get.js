// Name in config for Alfresco server to connect to, defined in surf.xml
var SERVER_NAME = "default";

// The permanent connection to the Alfresco server will be known by this identifier
var CONNECTION_ID = "my-webapp-" + SERVER_NAME;

// Check if we already got a connection going (handled by the Spring CMIS extensions - Connection Manager)
var connection = cmis.getConnection(CONNECTION_ID);
if (connection == null) {
    // Get server definition, defined in surf.xml
    var serverDefinition = cmis.getServerDefinition(SERVER_NAME);
    if (serverDefinition == null) {
        status.code = 400;
        status.message = "Could not find server definition for server name " + SERVER_NAME + " - see surf.xml!";
        status.redirect = true;
    }

    // Connect to Alfresco, the Spring CMIS extension Connection Manager stores the connection for later retrieval
    try {
    	connection = cmis.createUserConnection(serverDefinition, CONNECTION_ID);
    	logger.warn("Connected with connection id: " + CONNECTION_ID);
    } catch (e) {
    	logger.warn((e.javaException == null ? e.rhinoException.message : e.javaException.message));
    }
} else {
    logger.warn("Already connected with connection id: " + CONNECTION_ID);
}

// Get the Repository Information
var repoInfo = connection.session.getRepositoryInfo();

model.productName = repoInfo.getProductName();
model.productVersion = repoInfo.getProductVersion();

// Get a Document object for the /Company Home/Page.txt file
var pageTextObject = connection.session.getObjectByPath("/Page.txt");

// Get Content Stream info object with information such as mimetype, content length, and access to input stream
var contentStreamInfo = pageTextObject.getContentStream();

// Get the text via input stream, passed into custom root object for conversion to text
model.pageText = cmisUtil.inputStream2Text(contentStreamInfo.getStream());
