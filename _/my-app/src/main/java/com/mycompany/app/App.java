package com.mycompany.app;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;

import java.io.IOException;

/**
 * Application using OpenCMIS
 */
public class App {
    public static void main(String[] args) {
        CmisClient cmisClient = new CmisClient();
        String connectionName = "martinAlf01";
        Session session = cmisClient.getSession(connectionName, "admin", "admin");
        cmisClient.listRepoCapabilities(session.getRepositoryInfo());
        cmisClient.listTopFolder(session);
        cmisClient.listTopFolderWithPagingAndPropFilter(session);
        cmisClient.listTypesAndSubtypes(session);
        Folder folder = cmisClient.createFolder(session);
        Folder folder2 = cmisClient.createFolderWithCustomType(session);
        Document document = null;
        try {
            document = cmisClient.createDocument(session, folder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Document document2 = cmisClient.createDocumentFromFileWithCustomType(session);
        Folder updatedFolder = cmisClient.updateFolder(folder);
        Document updatedDocument = null;
        try {
            updatedDocument = cmisClient.updateDocument(session, document);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        cmisClient.deleteDocument(updatedDocument);
        cmisClient.deleteFolder(updatedFolder);
        cmisClient.deleteFolderTree(session);
        cmisClient.getContentForDocumentAndStoreInFile(session);
        cmisClient.copyDocument(session, document2); // Does not work when using Alfresco OpenCMIS extension as complains about myc:language not part of aspect or type
        cmisClient.moveDocument(session, document2);
        cmisClient.createFolderWithTitledAspect(session);
        // cmisClient.createFolderWithTitledAspectWithAlfrescoExtension(session);
        cmisClient.addAspectToExistingDocument(document2);
        // cmisClient.addAspectToExistingDocumentWithAlfrescoExtension(session);
        cmisClient.readAspectsForExistingDocument(document2);
        // cmisClient.readAspectsForExistingDocumentWithAlfrescoExtension(session);
        Document pwc = cmisClient.checkOutDocument(session, document2);
        cmisClient.updateContentAndCheckInDocument(session, pwc);
        cmisClient.addPermissionToFolder(session, folder2);
        cmisClient.addCheckOutPermissionsToUser(session, document2);
        cmisClient.setupRelationshipBetween2Folders(session);
        cmisClient.searchMetadataAndFTS(session);
    }
}
