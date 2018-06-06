package com.king.jdk.net;

import java.io.ObjectStreamException;

public final class Inet4Address extends InetAddress {
    final static int INADDRSZ = 4;

    private static final long serialVersionUID = 3286316764910316507L;

    static {
        init();
    }

    Inet4Address() {
        super();
        holder().hostName = null;
        holder().address = 0;
        holder().family = IPv4;
    }

    Inet4Address(String hostName, byte addr[]) {
        holder().hostName = hostName;
        holder().family = IPv4;
        if (addr != null) {
            if (addr.length == INADDRSZ) {
                int address = addr[3] & 0xFF;
                address |= ((addr[2] << 8) & 0xFF00);
                address |= ((addr[1] << 16) & 0xFF0000);
                address |= ((addr[0] << 24) & 0xFF000000);
                holder().address = address;
            }
        }
    }

    Inet4Address(String hostName, int address) {
        holder().hostName = hostName;
        holder().family = IPv4;
        holder().address = address;
    }

    static String numericToTextFormat(byte[] src) {
        return (src[0] & 0xff) + "." + (src[1] & 0xff) + "." + (src[2] & 0xff) + "." + (src[3] & 0xff);
    }


    private static native void init();


    private Object writeReplace() throws ObjectStreamException {
        // will replace the to be serialized 'this' object
        InetAddress inet = new InetAddress();
        inet.holder().hostName = holder().getHostName();
        inet.holder().address = holder().getAddress();


        inet.holder().family = 2;

        return inet;
    }


    public boolean isMulticastAddress() {
        return ((holder().getAddress() & 0xf0000000) == 0xe0000000);
    }


    public boolean isAnyLocalAddress() {
        return holder().getAddress() == 0;
    }


    public boolean isLoopbackAddress() {
        /* 127.x.x.x */
        byte[] byteAddr = getAddress();
        return byteAddr[0] == 127;
    }


    public boolean isLinkLocalAddress() {

        int address = holder().getAddress();
        return (((address >>> 24) & 0xFF) == 169)
                && (((address >>> 16) & 0xFF) == 254);
    }


    public boolean isSiteLocalAddress() {

        int address = holder().getAddress();
        return (((address >>> 24) & 0xFF) == 10)
                || ((((address >>> 24) & 0xFF) == 172)
                && (((address >>> 16) & 0xF0) == 16))
                || ((((address >>> 24) & 0xFF) == 192)
                && (((address >>> 16) & 0xFF) == 168));
    }


    public boolean isMCGlobal() {
        // 224.0.1.0 to 238.255.255.255
        byte[] byteAddr = getAddress();
        return ((byteAddr[0] & 0xff) >= 224 && (byteAddr[0] & 0xff) <= 238) &&
                !((byteAddr[0] & 0xff) == 224 && byteAddr[1] == 0 &&
                        byteAddr[2] == 0);
    }


    public boolean isMCNodeLocal() {
        // unless ttl == 0
        return false;
    }


    public boolean isMCLinkLocal() {
        // 224.0.0/24 prefix and ttl == 1
        int address = holder().getAddress();
        return (((address >>> 24) & 0xFF) == 224)
                && (((address >>> 16) & 0xFF) == 0)
                && (((address >>> 8) & 0xFF) == 0);
    }

    public boolean isMCSiteLocal() {
        // 239.255/16 prefix or ttl < 32
        int address = holder().getAddress();
        return (((address >>> 24) & 0xFF) == 239)
                && (((address >>> 16) & 0xFF) == 255);
    }


    public boolean isMCOrgLocal() {
        // 239.192 - 239.195
        int address = holder().getAddress();
        return (((address >>> 24) & 0xFF) == 239)
                && (((address >>> 16) & 0xFF) >= 192)
                && (((address >>> 16) & 0xFF) <= 195);
    }


    public byte[] getAddress() {
        int address = holder().getAddress();
        byte[] addr = new byte[INADDRSZ];

        addr[0] = (byte) ((address >>> 24) & 0xFF);
        addr[1] = (byte) ((address >>> 16) & 0xFF);
        addr[2] = (byte) ((address >>> 8) & 0xFF);
        addr[3] = (byte) (address & 0xFF);
        return addr;
    }


    public String getHostAddress() {
        return numericToTextFormat(getAddress());
    }

    public int hashCode() {
        return holder().getAddress();
    }

    public boolean equals(Object obj) {
        return (obj != null) && (obj instanceof Inet4Address) &&
                (((InetAddress) obj).holder().getAddress() == holder().getAddress());
    }
}
