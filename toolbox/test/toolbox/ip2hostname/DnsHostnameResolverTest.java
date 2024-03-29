package toolbox.ip2hostname;

import org.apache.log4j.Logger;

import junit.framework.TestCase;

public class DnsHostnameResolverTest extends TestCase {

    private static final Logger log = Logger.getLogger(DnsHostnameResolverTest.class);

    public void testResolveIPAddressToHostnameSuccessful() throws Exception {
        log.info("Running testResolveIPAddressToHostnameSuccessful...");
        DnsHostnameResolver resolver = new DnsHostnameResolver();
        String ipAddress = "127.0.0.1";
        String hostname = resolver.resolve(ipAddress);
        log.debug(hostname);
        assertFalse(ipAddress.equals(hostname));
    }
    
    public void testResolveBogusIPAddress() throws Exception {
        log.info("Running testResolveBogusIPAddress...");
        DnsHostnameResolver resolver = new DnsHostnameResolver();
        String hostname = resolver.resolve("1.1.1.1");
        log.debug(hostname);
        assertEquals("1.1.1.1", hostname);
    }
}
