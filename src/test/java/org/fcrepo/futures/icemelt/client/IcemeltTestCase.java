package org.fcrepo.futures.icemelt.client;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.CreateVaultRequest;
import com.amazonaws.services.glacier.model.CreateVaultResult;
import com.amazonaws.services.glacier.model.DeleteVaultRequest;
import com.amazonaws.services.glacier.model.DescribeJobRequest;
import com.amazonaws.services.glacier.model.GetJobOutputRequest;
import com.amazonaws.services.glacier.model.GetJobOutputResult;
import com.amazonaws.services.glacier.model.InitiateJobRequest;
import com.amazonaws.services.glacier.model.InitiateJobResult;
import com.amazonaws.services.glacier.model.JobParameters;



public abstract class IcemeltTestCase extends TestCase {

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

    protected AmazonGlacierClient m_client;
    protected final String m_defaultVaultName = "myvault";

    public IcemeltTestCase() {
        m_client = getClient();
    }

    protected CreateVaultResult createVault(String vaultName) {
        CreateVaultRequest createVaultRequest = new CreateVaultRequest(vaultName);
        return m_client.createVault(createVaultRequest);
    }

    protected void deleteVault(String vaultName) {
        DeleteVaultRequest deleteVaultRequest = new DeleteVaultRequest().withVaultName(vaultName);
        m_client.deleteVault(deleteVaultRequest);
    }

    protected void poll(String vaultName, String jobId) {
        DescribeJobRequest djr = new DescribeJobRequest(vaultName, jobId);
        while (!m_client.describeJob(djr).isCompleted()){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // This would never happen!
                e.printStackTrace();
                break;
            }
        }
    }

    protected JsonNode retrieveInventory(String vaultId) throws JsonParseException, IOException {
        InputStream output = retrieveJobOutput("inventory-retrieval", vaultId, null);
        // parse the json
        JsonParser jp = new JsonFactory(new ObjectMapper()).createJsonParser(output);
        return jp.readValueAsTree();
    }

    protected InputStream retrieveArchive(String vaultId, String archiveId) {
        return retrieveJobOutput("archive-retrieval", vaultId, archiveId);
    }

    protected InputStream retrieveJobOutput(String type, String vaultId, String archiveId) {
        JobParameters params =
            new JobParameters().withType(type);
        if (archiveId != null) params.setArchiveId(archiveId);
        InitiateJobRequest initiateJobRequest =
                new InitiateJobRequest().withVaultName(vaultId)
                                        .withJobParameters(params);
        InitiateJobResult job = m_client.initiateJob(initiateJobRequest);
        String jobId = job.getJobId();
        poll(vaultId, jobId); // this is artificially short in IceMelt, but there is still some lag
        GetJobOutputRequest getJobOutputRequest =
            new GetJobOutputRequest().withJobId(jobId)
                                     .withVaultName(vaultId);
        GetJobOutputResult jobOutput = m_client.getJobOutput(getJobOutputRequest);
        return jobOutput.getBody();
    }

}
