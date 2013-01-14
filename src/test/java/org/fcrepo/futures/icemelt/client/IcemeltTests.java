package org.fcrepo.futures.icemelt.client;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    org.fcrepo.futures.icemelt.client.TestVaults.class
    })
public class IcemeltTests {
    // just in case this is an old junit
    public static junit.framework.Test suite() throws Exception {
        junit.framework.TestSuite suite = new junit.framework.TestSuite(IcemeltTests.class.getName());
        suite.addTest(suite(TestVaults.class));
        return suite;
    }

    private static junit.framework.Test suite(Class klass) {
        return new junit.framework.JUnit4TestAdapter(klass);
    }
}
