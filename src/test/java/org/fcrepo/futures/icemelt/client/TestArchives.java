package org.fcrepo.futures.icemelt.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Arrays;

import org.codehaus.jackson.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.glacier.TreeHashGenerator;
import com.amazonaws.services.glacier.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.glacier.model.CompleteMultipartUploadResult;
import com.amazonaws.services.glacier.model.DeleteArchiveRequest;
import com.amazonaws.services.glacier.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.glacier.model.InitiateMultipartUploadResult;
import com.amazonaws.services.glacier.model.UploadArchiveRequest;
import com.amazonaws.services.glacier.model.UploadArchiveResult;
import com.amazonaws.services.glacier.model.UploadMultipartPartRequest;


public class TestArchives extends IcemeltTestCase {

    @Override
    @Before
    public void setUp() {
        createVault(m_defaultVaultName);
    }

    @Override
    @After
    public void tearDown() {
        deleteVault(m_defaultVaultName);
    }

    @Test
    public void testUpload() throws UnsupportedEncodingException{
        String vaultId = "myvaultwithcontent";
        createVault(vaultId);
        UploadArchiveRequest uploadArchiveRequest = new UploadArchiveRequest();
        uploadArchiveRequest.setVaultName(vaultId);
        ByteArrayInputStream bytes = new ByteArrayInputStream("data body".getBytes("UTF-8"));
        String checksum = TreeHashGenerator.calculateTreeHash(bytes);
        bytes.reset();
        uploadArchiveRequest.setBody(bytes);
        uploadArchiveRequest.setContentLength(9L); // necessary!
        uploadArchiveRequest.setArchiveDescription("");
        UploadArchiveResult upload = m_client.uploadArchive(uploadArchiveRequest);
        assertEquals(checksum, upload.getChecksum());
        // retrieve it and make sure
        InputStream retrieved = retrieveArchive(vaultId, upload.getArchiveId());
        assertEquals(checksum, TreeHashGenerator.calculateTreeHash(retrieved));
        deleteVault(vaultId);
    }

    @Test
    public void testMultipartUpload(){
        // create a vault
        String vaultId = "multivault";
        createVault(vaultId);
        // initiate the upload & define art size
        String partSize = Integer.toString(1024*1024);
        InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(vaultId,"",partSize);
        InitiateMultipartUploadResult init = m_client.initiateMultipartUpload(initiateMultipartUploadRequest);
        // build some random bytes of content for the body
        SecureRandom rand = new SecureRandom();
        byte [] bytes = new byte[2097152];
        rand.nextBytes(bytes);
        ByteArrayInputStream body = new ByteArrayInputStream(bytes);
        // get the treehash
        String checksum = TreeHashGenerator.calculateTreeHash(body);
        body.reset();
        // build the range header & upload the part
        String range = "bytes 0-2097151";
        UploadMultipartPartRequest uploadMultipartPartRequest =
                new UploadMultipartPartRequest(vaultId,
                                               init.getUploadId(),
                                               checksum,
                                               range,
                                               body);
        m_client.uploadMultipartPart(uploadMultipartPartRequest);
        // complete the upload
        //-- bug in icemelt: when no vault name, it does not return appropriate JSON error
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest().withChecksum(checksum)
                .withArchiveSize(Integer.toString(bytes.length))
                .withUploadId(init.getUploadId())
                .withVaultName(vaultId);
        CompleteMultipartUploadResult upload = m_client.completeMultipartUpload(completeMultipartUploadRequest);
        String archiveId = upload.getArchiveId();
        try {
            JsonNode inventory = retrieveInventory(vaultId);
            // Now do an inventory of the vault, and check for the archive ID
            boolean found = false;
            for (JsonNode archiveNode:inventory.findValues("ArchiveList")) {
                // we should find our archive in the vault
                if (archiveNode.findValue("ArchiveId").getValueAsText().equals(archiveId)){
                    found = true;
                    // should be two blocks
                    assertEquals(2*1024*1024, archiveNode.findValue("Size").getValueAsInt());
                }
            }
            assertTrue("Could not find archive with id " + archiveId + " in list", found);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
        // fetch the content and make sure it is unchanged
        InputStream retrieved = retrieveArchive(vaultId, archiveId);
        assertEquals(checksum, TreeHashGenerator.calculateTreeHash(retrieved));
        // cleanup this mess
        DeleteArchiveRequest deleteArchiveRequest =
                new DeleteArchiveRequest().withVaultName(vaultId).withArchiveId(archiveId);
        m_client.deleteArchive(deleteArchiveRequest);
        deleteVault(vaultId);
    }

    @Test
    public void testRetrieveContent() throws UnsupportedEncodingException {
        UploadArchiveRequest uploadArchiveRequest = uploadString(m_defaultVaultName, "asdfgh");
        UploadArchiveResult upload = m_client.uploadArchive(uploadArchiveRequest);
        InputStream retrieved = retrieveArchive(m_defaultVaultName, upload.getArchiveId());
        byte[] expected = "asdfgh".getBytes("UTF-8");
        byte[] actual = new byte[expected.length];
        try {
            retrieved.read(actual);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.toString());
        }
        assertTrue(Arrays.equals(expected, actual));
    }

    private UploadArchiveRequest uploadString(String vaultId, String body) {
        ByteArrayInputStream bytes = null;
        long contentLength = 0;
        try{
            byte [] b = body.getBytes("UTF-8");
            bytes = new ByteArrayInputStream(b);
            contentLength = b.length;
        } catch (UnsupportedEncodingException e) {}; // shh
        String checksum = TreeHashGenerator.calculateTreeHash(bytes);
        bytes.reset();
        UploadArchiveRequest uploadArchiveRequest =
                new UploadArchiveRequest().withBody(bytes)
                                          .withVaultName(vaultId)
                                          .withChecksum(checksum)
                                          .withContentLength(contentLength)
                                          .withArchiveDescription("");
        return uploadArchiveRequest;
    }

}
