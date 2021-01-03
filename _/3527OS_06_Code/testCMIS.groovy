@Grab(group='org.apache.chemistry.opencmis', module='chemistry-opencmis-client-impl', version='0.10.0')
import org.apache.chemistry.opencmis.commons.enums.BindingType
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl
import org.apache.chemistry.opencmis.client.api.Document
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException

/////////////////////////////////////////////////////////////////////////
// Setup a Session
/////////////////////////////////////////////////////////////////////////

// Setup session parameters to connect with
def props = ['org.apache.chemistry.opencmis.user': 'admin',
        'org.apache.chemistry.opencmis.password': "admin",
        'org.apache.chemistry.opencmis.binding.atompub.url': "http://localhost:8080/alfresco/cmisatom",
        'org.apache.chemistry.opencmis.binding.spi.type': BindingType.ATOMPUB.value(),
        'org.apache.chemistry.opencmis.binding.compression': "true",
        'org.apache.chemistry.opencmis.cache.objects.ttl': "0"]

// Get all repositories from the server and then use the first one
def repositories = SessionFactoryImpl.newInstance().getRepositories(props)
def alfrescoRepository = repositories.get(0)

// List info for all repos, if there would be more than one we would see it now
repositories.eachWithIndex { repo, i ->
	println "Info about Alfresco repo # ${i} [ID=${repo.id}][name=${repo.name}][CMIS ver supported=${repo.cmisVersionSupported}]"
}

// Create a new session with the Alfresco repository
def session = alfrescoRepository.createSession()

/////////////////////////////////////////////////////////////////////////
// List the root folder
/////////////////////////////////////////////////////////////////////////

def root = session.rootFolder
def contentItems = root.children

contentItems.each { contentItem -> 
    if (contentItem instanceof Document) {
        def docContent = contentItem.contentStream
        println "${contentItem.name} [size=${docContent.length}][Mimetype=${docContent.mimeType}][type=${contentItem.type.displayName}]"
    } else {
        println "${contentItem.name} [type=${contentItem.type.displayName}]"
    }
}

/////////////////////////////////////////////////////////////////////////
// Creating a folder
/////////////////////////////////////////////////////////////////////////

def folderName = "GroovyStuff"
def someFolder = null

try {
    someFolder = session.getObjectByPath("/" + folderName)
} catch (CmisObjectNotFoundException nfe) {
    // Nothing to do, object does not exist
}

if (someFolder == null) {
    props = ['cmis:objectTypeId': 'cmis:folder',
             'cmis:name': 'GroovyStuff']
    someFolder = root.createFolder(props)

    println "Created new folder: " + someFolder.name + " [creator=" + someFolder.createdBy + "][created=" +
            someFolder.creationDate.time + "]"
} else {
    println "Folder already exist: " + folderName
}

/////////////////////////////////////////////////////////////////////////
// Creating a document with the CMIS helper script
/////////////////////////////////////////////////////////////////////////

// Load the CMIS Helper class
def cmis = new scripts.CMIS(session)

// Create doc
def groovyFolder = session.getObjectByPath '/GroovyStuff'
def file = new File('test.pdf')
def cmisArticleDoc = cmis.createDocumentFromFile groovyFolder, file, "cmis:document", null
cmis.printProperties cmisArticleDoc
