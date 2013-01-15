package org.fcrepo.futures.icemelt.client;

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.glacier.model.DeleteVaultRequest;
import com.amazonaws.services.glacier.model.DescribeVaultOutput;
import com.amazonaws.services.glacier.model.DescribeVaultRequest;
import com.amazonaws.services.glacier.model.DescribeVaultResult;
import com.amazonaws.services.glacier.model.ListVaultsRequest;
import com.amazonaws.services.glacier.model.ListVaultsResult;



public class TestVaults extends IcemeltTestCase {

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
    public void testDescribe() {
        DescribeVaultRequest describeVaultRequest = new DescribeVaultRequest(m_defaultVaultName);
        DescribeVaultResult describe = m_client.describeVault(describeVaultRequest);
        assertEquals(m_defaultVaultName, describe.getVaultName());
    }

    @Test
    public void testList() {
        ListVaultsResult list = m_client.listVaults(new ListVaultsRequest());
        assertTrue("could not find vault with name \"" + m_defaultVaultName + "\"", findVaultId(m_defaultVaultName, list.getVaultList()));
        assertEquals(1,list.getVaultList().size());
    }

    @Test
    public void testDelete() {
        String vaultName = "deleteThisVault";
        createVault(vaultName);
        ListVaultsResult list = m_client.listVaults(new ListVaultsRequest());
        List<DescribeVaultOutput> vaults = list.getVaultList();
        assertTrue("could not find vault with name \"" + vaultName + "\"", findVaultId(vaultName, vaults));
        assertTrue("could not find vault with name \"" + m_defaultVaultName + "\"", findVaultId(m_defaultVaultName, vaults));

        //assertEquals(2,vaults.size()); // myvault and deletethisvault
        DeleteVaultRequest deleteVaultRequest = new DeleteVaultRequest("deleteThisVault");
        m_client.deleteVault(deleteVaultRequest);
        list = m_client.listVaults(new ListVaultsRequest());
        assertFalse("Vault with id \"" + vaultName + "\" wasn't deleted!",findVaultId(vaultName, list.getVaultList()));
    }

    private boolean findVaultId(String needle, List<DescribeVaultOutput> haystack) {
        boolean found = false;
        for (DescribeVaultOutput describe:haystack){
            if (needle.equals(describe.getVaultName())) found = true;
        }
        return found;
    }

    @Test
    public void testInventory() {
        try {
            JsonNode inventory = retrieveInventory(m_defaultVaultName);
            assertTrue(inventory.findValues("ArchiveList").size() > 0);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }
}
