package org.fcrepo.futures.icemelt.client;

import java.io.ByteArrayInputStream;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.glacier.TreeHashGenerator;
import com.amazonaws.services.glacier.model.UploadArchiveRequest;
import com.amazonaws.services.glacier.model.UploadArchiveResult;


public class TestArchives extends IcemeltTestCase {

    @Override
    @Before
    public void setUp() {
        createVault(m_defaultVaultName);
    }

    @Test
    public void testUpload(){
        createVault("myvaultwithcontent");
        UploadArchiveRequest car = new UploadArchiveRequest();
        car.setVaultName("myvaultwithcontent");
        ByteArrayInputStream bytes = new ByteArrayInputStream("data body".getBytes());
        String checksum = new TreeHashGenerator().calculateTreeHash(bytes);
        bytes.reset();
        car.setBody(bytes);
        car.setArchiveDescription("");
        UploadArchiveResult upload = m_client.uploadArchive(car);
        // where is the test here? we'd
        assertEquals(checksum, upload.getChecksum());
    }

    @Test
    public void testMultipartUpload(){

    }
}
