package com.king.jdk.net;


import com.king.jdk.net.exception.SocketException;

import java.security.AccessController;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public final class NetworkInterface {
    private static final NetworkInterface defaultInterface;
    private static final int defaultIndex; /* index of defaultInterface */

    static {
        AccessController.doPrivileged(
                new java.security.PrivilegedAction<Void>() {
                    public Void run() {
                        System.loadLibrary("jdk/net");
                        return null;
                    }
                });

        init();
        defaultInterface = DefaultInterface.getDefault();
        if (defaultInterface != null) {
            defaultIndex = defaultInterface.getIndex();
        } else {
            defaultIndex = 0;
        }
    }

    private String name;
    private String displayName;
    private int index;
    private InetAddress addrs[];
    private InterfaceAddress bindings[];
    private NetworkInterface childs[];
    private NetworkInterface parent = null;
    private boolean virtual = false;

    NetworkInterface() {
    }

    NetworkInterface(String name, int index, InetAddress[] addrs) {
        this.name = name;
        this.index = index;
        this.addrs = addrs;
    }

    public static NetworkInterface getByName(String name) throws SocketException {
        if (name == null)
            throw new NullPointerException();
        return getByName0(name);
    }

    public static NetworkInterface getByIndex(int index) throws SocketException {
        if (index < 0)
            throw new IllegalArgumentException("Interface index can't be negative");
        return getByIndex0(index);
    }

    public static NetworkInterface getByInetAddress(InetAddress addr) throws SocketException {
        if (addr == null) {
            throw new NullPointerException();
        }
        if (!(addr instanceof Inet4Address || addr instanceof Inet6Address)) {
            throw new IllegalArgumentException("invalid address type");
        }
        return getByInetAddress0(addr);
    }

    public static Enumeration<NetworkInterface> getNetworkInterfaces()
            throws SocketException {
        final NetworkInterface[] netifs = getAll();

        // specified to return null if no network interfaces
        if (netifs == null)
            return null;

        return new Enumeration<NetworkInterface>() {
            private int i = 0;

            public NetworkInterface nextElement() {
                if (netifs != null && i < netifs.length) {
                    NetworkInterface netif = netifs[i++];
                    return netif;
                } else {
                    throw new NoSuchElementException();
                }
            }

            public boolean hasMoreElements() {
                return (netifs != null && i < netifs.length);
            }
        };
    }

    private native static NetworkInterface[] getAll()
            throws SocketException;

    private native static NetworkInterface getByName0(String name)
            throws SocketException;

    private native static NetworkInterface getByIndex0(int index)
            throws SocketException;

    private native static NetworkInterface getByInetAddress0(InetAddress addr)
            throws SocketException;

    private native static boolean isUp0(String name, int ind) throws SocketException;

    private native static boolean isLoopback0(String name, int ind) throws SocketException;

    private native static boolean supportsMulticast0(String name, int ind) throws SocketException;

    private native static boolean isP2P0(String name, int ind) throws SocketException;

    private native static byte[] getMacAddr0(byte[] inAddr, String name, int ind) throws SocketException;

    private native static int getMTU0(String name, int ind) throws SocketException;

    private static native void init();

    /**
     * Returns the default network interface of this system
     *
     * @return the default interface
     */
    static NetworkInterface getDefault() {
        return defaultInterface;
    }

    /**
     * Get the name of this network interface.
     *
     * @return the name of this network interface
     */
    public String getName() {
        return name;
    }

    public Enumeration<InetAddress> getInetAddresses() {

        class checkedAddresses implements Enumeration<InetAddress> {

            private int i = 0, count = 0;
            private InetAddress local_addrs[];

            checkedAddresses() {
                local_addrs = new InetAddress[addrs.length];
                boolean trusted = true;

                SecurityManager sec = System.getSecurityManager();
                if (sec != null) {
                    try {
                        sec.checkPermission(new NetPermission("getNetworkInformation"));
                    } catch (SecurityException e) {
                        trusted = false;
                    }
                }
                for (int j = 0; j < addrs.length; j++) {
                    try {
                        if (sec != null && !trusted) {
                            sec.checkConnect(addrs[j].getHostAddress(), -1);
                        }
                        local_addrs[count++] = addrs[j];
                    } catch (SecurityException e) {
                    }
                }

            }

            public InetAddress nextElement() {
                if (i < count) {
                    return local_addrs[i++];
                } else {
                    throw new NoSuchElementException();
                }
            }

            public boolean hasMoreElements() {
                return (i < count);
            }
        }
        return new checkedAddresses();

    }

    public java.util.List<InterfaceAddress> getInterfaceAddresses() {
        java.util.List<InterfaceAddress> lst = new java.util.ArrayList<InterfaceAddress>(1);
        SecurityManager sec = System.getSecurityManager();
        for (int j = 0; j < bindings.length; j++) {
            try {
                if (sec != null) {
                    sec.checkConnect(bindings[j].getAddress().getHostAddress(), -1);
                }
                lst.add(bindings[j]);
            } catch (SecurityException e) {
            }
        }
        return lst;
    }

    /**
     * Get an Enumeration with all the subinterfaces (also known as virtual
     * interfaces) attached to this network interface.
     * <p>
     * For instance eth0:1 will be a subinterface to eth0.
     *
     * @return an Enumeration object with all of the subinterfaces
     * of this network interface
     * @since 1.6
     */
    public Enumeration<NetworkInterface> getSubInterfaces() {
        class subIFs implements Enumeration<NetworkInterface> {

            private int i = 0;

            subIFs() {
            }

            public NetworkInterface nextElement() {
                if (i < childs.length) {
                    return childs[i++];
                } else {
                    throw new NoSuchElementException();
                }
            }

            public boolean hasMoreElements() {
                return (i < childs.length);
            }
        }
        return new subIFs();

    }

    /**
     * Returns the parent NetworkInterface of this interface if this is
     * a subinterface, or {@code null} if it is a physical
     * (non virtual) interface or has no parent.
     *
     * @return The {@code NetworkInterface} this interface is attached to.
     * @since 1.6
     */
    public NetworkInterface getParent() {
        return parent;
    }

    public int getIndex() {
        return index;
    }

    public String getDisplayName() {
        /* strict TCK conformance */
        return "".equals(displayName) ? null : displayName;
    }


    public boolean isUp() throws SocketException {
        return isUp0(name, index);
    }

    public boolean isLoopback() throws SocketException {
        return isLoopback0(name, index);
    }

    public boolean isPointToPoint() throws SocketException {
        return isP2P0(name, index);
    }

    public boolean supportsMulticast() throws SocketException {
        return supportsMulticast0(name, index);
    }

    public byte[] getHardwareAddress() throws SocketException {
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            try {
                sec.checkPermission(new NetPermission("getNetworkInformation"));
            } catch (SecurityException e) {
                if (!getInetAddresses().hasMoreElements()) {
                    // don't have connect permission to any local address
                    return null;
                }
            }
        }
        for (InetAddress addr : addrs) {
            if (addr instanceof Inet4Address) {
                return getMacAddr0(((Inet4Address) addr).getAddress(), name, index);
            }
        }
        return getMacAddr0(null, name, index);
    }

    public int getMTU() throws SocketException {
        return getMTU0(name, index);
    }

    public boolean isVirtual() {
        return virtual;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkInterface)) {
            return false;
        }
        NetworkInterface that = (NetworkInterface) obj;
        if (this.name != null) {
            if (!this.name.equals(that.name)) {
                return false;
            }
        } else {
            if (that.name != null) {
                return false;
            }
        }

        if (this.addrs == null) {
            return that.addrs == null;
        } else if (that.addrs == null) {
            return false;
        }

        /* Both addrs not null. Compare number of addresses */

        if (this.addrs.length != that.addrs.length) {
            return false;
        }

        InetAddress[] thatAddrs = that.addrs;
        int count = thatAddrs.length;

        for (int i = 0; i < count; i++) {
            boolean found = false;
            for (int j = 0; j < count; j++) {
                if (addrs[i].equals(thatAddrs[j])) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return name == null ? 0 : name.hashCode();
    }

    public String toString() {
        String result = "name:";
        result += name == null ? "null" : name;
        if (displayName != null) {
            result += " (" + displayName + ")";
        }
        return result;
    }
}

