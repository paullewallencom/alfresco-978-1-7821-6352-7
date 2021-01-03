package com.mycompany.app;

//import org.alfresco.cmis.client.AlfrescoDocument;

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.*;
import org.apache.chemistry.opencmis.commons.enums.*;
import org.apache.chemistry.opencmis.commons.exceptions.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cmis Client to demonstrate the OpenCMIS library
 *
 * @author martin.bergljung@ixxus.com
 * @version 1.0
 */
public class CmisClient {
    private static Log logger = LogFactory.getLog(CmisClient.class);

    // Map with all open connections, will only be one for now
    private static Map<String, Session> connections = new ConcurrentHashMap<String, Session>();

    public CmisClient() {

    }

    /**
     * Get an Open CMIS session to use when talking to the Alfresco repo.
     * Will check if there is already a connection to the Alfresco repo
     * and re-use that session.
     *
     * @param connectionName the name of the new connection to be created
     * @param username       the Alfresco username to connect with
     * @param pwd            the Alfresco password to connect with
     * @return an Open CMIS Session object
     */
    public Session getSession(String connectionName, String username, String pwd) {
        Session session = connections.get(connectionName);
        if (session == null) {
            logger.info("Not connected, creating new connection to Alfresco with the connection id ("
                    + connectionName + ")");

            // No connection to Alfresco available, create a new one
            SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put(SessionParameter.USER, username);
            parameters.put(SessionParameter.PASSWORD, pwd);
            parameters.put(SessionParameter.ATOMPUB_URL,
                    "http://localhost:8080/alfresco/api/-default-/cmis/versions/1.1/atom");
            // Use this URL for Alfresco versions older than 4.2.e Community and 4.2.0 Enterprise
            //parameters.put(SessionParameter.ATOMPUB_URL, "http://localhost:8080/alfresco/cmisatom");
            parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
            parameters.put(SessionParameter.COMPRESSION, "true");
            parameters.put(SessionParameter.CACHE_TTL_OBJECTS, "0");

            // If you are using an Alfresco versions older than 4.2.e Community and 4.2.0 Enterprise
            // then use this Alfresco specific factory for object creation
            // to get Alfresco specific document and folder classes that can be used to manage aspects
            //parameters.put(SessionParameter.OBJECT_FACTORY_CLASS,
            //      "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");

            // If there is only one repository exposed (e.g. Alfresco), these
            // lines will help detect it and its ID
            List<Repository> repositories = sessionFactory.getRepositories(parameters);
            Repository alfrescoRepository = null;
            if (repositories != null && repositories.size() > 0) {
                logger.info("Found (" + repositories.size() + ") Alfresco repositories");
                alfrescoRepository = repositories.get(0);
                logger.info("Info about the first Alfresco repo [ID=" + alfrescoRepository.getId() +
                        "][name=" + alfrescoRepository.getName() +
                        "][CMIS ver supported=" + alfrescoRepository.getCmisVersionSupported() + "]");
            } else {
                throw new CmisConnectionException(
                        "Could not connect to the Alfresco Server, no repository found!");
            }

            // Create a new session with the Alfresco repository
            session = alfrescoRepository.createSession();

            // Save connection for reuse
            connections.put(connectionName, session);
        } else {
            logger.info("Already connected to Alfresco with the connection id (" + connectionName + ")");
        }

        return session;
    }

    public void listRepoCapabilities(RepositoryInfo repositoryInfo) {
        RepositoryCapabilities repoCapabilities = repositoryInfo.getCapabilities();

        logger.info("aclCapability = " + repoCapabilities.getAclCapability().name());
        logger.info("changesCapability = " + repoCapabilities.getChangesCapability().name());
        logger.info("contentStreamUpdatable = " + repoCapabilities.getContentStreamUpdatesCapability().name());
        logger.info("joinCapability = " + repoCapabilities.getJoinCapability().name());
        logger.info("queryCapability = " + repoCapabilities.getQueryCapability().name());
        logger.info("renditionCapability = " + repoCapabilities.getRenditionsCapability().name());
        logger.info("allVersionsSearchable? = " + repoCapabilities.isAllVersionsSearchableSupported());
        logger.info("getDescendantSupported? = " + repoCapabilities.isGetDescendantsSupported());
        logger.info("getFolderTreeSupported? = " + repoCapabilities.isGetFolderTreeSupported());
        logger.info("multiFilingSupported? = " + repoCapabilities.isMultifilingSupported());
        logger.info("privateWorkingCopySearchable? = " + repoCapabilities.isPwcSearchableSupported());
        logger.info("pwcUpdateable? = " + repoCapabilities.isPwcUpdatableSupported());
        logger.info("unfilingSupported? = " + repoCapabilities.isUnfilingSupported());
        logger.info("versionSpecificFilingSupported? = " + repoCapabilities.isVersionSpecificFilingSupported());
    }

    public void listTopFolder(Session session) {
        Folder root = session.getRootFolder();
        ItemIterable<CmisObject> contentItems = root.getChildren();
        for (CmisObject contentItem : contentItems) {
            if (contentItem instanceof Document) {
                Document docMetadata = (Document) contentItem;
                ContentStream docContent = docMetadata.getContentStream();
                logger.info(docMetadata.getName() + " [size=" + docContent.getLength() + "][Mimetype=" +
                        docContent.getMimeType() + "][type=" + docMetadata.getType().getDisplayName() + "]");
            } else {
                logger.info(contentItem.getName() + " [type=" + contentItem.getType().getDisplayName() + "]");
            }
        }
    }

    public void listTopFolderWithPagingAndPropFilter(Session session) {
        Folder root = session.getRootFolder();

        OperationContext operationContext = new OperationContextImpl();

        // Setup paging
        int maxItemsPerPage = 5;
        operationContext.setMaxItemsPerPage(maxItemsPerPage);

        // Setup property filter
        Set<String> propertyFilter = new HashSet<String>();
        propertyFilter.add(PropertyIds.CREATED_BY);
        propertyFilter.add(PropertyIds.NAME);
        operationContext.setFilter(propertyFilter);

        // Get all child nodes under the top /Company Home folder
        ItemIterable<CmisObject> contentItems = root.getChildren(operationContext);
        long numerOfPages = Math.abs(contentItems.getTotalNumItems() / maxItemsPerPage);

        // Loop through each page of 5 content items
        int pageNumber = 1;
        boolean finishedPaging = false;
        int count = 0;
        while (!finishedPaging) {
            logger.info("Page " + pageNumber + " (" + numerOfPages + ")");
            ItemIterable<CmisObject> currentPage = contentItems.skipTo(count).getPage();
            for (CmisObject contentItem : currentPage) {
                logger.info(contentItem.getName() + " [type=" + contentItem.getType().getDisplayName() + "]");
                listProperties(contentItem);

                if (ObjectType.FOLDER_BASETYPE_ID.equals(contentItem.getBaseType().getId())) {
                    logger.info("Base type is Folder");
                } else if (ObjectType.DOCUMENT_BASETYPE_ID.equals(contentItem.getBaseType().getId())) {
                    logger.info("Base type is Document");
                }

                count++;
            }

            pageNumber++;

            if (!currentPage.getHasMoreItems()) {
                finishedPaging = true;
            }
        }
    }

    public void listTypesAndSubtypes(Session session) {
        boolean includePropertyDefinitions = false;
        List<Tree<ObjectType>> typeTrees = session.getTypeDescendants(null, -1, includePropertyDefinitions);
        for (Tree<ObjectType> typeTree : typeTrees) {
            logTypes(typeTree, "");
        }
    }

    public Folder createFolder(Session session) {
        String folderName = "OpenCMISTest";
        Folder parentFolder = session.getRootFolder();

        // Make sure the user is allowed to create a folder under the root folder
        if (parentFolder.getAllowableActions().getAllowableActions().contains(Action.CAN_CREATE_FOLDER) == false) {
            throw new CmisUnauthorizedException("Current user does not have permission to create a sub-folder in " +
                    parentFolder.getPath());
        }

        // Check if folder already exist, if not create it
        Folder newFolder = (Folder) getObject(session, parentFolder, folderName);
        if (newFolder == null) {
            Map<String, Object> newFolderProps = new HashMap<String, Object>();
            newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
            newFolderProps.put(PropertyIds.NAME, folderName);
            newFolder = parentFolder.createFolder(newFolderProps);

            logger.info("Created new folder: " + newFolder.getPath() +
                    " [creator=" + newFolder.getCreatedBy() + "][created=" +
                    date2String(newFolder.getCreationDate().getTime()) + "]");
        } else {
            logger.info("Folder already exist: " + newFolder.getPath());
        }

        return newFolder;
    }

    public Folder createFolderWithCustomType(Session session) {
        String folderName = "OpenCMISTest2";
        Folder parentFolder = session.getRootFolder();

        // Check if folder already exist, if not create it
        Folder newFolder = (Folder) getObject(session, parentFolder, folderName);
        if (newFolder == null) {
            Map<String, Object> newFolderProps = new HashMap<String, Object>();
            newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, "F:myc:project");
            newFolderProps.put(PropertyIds.NAME, folderName);
            newFolderProps.put("myc:projectCode", "PROJ001");
            newFolder = parentFolder.createFolder(newFolderProps);

            logger.info("Created new folder: " + newFolder.getPath() +
                    " [creator=" + newFolder.getCreatedBy() + "][created=" +
                    date2String(newFolder.getCreationDate().getTime()) + "]");
        } else {
            logger.info("Folder already exist: " + newFolder.getPath());
        }

        return newFolder;
    }

    public Document createDocument(Session session, Folder parentFolder) throws IOException {
        String documentName = "OpenCMISTest.txt";

        // Make sure the user is allowed to create a document in the passed in folder
        if (parentFolder.getAllowableActions().getAllowableActions().contains(Action.CAN_CREATE_DOCUMENT) == false) {
            throw new CmisUnauthorizedException("Current user does not have permission to create a document in " +
                    parentFolder.getPath());
        }

        // Check if document already exist, if not create it
        Document newDocument = (Document) getObject(session, parentFolder, documentName);
        if (newDocument == null) {
            // Setup document metadata
            Map<String, Object> newDocumentProps = new HashMap<String, Object>();
            String typeId = "cmis:document";
            newDocumentProps.put(PropertyIds.OBJECT_TYPE_ID, typeId);
            newDocumentProps.put(PropertyIds.NAME, documentName);

            // Setup document content
            String mimetype = "text/plain; charset=UTF-8";
            String documentText = "This is a test document!";
            byte[] bytes = documentText.getBytes("UTF-8");
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            ContentStream contentStream = session.getObjectFactory().createContentStream(
                    documentName, bytes.length, mimetype, input);

            // Check if we need versioning
            VersioningState versioningState = VersioningState.NONE;
            DocumentType docType = (DocumentType) session.getTypeDefinition(typeId);
            if (Boolean.TRUE.equals(docType.isVersionable())) {
                logger.info("Document type " + typeId + " is versionable, setting MAJOR version state.");
                versioningState = VersioningState.MAJOR;
            }

            // Create versioned document object
            newDocument = parentFolder.createDocument(newDocumentProps, contentStream, versioningState);

            logger.info("Created new document: " + getDocumentPath(newDocument) +
                    " [version=" + newDocument.getVersionLabel() + "][creator=" + newDocument.getCreatedBy() +
                    "][created=" + date2String(newDocument.getCreationDate().getTime()) + "]");
        } else {
            logger.info("Document already exist: " + getDocumentPath(newDocument));
        }

        return newDocument;
    }

    public Document createDocumentFromFileWithCustomType(Session session) {
        String documentName = "OpenCMISTest2.pdf";
        File file = new File("Some.pdf");
        Folder parentFolder = session.getRootFolder();

        // Check if document already exist, if not create it
        Document newDocument = (Document) getObject(session, parentFolder, documentName);
        if (newDocument == null) {
            // Setup document metadata
            Map<String, Object> newDocumentProps = new HashMap<String, Object>();
            newDocumentProps.put(PropertyIds.OBJECT_TYPE_ID, "D:myc:itDoc");
            newDocumentProps.put(PropertyIds.NAME, documentName);

            InputStream is = null;
            try {
                // Setup document content
                is = new FileInputStream(file);
                String mimetype = "application/pdf";
                ContentStream contentStream = session.getObjectFactory().createContentStream(
                        documentName, file.length(), mimetype, is);

                // Create versioned document object
                newDocument = parentFolder.createDocument(newDocumentProps, contentStream, VersioningState.MAJOR);
                logger.info("Created new document: " + getDocumentPath(newDocument) +
                    " [version=" + newDocument.getVersionLabel() + "][creator=" + newDocument.getCreatedBy() +
                    "][created=" + date2String(newDocument.getCreationDate().getTime()) + "]");

                // Close the stream to handle any IO Exception
                is.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                IOUtils.closeQuietly(is);
            }
        } else {
            logger.info("Document already exist: " + getDocumentPath(newDocument));
        }

        return newDocument;
    }

    public Folder updateFolder(Folder folder) {
        String newFolderName = "OpenCMISTest_Updated";
        Folder updatedFolder = null;

        // If we got a folder update the name of it
        if (folder != null) {
            // Make sure the user is allowed to update folder properties
            if (folder.getAllowableActions().getAllowableActions().contains(Action.CAN_UPDATE_PROPERTIES) == false) {
                throw new CmisUnauthorizedException(
                    "Current user does not have permission to update folder properties for " +
                    folder.getPath());
            }

            // Update the folder with a new name
            String oldName = folder.getName();
            Map<String, Object> newFolderProps = new HashMap<String, Object>();
            newFolderProps.put(PropertyIds.NAME, newFolderName);
            updatedFolder = (Folder) folder.updateProperties(newFolderProps);

            logger.info("Updated " + oldName + " with new name: " + updatedFolder.getPath() +
                    " [creator=" + updatedFolder.getCreatedBy() + "][created=" +
                    date2String(updatedFolder.getCreationDate().getTime()) + "][modifier=" +
                    updatedFolder.getLastModifiedBy() + "][modified=" +
                    date2String(updatedFolder.getLastModificationDate().getTime()) + "]");
        } else {
            logger.error("Folder to update is null!");
        }

        return updatedFolder;
    }

    public Document updateDocument(Session session, Document document) throws IOException {
        RepositoryInfo repoInfo = session.getRepositoryInfo();
        if (!repoInfo.getCapabilities().getContentStreamUpdatesCapability()
                .equals(CapabilityContentStreamUpdates.ANYTIME)) {
            logger.warn("Updating content stream without a checkout is not supported by this repository [repoName=" +
                    repoInfo.getProductName() + "][repoVersion=" + repoInfo.getProductVersion() + "]");
            return document;
        }

        // Make sure we got a document, then update it
        Document updatedDocument = null;
        if (document != null) {
            // Make sure the user is allowed to update the content for this document
            if (document.getAllowableActions().getAllowableActions().contains(Action.CAN_SET_CONTENT_STREAM) == false) {
                throw new CmisUnauthorizedException("Current user does not have permission to set/update content stream for " +
                        getDocumentPath(document));
            }

            // Setup new document content
            String newDocumentText = "This is a test document that has been updated with new content!";
            String mimetype = "text/plain; charset=UTF-8";
            byte[] bytes = newDocumentText.getBytes("UTF-8");
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            ContentStream contentStream = session.getObjectFactory().createContentStream(
                    document.getName(), bytes.length, mimetype, input);
            boolean overwriteContent = true;
            updatedDocument = document.setContentStream(contentStream, overwriteContent);
            if (updatedDocument == null) {
                logger.info("No new version was created when content stream was updated for " + getDocumentPath(document));
                updatedDocument = document;
            }

            logger.info("Updated content for document: " + getDocumentPath(updatedDocument) +
                    " [version=" + updatedDocument.getVersionLabel() + "][modifier=" + updatedDocument.getLastModifiedBy() +
                    "][modified=" + date2String(updatedDocument.getLastModificationDate().getTime()) + "]");
        } else {
            logger.info("Document is null, cannot update it!");
        }

        return updatedDocument;
    }

    public void deleteDocument(Document document) {
        // If we got a document try and delete it
        if (document != null) {
            // Make sure the user is allowed to delete the document
            if (document.getAllowableActions().getAllowableActions().contains(Action.CAN_DELETE_OBJECT) == false) {
                throw new CmisUnauthorizedException("Current user does not have permission to delete document " +
                        document.getName() + " with Object ID " + document.getId());
            }

            String docPath = getDocumentPath(document);
            boolean deleteAllVersions = true;
            document.delete(deleteAllVersions);
            logger.info("Deleted document: " + docPath);
        } else {
            logger.info("Cannot delete document as it is null!");
        }
    }

    public void deleteFolder(Folder folder) {
        // If we got a folder then delete
        if (folder != null) {
            // Make sure the user is allowed to delete the folder
            if (folder.getAllowableActions().getAllowableActions().contains(Action.CAN_DELETE_OBJECT) == false) {
                throw new CmisUnauthorizedException("Current user does not have permission to delete folder " +
                        folder.getPath());
            }

            String folderPath = folder.getPath();
            folder.delete();
            logger.info("Deleted folder: " + folderPath);
        } else {
            logger.info("Cannot delete folder that is null");
        }
    }

    public void deleteFolderTree(Session session) {
        UnfileObject unfileMode = UnfileObject.UNFILE;
        RepositoryInfo repoInfo = session.getRepositoryInfo();
        if (!repoInfo.getCapabilities().isUnfilingSupported()) {
            logger.warn("The repository does not support unfiling a document from a folder, documents will " +
                    "be deleted completely from all associated folders [repoName=" +
                    repoInfo.getProductName() + "][repoVersion=" + repoInfo.getProductVersion() + "]");
            unfileMode = UnfileObject.DELETE;
        }

        String folderName = "OpenCMISTestWithContent";
        Folder parentFolder = session.getRootFolder();

        // Check if folder exist, if not don't try and delete it
        Folder someFolder = (Folder) getObject(session, parentFolder, folderName);
        if (someFolder != null) {
            // Make sure the user is allowed to delete the folder
            if (someFolder.getAllowableActions().getAllowableActions().contains(Action.CAN_DELETE_TREE) == false) {
                throw new CmisUnauthorizedException("Current user does not have permission to delete folder tree" +
                        parentFolder.getPath());
            }

            boolean deleteAllVersions = true;
            boolean continueOnFailure = true;
            List<String> failedObjectIds = someFolder.deleteTree(deleteAllVersions, unfileMode, continueOnFailure);
            logger.info("Deleted folder and all its content: " + someFolder.getName());
            if (failedObjectIds != null && failedObjectIds.size() > 1) {
                for (String failedObjectId : failedObjectIds) {
                    logger.info("Could not delete Alfresco node with Node Ref: " + failedObjectId);
                }
            }
        } else {
            logger.info("Did not delete folder as it does not exist: " + parentFolder.getPath() + folderName);
        }
    }

    public void getContentForDocumentAndStoreInFile(Session session) {
        // This is one of the out-of-the-box email templates in Alfresco
        String documentPath = "/Data Dictionary/Email Templates/invite/invite-email.html.ftl";

        // Get the document object by path so we can get to the content stream
        Document templateDocument = (Document) session.getObjectByPath(documentPath);
        if (templateDocument != null) {
            // Make sure the user is allowed to get the content stream (bytes) for the document
            if (templateDocument.getAllowableActions().getAllowableActions().contains(Action.CAN_GET_CONTENT_STREAM) == false) {
                throw new CmisUnauthorizedException(
                        "Current user does not have permission to get the content stream for " + documentPath);
            }

            File file = null;
            InputStream input = null;
            OutputStream output = null;

            try {
                // Create the file on the local drive without any content
                file = new File(templateDocument.getName());
                if (!file.exists()) {
                    file.createNewFile();
                }

                // Get the object content stream and write to the new local file
                input = templateDocument.getContentStream().getStream();
                output = new FileOutputStream(file);
                IOUtils.copy(input, output);

                // Close streams and handle exceptions
                input.close();
                output.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                IOUtils.closeQuietly(output);
                IOUtils.closeQuietly(input);
            }

            logger.info("Created a new file " + file.getAbsolutePath() + " with content from document: " + documentPath);
        } else {
            logger.error("Template document could not be found: " + documentPath);
        }
    }

    public void copyDocument(Session session, Document document) {
        Folder parentFolder = session.getRootFolder();
        String destinationFolderName = "Guest Home";
        Folder destinationFolder = (Folder) getObject(session, parentFolder, destinationFolderName);

        if (destinationFolder == null) {
            logger.error("Cannot copy " + document.getName() + ", could not find folder with the name " +
                    destinationFolderName + ", are you using Alfresco?");
            return;
        }

        // Check that we got the document, then copy
        if (document != null) {
            try {
                document.copy(destinationFolder);
                logger.info("Copied document " + document.getName() + " from folder " + parentFolder.getPath() +
                        " to folder " + destinationFolder.getPath());
            } catch (CmisContentAlreadyExistsException e) {
                logger.error("Cannot copy document " + document.getName() +
                        ", already exist in to folder " + destinationFolder.getPath());
            }
        } else {
            logger.error("Document is null, cannot copy to " + destinationFolder.getPath());
        }
    }

    public void moveDocument(Session session, Document document) {
        Folder parentFolder = session.getRootFolder();
        Folder sourceFolder = getDocumentParentFolder(document);
        String destinationFolderName = "User Homes";
        Folder destinationFolder = (Folder) getObject(session, parentFolder, destinationFolderName);

        // Check that we got the document, then move
        if (document != null) {
            // Make sure the user is allowed to move the document to a new folder
            if (document.getAllowableActions().getAllowableActions().contains(Action.CAN_MOVE_OBJECT) == false) {
                throw new CmisUnauthorizedException("Current user does not have permission to move " +
                        getDocumentPath(document) + document.getName());
            }

            String pathBeforeMove = getDocumentPath(document);
            try {
                document.move(sourceFolder, destinationFolder);
                logger.info("Moved document " + pathBeforeMove + " to folder " +
                        destinationFolder.getPath());
            } catch (CmisRuntimeException e) {
                logger.error("Cannot move document to folder " + destinationFolder.getPath() + ": " + e.getMessage());
            }
        } else {
            logger.error("Document is null, cannot move!");
        }
    }

    private static final String SECONDARY_OBJECT_TYPE_IDS_PROP_NAME = "cmis:secondaryObjectTypeIds";

    public void createFolderWithTitledAspect(Session session) {
        String folderName = "OpenCMISTestTitled";
        Folder parentFolder = session.getRootFolder();

        // Check if folder already exist, if not create it
        Folder newFolder = (Folder) getObject(session, parentFolder, folderName);
        if (newFolder == null) {
            List<Object> aspects = new ArrayList<Object>();
            aspects.add("P:cm:titled");
            Map<String, Object> newFolderProps = new HashMap<String, Object>();
            newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
            newFolderProps.put(PropertyIds.NAME, folderName);
            newFolderProps.put(SECONDARY_OBJECT_TYPE_IDS_PROP_NAME, aspects);
            newFolderProps.put("cm:title", "Folder Title");
            newFolderProps.put("cm:description", "Folder Description");
            newFolder = parentFolder.createFolder(newFolderProps);

            logger.info("Created new folder with Titled aspect: " + newFolder.getPath() +
                    " [creator=" + newFolder.getCreatedBy() + "][created=" +
                    date2String(newFolder.getCreationDate().getTime()) + "]");
        } else {
            logger.info("Cannot create folder, it already exist: " + newFolder.getPath());
        }
    }

    /*
    public void createFolderWithTitledAspectWithAlfrescoExtension(Session session) {
        String folderName = "OpenCMISTestTitled";
        Folder parentFolder = session.getRootFolder();

        // Check if folder already exist, if not create it
        Folder newFolder = (Folder) getObject(session, parentFolder, folderName);
        if (newFolder == null) {
            Map<String, Object> newFolderProps = new HashMap<String, Object>();
            newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder,P:cm:titled");
            newFolderProps.put(PropertyIds.NAME, folderName);
            newFolderProps.put("cm:title", "Folder Title");
            newFolderProps.put("cm:description", "Folder Description");
            newFolder = parentFolder.createFolder(newFolderProps);

            logger.info("Created new folder with Titled aspect: " + newFolder.getName() +
                    " [creator=" + newFolder.getCreatedBy() + "][created=" +
                    date2String(newFolder.getCreationDate().getTime()) + "]");
        } else {
            logger.info("Folder already exist: " + newFolder.getPath());
        }
    }
    */

    public void addAspectToExistingDocument(Document document) {
        String aspectName = "P:cm:effectivity";
        // Make sure we got a document, and then add the aspect to it
        if (document != null) {
            // Check that document don't already got the aspect applied
            List<Object> aspects = document.getProperty(SECONDARY_OBJECT_TYPE_IDS_PROP_NAME).getValues();
            if (!aspects.contains(aspectName)) {
                aspects.add(aspectName);
                Map<String, Object> properties = new HashMap<String, Object>();
                properties.put(SECONDARY_OBJECT_TYPE_IDS_PROP_NAME, aspects);
                properties.put("cm:from", new Date());
                Calendar toDate = Calendar.getInstance();
                toDate.add(Calendar.MONTH, 2);
                properties.put("cm:to", toDate.getTime());
                Document updatedDocument = (Document) document.updateProperties(properties);
                logger.info("Added aspect " + aspectName + " to " + getDocumentPath(updatedDocument));
            } else {
                logger.info("Aspect " + aspectName + " is already applied to " + getDocumentPath(document));
            }
        } else {
            logger.error("Document is null, cannot add aspect to it!");
        }
    }

    /*
   public void addAspectToExistingDocumentWithAlfrescoExtension(Session session) {
       String documentName = "OpenCMISTest2.pdf";
       String aspectName = "P:cm:effectivity";
       Folder parentFolder = session.getRootFolder();

       // Make sure document exists and get the object for it, if not don't try and add the aspect
       AlfrescoDocument someDocument = (AlfrescoDocument) getObject(session, parentFolder, documentName);
       if (someDocument != null) {
           // Check that document don't already got the aspect applied
           if (!someDocument.hasAspect(aspectName)) {
               Map<String, Object> aspectProperties = new HashMap<String, Object>();
               aspectProperties.put("cm:from", new Date());
               Calendar toDate = Calendar.getInstance();
               toDate.add(Calendar.MONTH, 2);
               aspectProperties.put("cm:to", toDate.getTime());
               someDocument.addAspect(aspectName, aspectProperties);

               logger.info("Added aspect " + aspectName + " to " + getDocumentPath(someDocument));
           } else {
               logger.info("Aspect " + aspectName + " is already applied to " + getDocumentPath(someDocument));
           }
       } else {
           logger.info("Document does not exist, cannot add aspect to it: " + parentFolder.getPath() + documentName);
       }
   }  */

    public void readAspectsForExistingDocument(Document document) {
        // Make sure we got a document, then list aspects
        if (document != null) {
            List<SecondaryType> aspects = document.getSecondaryTypes();
            logger.info("Aspects for: " + getDocumentPath(document));
            for (SecondaryType aspect : aspects) {
                logger.info("    " + aspect.getDisplayName() + " (" + aspect.getId() + ")");
            }
        } else {
            logger.error("Document is null, cannot list aspects for it!");
        }
    }

    /*
    public void readAspectsForExistingDocumentWithAlfrescoExtension(Session session) {
        String documentName = "OpenCMISTest2.pdf";
        Folder parentFolder = session.getRootFolder();

        // Make sure document exists and get the object for it, if not don't try and list aspects
        AlfrescoDocument someDocument = (AlfrescoDocument) getObject(session, parentFolder, documentName);
        if (someDocument != null) {
            Collection<ObjectType> aspects = someDocument.getAspects();
            logger.info("Aspects for: " + getDocumentPath(someDocument);
            for (ObjectType aspect : aspects) {
                logger.info("    " + aspect.getDisplayName() + " (" + aspect.getId() + ")");
            }
        } else {
            logger.info("Document does not exist, cannot list aspects for it: " +
                    parentFolder.getPath() + documentName);
        }
    } */

    public Document checkOutDocument(Session session, Document document) {
        // Check that we got the document before we try and do a check-out
        Document workingCopy = null;
        if (document != null) {
            // If it is already checked out cancel that checkout
            if (document.isVersionSeriesCheckedOut()) {
                document.cancelCheckOut();
                logger.info("Document was already checked out, cancelled check out for document: " +
                        getDocumentPath(document));
            }

            ObjectId workingCopyId = document.checkOut();
            workingCopy = (Document) session.getObject(workingCopyId);

            logger.info("Checked Out document: " + getDocumentPath(document) +
                    " [version=" + document.getVersionLabel() + "][pwcName=" + workingCopy.getName() + "]");
        } else {
            logger.error("Document is null, cannot check-out!");
        }

        return workingCopy;
    }

    public void updateContentAndCheckInDocument(Session session, Document pwc) {
        String documentName = "OpenCMISTest2.pdf";
        File file = new File("UpdatedContent.pdf");

        InputStream is = null;
        ObjectId newObjectId = null;
        try {
            // Setup updated document content
            is = new FileInputStream(file);
            String mimetype = "application/pdf";
            ContentStream contentStream = session.getObjectFactory().createContentStream(
                    documentName, file.length(), mimetype, is);

            // Check in the Private Working Copy (pwc) with new content
            boolean majorVersion = false;
            Map<String, Object> props = null;
            String checkInComment = "This is just a minor update";
            newObjectId = pwc.checkIn(majorVersion, props, contentStream, checkInComment);

            // Close stream and handle exceptions
            is.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            IOUtils.closeQuietly(is);
        }

        // Get the document so we can check the new version
        Document updatedDocument = (Document) session.getObject(newObjectId);
        logger.info("Checked In document: " + getDocumentPath(updatedDocument) +
                " [newVersion=" + updatedDocument.getVersionLabel() +
                "][checkInComment=" + updatedDocument.getCheckinComment() + "]");
    }

    public void addPermissionToFolder(Session session, Folder folder) {
        // Check if the repo supports ACLs
        RepositoryInfo repoInfo = session.getRepositoryInfo();
        if (!repoInfo.getCapabilities().getAclCapability().equals(CapabilityAcl.MANAGE)) {
            logger.warn("Repository does not allow ACL management [repoName=" + repoInfo.getProductName() +
                    "][repoVersion=" + repoInfo.getProductVersion() + "]");
        } else {
            // Check that we got the folder, if not don't assign new permission to it
            if (folder != null) {
                List<String> permissions = new ArrayList<String>();
                permissions.add("{http://www.alfresco.org/model/content/1.0}folder.Collaborator");
                String principal = "GROUP_MARKETING";
                Ace aceIn = session.getObjectFactory().createAce(principal, permissions);
                List<Ace> aceListIn = new ArrayList<Ace>();
                aceListIn.add(aceIn);
                folder.addAcl(aceListIn, AclPropagation.REPOSITORYDETERMINED);

                logger.info("ACL for " + folder.getPath() + " after adding an ACE:");
                OperationContextImpl operationContext = new OperationContextImpl();
                operationContext.setIncludeAcls(true);
                folder = (Folder) session.getObject(folder, operationContext);
                for (Ace ace : folder.getAcl().getAces()) {
                    System.out.println("    " + ace.getPrincipalId() + " " + ace.toString());
                }
            } else {
                logger.error("Folder is null, cannot add permission!");
            }
        }
    }

    public void addCheckOutPermissionsToUser(Session session, Document document) {
        String principal = "mjackson";
        Folder parentFolder = session.getRootFolder();

        // Make sure we got a document, if not don't try and add permission
        if (document != null) {
            RepositoryInfo repositoryInfo = session.getRepositoryInfo();
            AclCapabilities aclCapabilities = repositoryInfo.getAclCapabilities();
            Map<String, PermissionMapping> permissionMappings = aclCapabilities.getPermissionMapping();
            PermissionMapping permissionMapping = permissionMappings.get(PermissionMapping.CAN_CHECKOUT_DOCUMENT);
            List<String> permissions = permissionMapping.getPermissions();
            Ace addAce = session.getObjectFactory().createAce(principal, permissions);
            List<Ace> addAces = new LinkedList<Ace>();
            addAces.add(addAce);
            document.addAcl(addAces, AclPropagation.REPOSITORYDETERMINED);

            logger.info("Added check-out permissions for user " + principal + " to " + getDocumentPath(document));
        } else {
            logger.error("Document is null, cannot add permission!");
        }
    }

    public void setupRelationshipBetween2Folders(Session session) {
        // First check that relationship types are supported and
        // that the custom Alfresco relationship/assocation is supported
        String cmisRelationshipTypeName = "cmis:relationship";
        String customCopiedFromAssociation = "R:cm:original";

        try {
            session.getTypeDefinition(cmisRelationshipTypeName);
        } catch (CmisObjectNotFoundException e) {
            logger.warn("Repository does not support " + cmisRelationshipTypeName + " objects");
        }

        try {
            session.getTypeDefinition(customCopiedFromAssociation);
        } catch (CmisObjectNotFoundException e) {
            logger.warn("Repository does not support " + customCopiedFromAssociation + " objects");
            return;
        }

        String oldFolderBeingReplacedName = "OpenCMISTest2";
        String newFolderName = "OpenCMISTestTitled";
        Folder parentFolder = session.getRootFolder();

        // Get the folder objects and check that the folders exists
        Folder sourceFolder = (Folder) getObject(session, parentFolder, newFolderName);
        Folder targetFolder = (Folder) getObject(session, parentFolder, oldFolderBeingReplacedName);
        if (sourceFolder == null || targetFolder == null) {
            logger.warn("Cannot setup relationship as at least one of the folders does not exist");
        }

        // Check that relationship does not already exist
        OperationContextImpl operationContext = new OperationContextImpl();
        operationContext.setIncludeRelationships(IncludeRelationships.SOURCE);
        sourceFolder = (Folder) session.getObject(sourceFolder, operationContext);
        List<Relationship> existingRelationships = sourceFolder.getRelationships();
        if (existingRelationships != null) {
            for (Relationship existingRelationship : existingRelationships) {
                logger.warn("Relationship: " + existingRelationship.toString());
                if (existingRelationship.getType().getId().equalsIgnoreCase(customCopiedFromAssociation)) {
                    logger.warn("Folders are already setup with relationship: " + customCopiedFromAssociation);
                }
                return;
            }
        }

        // Setup copiedFrom relationship between folders
        Map<String, String> relationshipProperties = new HashMap<String, String>();
        relationshipProperties.put("cmis:objectTypeId", customCopiedFromAssociation);
        relationshipProperties.put("cmis:sourceId", sourceFolder.getId());
        relationshipProperties.put("cmis:targetId", targetFolder.getId());
        session.createRelationship(relationshipProperties, null, null, null);

        logger.info("Setup " + customCopiedFromAssociation + " relationship between folder " +
                sourceFolder.getPath() + " and folder " + targetFolder.getPath());
    }

    public void searchMetadataAndFTS(Session session) {
        // Check if the repo supports Metadata search and Full Text Search (FTS)
        RepositoryInfo repoInfo = session.getRepositoryInfo();
        if (repoInfo.getCapabilities().getQueryCapability().equals(CapabilityQuery.METADATAONLY)) {
            logger.warn("Repository does not support FTS [repoName=" + repoInfo.getProductName() +
                    "][repoVersion=" + repoInfo.getProductVersion() + "]");
        } else {
            String query = "SELECT * FROM cmis:document WHERE cmis:name LIKE 'OpenCMIS%'";
            ItemIterable<QueryResult> searchResult = session.query(query, false);
            logSearchResult(query, searchResult);

            query = "SELECT * FROM cmis:document WHERE cmis:name LIKE 'OpenCMIS%' AND CONTAINS('testing')";
            searchResult = session.query(query, false);
            logSearchResult(query, searchResult);
        }
    }

    private void logSearchResult(String query, ItemIterable<QueryResult> searchResult) {
        logger.info("Results from query " + query);
        int i = 1;
        for (QueryResult resultRow : searchResult) {
            logger.info("--------------------------------------------\n" + i + " , "
                    + resultRow.getPropertyByQueryName("cmis:objectId").getFirstValue() + " , "
                    + resultRow.getPropertyByQueryName("cmis:objectTypeId").getFirstValue() + " , "
                    + resultRow.getPropertyByQueryName("cmis:name").getFirstValue());
            i++;
        }
    }

    public void copyFolder(Folder destinationFolder, Folder toCopyFolder) {
        Map<String, Object> folderProperties = new HashMap<String, Object>();
        folderProperties.put(PropertyIds.NAME, toCopyFolder.getName());
        folderProperties.put(PropertyIds.OBJECT_TYPE_ID, toCopyFolder.getBaseTypeId().value());
        Folder newFolder = destinationFolder.createFolder(folderProperties);
        copyChildren(newFolder, toCopyFolder);
    }

    public void copyChildren(Folder destinationFolder, Folder toCopyFolder) {
        ItemIterable<CmisObject> immediateChildren = toCopyFolder.getChildren();
        for (CmisObject child : immediateChildren) {
            if (child instanceof Document) {
                ((Document) child).copy(destinationFolder);
            } else if (child instanceof Folder) {
                copyFolder(destinationFolder, (Folder) child);
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    //////////////////////////////////////////////////////////////////////////////////////////////////

    private void listProperties(CmisObject cmisObject) {
        for (Property<?> p : cmisObject.getProperties()) {
            if (PropertyType.DATETIME == p.getType()) {
                Calendar calValue = (Calendar) p.getValue();
                logger.info("  - " + p.getId() + " = " + (calValue != null ? date2String(calValue.getTime()) : ""));
            } else {
                logger.info("  - " + p.getId() + " = " + p.getValue());
            }
        }
    }

    private void logTypes(Tree<ObjectType> typeTree, String tab) {
        ObjectType objType = typeTree.getItem();

        String docInfo = "";
        if (objType instanceof DocumentType) {
            DocumentType docType = (DocumentType) objType;
            docInfo = "[versionable=" + docType.isVersionable() + "][content=" + docType.getContentStreamAllowed() + "]";
        }

        logger.info(tab + objType.getDisplayName() + " [id=" + objType.getId() +
                "][fileable=" + objType.isFileable() + "][queryable=" + objType.isQueryable() + "]" + docInfo);

        for (Tree<ObjectType> subTypeTree : typeTree.getChildren()) {
            logTypes(subTypeTree, tab + " ");
        }
    }

    /**
     * Get a CMIS Object by name from a specified folder.
     *
     * @param parentFolder  the parent folder where the object might exist
     * @param objectName the name of the object that we are looking for
     * @return the Cmis Object if it existed, otherwise null
     */
    private CmisObject getObject(Session session, Folder parentFolder, String objectName) {
        CmisObject object = null;

        try {
            String path2Object = parentFolder.getPath();
            if (!path2Object.endsWith("/")) {
                path2Object += "/";
            }
            path2Object += objectName;
            object = session.getObjectByPath(path2Object);
        } catch (CmisObjectNotFoundException nfe0) {
            // Nothing to do, object does not exist
        }

        return object;
    }

    /**
     * Get the absolute path to the passed in Document object.
     * Called the primary folder path in the Alfresco world as most documents only have one parent folder.
     *
     * @param document the Document object to get the path for
     * @return the path to the passed in Document object, or "Un-filed/{object name}" if it does not have a parent folder
     */
    private String getDocumentPath(Document document) {
        String path2Doc = getParentFolderPath(document);
        if (!path2Doc.endsWith("/")) {
            path2Doc += "/";
        }
        path2Doc += document.getName();
        return path2Doc;
    }

    /**
     * Get the parent folder path for passed in Document object
     *
     * @param document the document object to get the path for
     * @return the parent folder path, or "Un-filed" if the document is un-filed and does not have a parent folder
     */
    private String getParentFolderPath(Document document) {
        Folder parentFolder = getDocumentParentFolder(document);
        return parentFolder == null ? "Un-filed" : parentFolder.getPath();
    }

    /**
     * Get the parent folder for the passed in Document object.
     * Called the primary parent folder in the Alfresco world as most documents only have one parent folder.
     *
     * @param document the Document object to get the parent folder for
     * @return the parent Folder object, or null if it does not have a parent folder and is un-filed
     */
    private Folder getDocumentParentFolder(Document document) {
        // Get all the parent folders (could be more than one if multi-filed)
        List<Folder> parentFolders = document.getParents();

        // Grab the first parent folder
        if (parentFolders.size() > 0) {
            if (parentFolders.size() > 1) {
                logger.info("The " + document.getName() + " has more than one parent folder, it is multi-filed");
            }

            return parentFolders.get(0);
        } else {
            logger.info("Document " + document.getName() + " is un-filed and does not have a parent folder");
            return null;
        }
    }

    /**
     * Returns date as a string
     *
     * @param date date object
     * @return date as a string formatted with "yyyy-MM-dd HH:mm:ss z"
     */
    private String date2String(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(date);
    }
}