package com.king.jdk.net;


/**
 * This class represents a Network Interface address. In short it's an
 * IP address, a subnet mask and a broadcast address when the address is
 * an IPv4 one. An IP address and a network prefix length in the case
 * of IPv6 address.
 *
 * @since 1.6
 */
public class InterfaceAddress {
    private InetAddress address = null;
    private Inet4Address broadcast = null;
    private short maskLength = 0;

    InterfaceAddress() {
    }

    public InetAddress getAddress() {
        return address;
    }

    public InetAddress getBroadcast() {
        return broadcast;
    }

    public short getNetworkPrefixLength() {
        return maskLength;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof InterfaceAddress)) {
            return false;
        }
        InterfaceAddress cmp = (InterfaceAddress) obj;
        if (!(address == null ? cmp.address == null : address.equals(cmp.address)))
            return false;
        if (!(broadcast == null ? cmp.broadcast == null : broadcast.equals(cmp.broadcast)))
            return false;
        if (maskLength != cmp.maskLength)
            return false;
        return true;
    }

    /**
     * Returns a hashcode for this Interface address.
     *
     * @return a hash code value for this Interface address.
     */
    public int hashCode() {
        return address.hashCode() + ((broadcast != null) ? broadcast.hashCode() : 0) + maskLength;
    }

    public String toString() {
        return address + "/" + maskLength + " [" + broadcast + "]";
    }

}
