package org.fcrepo.futures.icemelt.client;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.glacier.model.DeleteVaultRequest;
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

    @Test
    public void testDescribe() {
        DescribeVaultRequest describeVaultRequest = new DescribeVaultRequest(m_defaultVaultName);
        DescribeVaultResult describe = m_client.describeVault(describeVaultRequest);
        assertEquals(m_defaultVaultName, describe.getVaultName());
    }

    @Test
    public void testList() {
        ListVaultsResult list = m_client.listVaults(new ListVaultsRequest());
        assertEquals(1,list.getVaultList().size());
        assertEquals(m_defaultVaultName,list.getVaultList().get(0).getVaultName());
    }

    @Test
    public void testDelete() {
        createVault("deleteThisVault");
        ListVaultsResult list = m_client.listVaults(new ListVaultsRequest());
        assertEquals(2,list.getVaultList().size()); // myvault and deletethisvault
        DeleteVaultRequest deleteVaultRequest = new DeleteVaultRequest("deleteThisVault");
        // this is an asynch operation, so Java API doesn't have a result; Icemelt will cheat for us
        m_client.deleteVault(deleteVaultRequest);
        list = m_client.listVaults(new ListVaultsRequest());
        assertEquals(1,list.getVaultList().size());
        assertEquals(m_defaultVaultName,list.getVaultList().get(0).getVaultName());
    }
}
