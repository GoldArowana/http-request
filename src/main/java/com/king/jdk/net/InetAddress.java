package com.king.jdk.net;


import com.king.jdk.net.exception.UnknownHostException;
import sun.net.InetAddressCachePolicy;
import com.king.sun.net.NameService;
import com.king.sun.net.NameServiceDescriptor;
import sun.net.util.IPAddressUtil;
import sun.security.action.GetPropertyAction;

import java.io.*;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream.PutField;
import java.security.AccessController;
import java.util.*;

public class InetAddress implements java.io.Serializable {
    static final int IPv4 = 1;

    static final int IPv6 = 2;

    private static final long serialVersionUID = 3286316764910316507L;
    private static final HashMap<String, Void> lookupTable = new HashMap<>();
    private static final long maxCacheTime = 5000L;
    private static final Object cacheLock = new Object();
    private static final long FIELDS_OFFSET;
    private static final sun.misc.Unsafe UNSAFE;
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("hostName", String.class),
            new ObjectStreamField("address", int.class),
            new ObjectStreamField("family", int.class),
    };
    static transient boolean preferIPv6Address = false;
    static InetAddress[] unknown_array; // put THIS in cache
    static InetAddressImpl impl;
    private static List<NameService> nameServices = null;
    private static Cache addressCache = new Cache(Cache.Type.Positive);
    private static Cache negativeCache = new Cache(Cache.Type.Negative);
    private static boolean addressCacheInit = false;
    private static InetAddress cachedLocalHost = null;
    private static long cacheTime = 0;

    static {
        preferIPv6Address = false;
        AccessController.doPrivileged(
                new java.security.PrivilegedAction<Void>() {
                    public Void run() {
                        System.loadLibrary("jdk/net");
                        return null;
                    }
                });
        init();
    }

    static {
        // create the impl
        impl = InetAddressImplFactory.create();

        // get name service if provided and requested
        String provider = null;
        ;
        String propPrefix = "sun.net.spi.nameservice.provider.";
        int n = 1;
        nameServices = new ArrayList<NameService>();
        provider = AccessController.doPrivileged(
                new GetPropertyAction(propPrefix + n));
        while (provider != null) {
            NameService ns = createNSProvider(provider);
            if (ns != null)
                nameServices.add(ns);

            n++;
            provider = AccessController.doPrivileged(
                    new GetPropertyAction(propPrefix + n));
        }

        if (nameServices.size() == 0) {
            NameService ns = createNSProvider("default");
            nameServices.add(ns);
        }
    }

    static {
        try {
            sun.misc.Unsafe unsafe = sun.misc.Unsafe.getUnsafe();
            FIELDS_OFFSET = unsafe.objectFieldOffset(
                    InetAddress.class.getDeclaredField("holder")
            );
            UNSAFE = unsafe;
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    /* Used to store the serializable fields of InetAddress */
    final transient InetAddressHolder holder;
    /* Used to store the best available hostname */
    private transient String canonicalHostName = null;

    InetAddress() {
        holder = new InetAddressHolder();
    }

    private static String getHostFromNameService(InetAddress addr, boolean check) {
        String host = null;
        for (NameService nameService : nameServices) {
            try {
                // first lookup the hostname
                host = nameService.getHostByAddr(addr.getAddress());

                if (check) {
                    SecurityManager sec = System.getSecurityManager();
                    if (sec != null) {
                        sec.checkConnect(host, -1);
                    }
                }

                InetAddress[] arr = InetAddress.getAllByName0(host, check);
                boolean ok = false;

                if (arr != null) {
                    for (int i = 0; !ok && i < arr.length; i++) {
                        ok = addr.equals(arr[i]);
                    }
                }

                if (!ok) {
                    host = addr.getHostAddress();
                    return host;
                }

                break;

            } catch (SecurityException e) {
                host = addr.getHostAddress();
                break;
            } catch (UnknownHostException e) {
                host = addr.getHostAddress();
                // let next provider resolve the hostname
            }
        }

        return host;
    }

    /*
     * Initialize cache and insert anyLocalAddress into the
     * unknown array with no expiry.
     */
    private static void cacheInitIfNeeded() {
        assert Thread.holdsLock(addressCache);
        if (addressCacheInit) {
            return;
        }
        unknown_array = new InetAddress[1];
        unknown_array[0] = impl.anyLocalAddress();

        addressCache.put(impl.anyLocalAddress().getHostName(),
                unknown_array);

        addressCacheInit = true;
    }

    /*
     * Cache the given hostname and addresses.
     */
    private static void cacheAddresses(String hostname,
                                       InetAddress[] addresses,
                                       boolean success) {
        hostname = hostname.toLowerCase();
        synchronized (addressCache) {
            cacheInitIfNeeded();
            if (success) {
                addressCache.put(hostname, addresses);
            } else {
                negativeCache.put(hostname, addresses);
            }
        }
    }

    /*
     * Lookup hostname in cache (positive & negative cache). If
     * found return addresses, null if not found.
     */
    private static InetAddress[] getCachedAddresses(String hostname) {
        hostname = hostname.toLowerCase();

        // search both positive & negative caches

        synchronized (addressCache) {
            cacheInitIfNeeded();

            CacheEntry entry = addressCache.get(hostname);
            if (entry == null) {
                entry = negativeCache.get(hostname);
            }

            if (entry != null) {
                return entry.addresses;
            }
        }

        // not found
        return null;
    }

    private static NameService createNSProvider(String provider) {
        if (provider == null)
            return null;

        NameService nameService = null;
        if (provider.equals("default")) {
            // initialize the default name service
            nameService = new NameService() {
                public InetAddress[] lookupAllHostAddr(String host)
                        throws UnknownHostException {
                    return impl.lookupAllHostAddr(host);
                }

                public String getHostByAddr(byte[] addr)
                        throws UnknownHostException {
                    return impl.getHostByAddr(addr);
                }
            };
        } else {
            final String providerName = provider;
            try {
                nameService = java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedExceptionAction<NameService>() {
                            public NameService run() {
                                Iterator<NameServiceDescriptor> itr =
                                        ServiceLoader.load(NameServiceDescriptor.class)
                                                .iterator();
                                while (itr.hasNext()) {
                                    NameServiceDescriptor nsd = itr.next();
                                    if (providerName.
                                            equalsIgnoreCase(nsd.getType() + ","
                                                    + nsd.getProviderName())) {
                                        try {
                                            return nsd.createNameService();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            System.err.println(
                                                    "Cannot create name service:"
                                                            + providerName + ": " + e);
                                        }
                                    }
                                }

                                return null;
                            }
                        }
                );
            } catch (java.security.PrivilegedActionException e) {
            }
        }

        return nameService;
    }

    /**
     * Creates an InetAddress based on the provided host name and IP address.
     * No name service is checked for the validity of the address.
     *
     * <p> The host name can either be a machine name, such as
     * "{@code java.sun.com}", or a textual representation of its IP
     * address.
     * <p> No validity checking is done on the host name either.
     *
     * <p> If addr specifies an IPv4 address an instance of Inet4Address
     * will be returned; otherwise, an instance of Inet6Address
     * will be returned.
     *
     * <p> IPv4 address byte array must be 4 bytes long and IPv6 byte array
     * must be 16 bytes long
     *
     * @param host the specified host
     * @param addr the raw IP address in network byte order
     * @return an InetAddress object created from the raw IP address.
     * @throws UnknownHostException if IP address is of illegal length
     * @since 1.4
     */
    public static InetAddress getByAddress(String host, byte[] addr)
            throws UnknownHostException {
        if (host != null && host.length() > 0 && host.charAt(0) == '[') {
            if (host.charAt(host.length() - 1) == ']') {
                host = host.substring(1, host.length() - 1);
            }
        }
        if (addr != null) {
            if (addr.length == Inet4Address.INADDRSZ) {
                return new Inet4Address(host, addr);
            } else if (addr.length == Inet6Address.INADDRSZ) {
                byte[] newAddr
                        = IPAddressUtil.convertFromIPv4MappedAddress(addr);
                if (newAddr != null) {
                    return new Inet4Address(host, newAddr);
                } else {
                    return new Inet6Address(host, addr);
                }
            }
        }
        throw new UnknownHostException("addr is of illegal length");
    }

    public static InetAddress getByName(String host)
            throws UnknownHostException {
        return InetAddress.getAllByName(host)[0];
    }

    // called from deployment cache manager
    private static InetAddress getByName(String host, InetAddress reqAddr)
            throws UnknownHostException {
        return InetAddress.getAllByName(host, reqAddr)[0];
    }

    public static InetAddress[] getAllByName(String host)
            throws UnknownHostException {
        return getAllByName(host, null);
    }

    private static InetAddress[] getAllByName(String host, InetAddress reqAddr)
            throws UnknownHostException {

        if (host == null || host.length() == 0) {
            InetAddress[] ret = new InetAddress[1];
            ret[0] = impl.loopbackAddress();
            return ret;
        }

        boolean ipv6Expected = false;
        if (host.charAt(0) == '[') {
            // This is supposed to be an IPv6 literal
            if (host.length() > 2 && host.charAt(host.length() - 1) == ']') {
                host = host.substring(1, host.length() - 1);
                ipv6Expected = true;
            } else {
                // This was supposed to be a IPv6 address, but it's not!
                throw new UnknownHostException(host + ": invalid IPv6 address");
            }
        }

        // if host is an IP address, we won't do further lookup
        if (Character.digit(host.charAt(0), 16) != -1
                || (host.charAt(0) == ':')) {
            byte[] addr = null;
            int numericZone = -1;
            String ifname = null;
            // see if it is IPv4 address
            addr = IPAddressUtil.textToNumericFormatV4(host);
            if (addr == null) {
                // This is supposed to be an IPv6 literal
                // Check if a numeric or string zone id is present
                int pos;
                if ((pos = host.indexOf("%")) != -1) {
                    numericZone = checkNumericZone(host);
                    if (numericZone == -1) { /* remainder of string must be an ifname */
                        ifname = host.substring(pos + 1);
                    }
                }
                if ((addr = IPAddressUtil.textToNumericFormatV6(host)) == null && host.contains(":")) {
                    throw new UnknownHostException(host + ": invalid IPv6 address");
                }
            } else if (ipv6Expected) {
                // Means an IPv4 litteral between brackets!
                throw new UnknownHostException("[" + host + "]");
            }
            InetAddress[] ret = new InetAddress[1];
            if (addr != null) {
                if (addr.length == Inet4Address.INADDRSZ) {
                    ret[0] = new Inet4Address(null, addr);
                } else {
                    if (ifname != null) {
                        ret[0] = new Inet6Address(null, addr, ifname);
                    } else {
                        ret[0] = new Inet6Address(null, addr, numericZone);
                    }
                }
                return ret;
            }
        } else if (ipv6Expected) {
            // We were expecting an IPv6 Litteral, but got something else
            throw new UnknownHostException("[" + host + "]");
        }
        return getAllByName0(host, reqAddr, true);
    }

    public static InetAddress getLoopbackAddress() {
        return impl.loopbackAddress();
    }

    private static int checkNumericZone(String s) throws UnknownHostException {
        int percent = s.indexOf('%');
        int slen = s.length();
        int digit, zone = 0;
        if (percent == -1) {
            return -1;
        }
        for (int i = percent + 1; i < slen; i++) {
            char c = s.charAt(i);
            if (c == ']') {
                if (i == percent + 1) {
                    /* empty per-cent field */
                    return -1;
                }
                break;
            }
            if ((digit = Character.digit(c, 10)) < 0) {
                return -1;
            }
            zone = (zone * 10) + digit;
        }
        return zone;
    }

    private static InetAddress[] getAllByName0(String host)
            throws UnknownHostException {
        return getAllByName0(host, true);
    }

    /**
     * package private so SocketPermission can call it
     */
    static InetAddress[] getAllByName0(String host, boolean check)
            throws UnknownHostException {
        return getAllByName0(host, null, check);
    }

    private static InetAddress[] getAllByName0(String host, InetAddress reqAddr, boolean check)
            throws UnknownHostException {

        if (check) {
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkConnect(host, -1);
            }
        }

        InetAddress[] addresses = getCachedAddresses(host);

        /* If no entry in cache, then do the host lookup */
        if (addresses == null) {
            addresses = getAddressesFromNameService(host, reqAddr);
        }

        if (addresses == unknown_array)
            throw new UnknownHostException(host);

        return addresses.clone();
    }

    private static InetAddress[] getAddressesFromNameService(String host, InetAddress reqAddr)
            throws UnknownHostException {
        InetAddress[] addresses = null;
        boolean success = false;
        UnknownHostException ex = null;

        if ((addresses = checkLookupTable(host)) == null) {
            try {
                for (NameService nameService : nameServices) {
                    try {

                        addresses = nameService.lookupAllHostAddr(host);
                        success = true;
                        break;
                    } catch (UnknownHostException uhe) {
                        if (host.equalsIgnoreCase("localhost")) {
                            InetAddress[] local = new InetAddress[]{impl.loopbackAddress()};
                            addresses = local;
                            success = true;
                            break;
                        } else {
                            addresses = unknown_array;
                            success = false;
                            ex = uhe;
                        }
                    }
                }

                // More to do?
                if (reqAddr != null && addresses.length > 1 && !addresses[0].equals(reqAddr)) {
                    // Find it?
                    int i = 1;
                    for (; i < addresses.length; i++) {
                        if (addresses[i].equals(reqAddr)) {
                            break;
                        }
                    }
                    // Rotate
                    if (i < addresses.length) {
                        InetAddress tmp, tmp2 = reqAddr;
                        for (int j = 0; j < i; j++) {
                            tmp = addresses[j];
                            addresses[j] = tmp2;
                            tmp2 = tmp;
                        }
                        addresses[i] = tmp2;
                    }
                }
                // Cache the address.
                cacheAddresses(host, addresses, success);

                if (!success && ex != null)
                    throw ex;

            } finally {
                // Delete host from the lookupTable and notify
                // all threads waiting on the lookupTable monitor.
                updateLookupTable(host);
            }
        }

        return addresses;
    }

    private static InetAddress[] checkLookupTable(String host) {
        synchronized (lookupTable) {
            // If the host isn't in the lookupTable, add it in the
            // lookuptable and return null. The caller should do
            // the lookup.
            if (lookupTable.containsKey(host) == false) {
                lookupTable.put(host, null);
                return null;
            }

            // If the host is in the lookupTable, it means that another
            // thread is trying to look up the addresses of this host.
            // This thread should wait.
            while (lookupTable.containsKey(host)) {
                try {
                    lookupTable.wait();
                } catch (InterruptedException e) {
                }
            }
        }

        // The other thread has finished looking up the addresses of
        // the host. This thread should retry to get the addresses
        // from the addressCache. If it doesn't get the addresses from
        // the cache, it will try to look up the addresses itself.
        InetAddress[] addresses = getCachedAddresses(host);
        if (addresses == null) {
            synchronized (lookupTable) {
                lookupTable.put(host, null);
                return null;
            }
        }

        return addresses;
    }

    private static void updateLookupTable(String host) {
        synchronized (lookupTable) {
            lookupTable.remove(host);
            lookupTable.notifyAll();
        }
    }

    public static InetAddress getByAddress(byte[] addr)
            throws UnknownHostException {
        return getByAddress(null, addr);
    }

    public static InetAddress getLocalHost() throws UnknownHostException {

        SecurityManager security = System.getSecurityManager();
        try {
            String local = impl.getLocalHostName();

            if (security != null) {
                security.checkConnect(local, -1);
            }

            if (local.equals("localhost")) {
                return impl.loopbackAddress();
            }

            InetAddress ret = null;
            synchronized (cacheLock) {
                long now = System.currentTimeMillis();
                if (cachedLocalHost != null) {
                    if ((now - cacheTime) < maxCacheTime) // Less than 5s old?
                        ret = cachedLocalHost;
                    else
                        cachedLocalHost = null;
                }

                // we are calling getAddressesFromNameService directly
                // to avoid getting localHost from cache
                if (ret == null) {
                    InetAddress[] localAddrs;
                    try {
                        localAddrs =
                                InetAddress.getAddressesFromNameService(local, null);
                    } catch (UnknownHostException uhe) {
                        // Rethrow with a more informative error message.
                        UnknownHostException uhe2 =
                                new UnknownHostException(local + ": " +
                                        uhe.getMessage());
                        uhe2.initCause(uhe);
                        throw uhe2;
                    }
                    cachedLocalHost = localAddrs[0];
                    cacheTime = now;
                    ret = localAddrs[0];
                }
            }
            return ret;
        } catch (java.lang.SecurityException e) {
            return impl.loopbackAddress();
        }
    }

    /**
     * Perform class load-time initializations.
     */
    private static native void init();

    /*
     * Returns the InetAddress representing anyLocalAddress
     * (typically 0.0.0.0 or ::0)
     */
    static InetAddress anyLocalAddress() {
        return impl.anyLocalAddress();
    }

    /*
     * Load and instantiate an underlying impl class
     */
    static InetAddressImpl loadImpl(String implName) {
        Object impl = null;

        /*
         * Property "impl.prefix" will be prepended to the classname
         * of the implementation object we instantiate, to which we
         * delegate the real work (like native methods).  This
         * property can vary across implementations of the java.
         * classes.  The default is an empty String "".
         */
        String prefix = AccessController.doPrivileged(
                new GetPropertyAction("impl.prefix", ""));
        try {
            impl = Class.forName("net." + prefix + implName).newInstance();
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: net.." + prefix +
                    implName + ":\ncheck impl.prefix property " +
                    "in your properties file.");
        } catch (InstantiationException e) {
            System.err.println("Could not instantiate: net." + prefix +
                    implName + ":\ncheck impl.prefix property " +
                    "in your properties file.");
        } catch (IllegalAccessException e) {
            System.err.println("Cannot access class: net." + prefix +
                    implName + ":\ncheck impl.prefix property " +
                    "in your properties file.");
        }

        if (impl == null) {
            try {
                impl = Class.forName(implName).newInstance();
            } catch (Exception e) {
                throw new Error("System property impl.prefix incorrect");
            }
        }

        return (InetAddressImpl) impl;
    }

    InetAddressHolder holder() {
        return holder;
    }

    private Object readResolve() throws ObjectStreamException {
        // will replace the deserialized 'this' object
        return new Inet4Address(holder().getHostName(), holder().getAddress());
    }

    public boolean isMulticastAddress() {
        return false;
    }

    public boolean isAnyLocalAddress() {
        return false;
    }

    public boolean isLoopbackAddress() {
        return false;
    }

    public boolean isLinkLocalAddress() {
        return false;
    }

    public boolean isSiteLocalAddress() {
        return false;
    }

    public boolean isMCGlobal() {
        return false;
    }

    public boolean isMCNodeLocal() {
        return false;
    }

    public boolean isMCLinkLocal() {
        return false;
    }

    public boolean isMCSiteLocal() {
        return false;
    }

    public boolean isMCOrgLocal() {
        return false;
    }

    public boolean isReachable(int timeout) throws IOException {
        return isReachable(null, 0, timeout);
    }

    public boolean isReachable(NetworkInterface netif, int ttl,
                               int timeout) throws IOException {
        if (ttl < 0)
            throw new IllegalArgumentException("ttl can't be negative");
        if (timeout < 0)
            throw new IllegalArgumentException("timeout can't be negative");

        return impl.isReachable(this, timeout, netif, ttl);
    }

    public String getHostName() {
        return getHostName(true);
    }

    String getHostName(boolean check) {
        if (holder().getHostName() == null) {
            holder().hostName = InetAddress.getHostFromNameService(this, check);
        }
        return holder().getHostName();
    }

    public String getCanonicalHostName() {
        if (canonicalHostName == null) {
            canonicalHostName =
                    InetAddress.getHostFromNameService(this, true);
        }
        return canonicalHostName;
    }

    public byte[] getAddress() {
        return null;
    }

    public String getHostAddress() {
        return null;
    }

    public int hashCode() {
        return -1;
    }

    public boolean equals(Object obj) {
        return false;
    }

    public String toString() {
        String hostName = holder().getHostName();
        return ((hostName != null) ? hostName : "")
                + "/" + getHostAddress();
    }

    private void readObjectNoData(ObjectInputStream s) throws
            IOException, ClassNotFoundException {
        if (getClass().getClassLoader() != null) {
            throw new SecurityException("invalid address type");
        }
    }

    private void readObject(ObjectInputStream s) throws
            IOException, ClassNotFoundException {
        if (getClass().getClassLoader() != null) {
            throw new SecurityException("invalid address type");
        }
        GetField gf = s.readFields();
        String host = (String) gf.get("hostName", null);
        int address = gf.get("address", 0);
        int family = gf.get("family", 0);
        InetAddressHolder h = new InetAddressHolder(host, address, family);
        UNSAFE.putObject(this, FIELDS_OFFSET, h);
    }

    private void writeObject(ObjectOutputStream s) throws
            IOException {
        if (getClass().getClassLoader() != null) {
            throw new SecurityException("invalid address type");
        }
        PutField pf = s.putFields();
        pf.put("hostName", holder().getHostName());
        pf.put("address", holder().getAddress());
        pf.put("family", holder().getFamily());
        s.writeFields();
    }

    static class InetAddressHolder {

        String hostName;
        /**
         * Holds a 32-bit IPv4 address.
         */
        int address;
        /**
         * Specifies the address family type, for instance, '1' for IPv4
         * addresses, and '2' for IPv6 addresses.
         */
        int family;

        InetAddressHolder() {
        }

        InetAddressHolder(String hostName, int address, int family) {
            this.hostName = hostName;
            this.address = address;
            this.family = family;
        }

        void init(String hostName, int family) {
            this.hostName = hostName;
            if (family != -1) {
                this.family = family;
            }
        }

        String getHostName() {
            return hostName;
        }

        int getAddress() {
            return address;
        }

        int getFamily() {
            return family;
        }
    }

    /* needed because the serializable fields no longer exist */

    /**
     * Represents a cache entry
     */
    static final class CacheEntry {

        InetAddress[] addresses;
        long expiration;

        CacheEntry(InetAddress[] addresses, long expiration) {
            this.addresses = addresses;
            this.expiration = expiration;
        }
    }

    /**
     * A cache that manages entries based on a policy specified
     * at creation time.
     */
    static final class Cache {
        private LinkedHashMap<String, CacheEntry> cache;
        private Type type;

        /**
         * Create cache
         */
        public Cache(Type type) {
            this.type = type;
            cache = new LinkedHashMap<String, CacheEntry>();
        }

        ;

        private int getPolicy() {
            if (type == Type.Positive) {
                return InetAddressCachePolicy.get();
            } else {
                return InetAddressCachePolicy.getNegative();
            }
        }

        /**
         * Add an entry to the cache. If there's already an
         * entry then for this host then the entry will be
         * replaced.
         */
        public Cache put(String host, InetAddress[] addresses) {
            int policy = getPolicy();
            if (policy == InetAddressCachePolicy.NEVER) {
                return this;
            }

            // purge any expired entries

            if (policy != InetAddressCachePolicy.FOREVER) {

                // As we iterate in insertion order we can
                // terminate when a non-expired entry is found.
                LinkedList<String> expired = new LinkedList<>();
                long now = System.currentTimeMillis();
                for (String key : cache.keySet()) {
                    CacheEntry entry = cache.get(key);

                    if (entry.expiration >= 0 && entry.expiration < now) {
                        expired.add(key);
                    } else {
                        break;
                    }
                }

                for (String key : expired) {
                    cache.remove(key);
                }
            }

            // create new entry and add it to the cache
            // -- as a HashMap replaces existing entries we
            //    don't need to explicitly check if there is
            //    already an entry for this host.
            long expiration;
            if (policy == InetAddressCachePolicy.FOREVER) {
                expiration = -1;
            } else {
                expiration = System.currentTimeMillis() + (policy * 1000);
            }
            CacheEntry entry = new CacheEntry(addresses, expiration);
            cache.put(host, entry);
            return this;
        }

        /**
         * Query the cache for the specific host. If found then
         * return its CacheEntry, or null if not found.
         */
        public CacheEntry get(String host) {
            int policy = getPolicy();
            if (policy == InetAddressCachePolicy.NEVER) {
                return null;
            }
            CacheEntry entry = cache.get(host);

            // check if entry has expired
            if (entry != null && policy != InetAddressCachePolicy.FOREVER) {
                if (entry.expiration >= 0 &&
                        entry.expiration < System.currentTimeMillis()) {
                    cache.remove(host);
                    entry = null;
                }
            }

            return entry;
        }

        enum Type {Positive, Negative}
    }
}



