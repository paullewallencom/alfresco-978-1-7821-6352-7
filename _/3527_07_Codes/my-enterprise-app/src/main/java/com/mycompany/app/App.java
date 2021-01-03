package com.mycompany.app;

/**
 * Example application calling Alfresco in the Cloud
 *
 * @author martin.bergljung@ixxus.com
 */
public class App {
    public static void main(String[] args) {
        CmisClient cmisClient = new CmisClient();
        try {
            cmisClient.authorizeAndMakeCmisCalls();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}