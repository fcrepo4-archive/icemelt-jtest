package org.fcrepo.futures.icemelt.client;

import junit.framework.TestCase;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.CreateVaultRequest;
import com.amazonaws.services.glacier.model.CreateVaultResult;



public class IcemeltTestCase extends TestCase {

    public static AmazonGlacierClient getClient() {
        String endpoint = "http://localhost:3000";
        if (System.getProperty("icemelt.endpoint") != null) {
            endpoint = System.getProperty("icemelt.endpoint");
        }
        ClientConfiguration config = new ClientConfiguration();
        AWSCredentials creds = new BasicAWSCredentials("","");
        AmazonGlacierClient client = new AmazonGlacierClient(creds, config);
        client.setEndpoint(endpoint);
        return client;
    }

    protected CreateVaultResult createVault(String vaultName) {
        CreateVaultRequest createVaultRequest = new CreateVaultRequest(vaultName);
        return m_client.createVault(createVaultRequest);
    }

    protected AmazonGlacierClient m_client;
    protected final String m_defaultVaultName = "myvault";

    public IcemeltTestCase() {
        m_client = getClient();
    }

}
