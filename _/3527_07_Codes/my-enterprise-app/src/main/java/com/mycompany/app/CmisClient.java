package com.mycompany.app;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpParser;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.mycompany.app.oauth.LocalServerReceiver;
import com.mycompany.app.oauth.VerificationCodeReceiver;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Command-line sample accessing Alfresco in the Cloud via CMIS and Google OAuth2 API
 *
 * @author martin.bergljung@ixxus.com
 * @version 1.0
 */
public class CmisClient {
    private static Log logger = LogFactory.getLog(CmisClient.class);

    public static final String ALFRESCO_API_URL = "https://api.alfresco.com/";
    public static final String TOKEN_SERVER_URL = ALFRESCO_API_URL + "auth/oauth/versions/2/token";
    public static final String AUTHORIZATION_SERVER_URL = ALFRESCO_API_URL + "auth/oauth/versions/2/authorize";
    public static final String ATOMPUB_URL = ALFRESCO_API_URL + "cmis/versions/1.0/atom";
    public static final String SCOPE = "public_api";
    public static final String CLIENT_APP_ID = "l7xx5e11e85c4a764cc9af182c35e0711854"; // Get this from Alfresco developer portal
    public static final String CLIENT_APP_SECRET = "c7fa1952bfaf4cb7971dcc6ae2902fe3"; // Get this from Alfresco developer portal

    public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    public static final JsonFactory JSON_FACTORY = new JacksonFactory();

    /**
     * Authorize this client application and then make some CMIS calls
     *
     * @throws Exception
     */
    public void authorizeAndMakeCmisCalls() throws Exception {
        VerificationCodeReceiver receiver = new LocalServerReceiver();

        try {
            // Authorize this client application
            String callbackURL = receiver.getRedirectUri();
            launchBrowserAndMakeTokenRequest("google-chrome", callbackURL, CLIENT_APP_ID, SCOPE);
            final Credential credential = exchangeCodeForToken(receiver, callbackURL);
            HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) throws IOException {
                    credential.initialize(request);
                    request.addParser(new JsonHttpParser(JSON_FACTORY));
                }
            });

            logger.info("Access token:" + credential.getAccessToken());

            // Make some CMIS calls
            makeCmisCall(requestFactory, credential);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            receiver.stop();
        }
    }

    /**
     * PRIVATE METHODS *************************************************************************************************
     */

    /**
     * This launches the Google Chrome browser and calls the Alfresco Authorization Server to
     * request an authorization code, which is needed to get an access token.
     *
     * @param browser what Web Browser to use for the call
     * @param callbackURL what URL to redirect to for calling back to the application with the authorization code
     * @param clientId the client application identifier (generated via Alfresco Dev Portal)
     * @param scope there is only one scope for the Alfresco Cloud API (public_api)
     * @throws IOException
     */
    private void launchBrowserAndMakeTokenRequest(String browser, String callbackURL, String clientId, String scope)
            throws IOException {
        String authorizationUrl = new AuthorizationCodeRequestUrl(AUTHORIZATION_SERVER_URL, clientId).
                setRedirectUri(callbackURL).setScopes(Arrays.asList(scope)).build();

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI.create(authorizationUrl));
                return;
            }
        }

        if (browser != null) {
            Runtime.getRuntime().exec(new String[]{browser, authorizationUrl});
        } else {
            logger.info("Open the following address in your favorite browser:");
            logger.info("  " + authorizationUrl);
        }
    }

    /**
     * Exchange the authorization code for an access token
     *
     * @param receiver the code receiver, will be waiting for the code
     * @param callbackURL what URL to redirect to for calling back to the application with the acccess token
     * @return credentials with access token that can be used for CMIS calls
     * @throws IOException
     */
    private Credential exchangeCodeForToken(VerificationCodeReceiver receiver, String callbackURL)
            throws IOException {
        String code = receiver.waitForCode();

        AuthorizationCodeFlow codeFlow = new AuthorizationCodeFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(),
                HTTP_TRANSPORT,
                JSON_FACTORY,
                new GenericUrl(TOKEN_SERVER_URL),
                new ClientParametersAuthentication(CLIENT_APP_ID, CLIENT_APP_SECRET),
                CLIENT_APP_ID,
                AUTHORIZATION_SERVER_URL).setScopes(SCOPE).build();

        TokenResponse response = codeFlow.newTokenRequest(code)
                .setRedirectUri(callbackURL).setScopes(SCOPE).execute();

        return codeFlow.createAndStoreCredential(response, null);
    }

    /**
     * Do some CMIS calls via OpenCMIS Java library
     *
     * @param requestFactory
     * @param credential credentials with access token to use during the CMIS call
     * @throws IOException
     */
    private void makeCmisCall(HttpRequestFactory requestFactory, Credential credential)
            throws IOException {
        String accessToken = credential.getAccessToken();
        Session cmisSession = getCmisSession(accessToken);

        // Get information about the repository we are connected to
        RepositoryInfo repositoryInfo = cmisSession.getRepositoryInfo();

        logger.info("    Name: " + repositoryInfo.getName());
        logger.info("  Vendor: " + repositoryInfo.getVendorName());
        logger.info(" Version: " + repositoryInfo.getProductVersion());
    }

    /**
     * Gets a CMIS Session by connecting to the Alfresco Cloud.
     *
     * @param accessToken
     * @return Session
     */
    public Session getCmisSession(String accessToken) {
        // default factory implementation
        SessionFactory factory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<String, String>();

        // connection settings
        parameter.put(SessionParameter.ATOMPUB_URL, ATOMPUB_URL);
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        parameter.put(SessionParameter.AUTH_HTTP_BASIC, "false");
        parameter.put(SessionParameter.HEADER + ".0", "Authorization: Bearer " + accessToken);
        // If you want Alfresco Aspect support use:   parameter.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");

        java.util.List<Repository> repositories = factory.getRepositories(parameter);

        return repositories.get(0).createSession();
    }
}