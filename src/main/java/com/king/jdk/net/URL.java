package com.king.jdk.net;


import com.king.jdk.net.exception.MalformedURLException;
import com.king.jdk.net.exception.URISyntaxException;
import sun.security.util.SecurityConstants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.StringTokenizer;

public final class URL implements java.io.Serializable {

    static final long serialVersionUID = -7627629688361524110L;

    private static final String protocolPathProp = "java.protocol.handler.pkgs";
    /**
     * The URLStreamHandler factory.
     */
    static URLStreamHandlerFactory factory;
    /**
     * A table of protocol handlers.
     */
    static Hashtable<String, URLStreamHandler> handlers = new Hashtable<>();
    private static Object streamHandlerLock = new Object();
    /**
     * The host's IP address, used in equals and hashCode.
     * Computed on demand. An uninitialized or unknown hostAddress is null.
     */
    transient InetAddress hostAddress;
    /**
     * The URLStreamHandler for this URL.
     */
    transient URLStreamHandler handler;
    private String protocol;
    /**
     * The host name to connect to.
     *
     * @serial
     */
    private String host;
    /**
     * The protocol port to connect to.
     *
     * @serial
     */
    private int port = -1;
    /**
     * The specified file name on that host. {@code file} is
     * defined as {@code path[?query]}
     *
     * @serial
     */
    private String file;
    /**
     * The query part of this URL.
     */
    private transient String query;
    /**
     * The authority part of this URL.
     *
     * @serial
     */
    private String authority;
    /**
     * The path part of this URL.
     */
    private transient String path;
    /**
     * The userinfo part of this URL.
     */
    private transient String userInfo;
    /**
     * # reference.
     *
     * @serial
     */
    private String ref;
    /* Our hash code.
     * @serial
     */
    private int hashCode = -1;

    public URL(String protocol, String host, int port, String file) throws MalformedURLException {
        this(protocol, host, port, file, null);
    }

    public URL(String protocol, String host, String file)
            throws MalformedURLException {
        this(protocol, host, -1, file);
    }

    public URL(String protocol, String host, int port, String file,
               URLStreamHandler handler) throws MalformedURLException {
        if (handler != null) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                // check for permission to specify a handler
                checkSpecifyHandler(sm);
            }
        }

        protocol = protocol.toLowerCase();
        this.protocol = protocol;
        if (host != null) {

            /**
             * if host is a literal IPv6 address,
             * we will make it conform to RFC 2732
             */
            if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
                host = "[" + host + "]";
            }
            this.host = host;

            if (port < -1) {
                throw new MalformedURLException("Invalid port number :" +
                        port);
            }
            this.port = port;
            authority = (port == -1) ? host : host + ":" + port;
        }

        Parts parts = new Parts(file);
        path = parts.getPath();
        query = parts.getQuery();

        if (query != null) {
            this.file = path + "?" + query;
        } else {
            this.file = path;
        }
        ref = parts.getRef();

        // Note: we don't do validation of the URL here. Too risky to change
        // right now, but worth considering for future reference. -br
        if (handler == null &&
                (handler = getURLStreamHandler(protocol)) == null) {
            throw new MalformedURLException("unknown protocol: " + protocol);
        }
        this.handler = handler;
    }

    public URL(String spec) throws MalformedURLException {
        this(null, spec);
    }

    public URL(URL context, String spec) throws MalformedURLException {
        this(context, spec, null);
    }

    public URL(URL context, String spec, URLStreamHandler handler) throws MalformedURLException {
        String original = spec;
        int i, limit, c;
        int start = 0;
        String newProtocol = null;
        boolean aRef = false;
        boolean isRelative = false;

        // Check for permission to specify a handler
        if (handler != null) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                checkSpecifyHandler(sm);
            }
        }

        try {
            limit = spec.length();
            while ((limit > 0) && (spec.charAt(limit - 1) <= ' ')) {
                limit--;        //eliminate trailing whitespace
            }
            while ((start < limit) && (spec.charAt(start) <= ' ')) {
                start++;        // eliminate leading whitespace
            }

            if (spec.regionMatches(true, start, "url:", 0, 4)) {
                start += 4;
            }
            if (start < spec.length() && spec.charAt(start) == '#') {
                /* we're assuming this is a ref relative to the context URL.
                 * This means protocols cannot start w/ '#', but we must parse
                 * ref URL's like: "hello:there" w/ a ':' in them.
                 */
                aRef = true;
            }
            for (i = start; !aRef && (i < limit) &&
                    ((c = spec.charAt(i)) != '/'); i++) {
                if (c == ':') {

                    String s = spec.substring(start, i).toLowerCase();
                    if (isValidProtocol(s)) {
                        newProtocol = s;
                        start = i + 1;
                    }
                    break;
                }
            }

            // Only use our context if the protocols match.
            protocol = newProtocol;
            if ((context != null) && ((newProtocol == null) ||
                    newProtocol.equalsIgnoreCase(context.protocol))) {
                // inherit the protocol handler from the context
                // if not specified to the constructor
                if (handler == null) {
                    handler = context.handler;
                }

                // If the context is a hierarchical URL scheme and the spec
                // contains a matching scheme then maintain backwards
                // compatibility and treat it as if the spec didn't contain
                // the scheme; see 5.2.3 of RFC2396
                if (context.path != null && context.path.startsWith("/"))
                    newProtocol = null;

                if (newProtocol == null) {
                    protocol = context.protocol;
                    authority = context.authority;
                    userInfo = context.userInfo;
                    host = context.host;
                    port = context.port;
                    file = context.file;
                    path = context.path;
                    isRelative = true;
                }
            }

            if (protocol == null) {
                throw new MalformedURLException("no protocol: " + original);
            }

            // Get the protocol handler if not specified or the protocol
            // of the context could not be used
            if (handler == null &&
                    (handler = getURLStreamHandler(protocol)) == null) {
                throw new MalformedURLException("unknown protocol: " + protocol);
            }

            this.handler = handler;

            i = spec.indexOf('#', start);
            if (i >= 0) {
                ref = spec.substring(i + 1, limit);
                limit = i;
            }

            /*
             * Handle special case inheritance of query and fragment
             * implied by RFC2396 section 5.2.2.
             */
            if (isRelative && start == limit) {
                query = context.query;
                if (ref == null) {
                    ref = context.ref;
                }
            }

            handler.parseURL(this, spec, start, limit);

        } catch (MalformedURLException e) {
            throw e;
        } catch (Exception e) {
            MalformedURLException exception = new MalformedURLException(e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }

    /**
     * Sets an application's {@code URLStreamHandlerFactory}.
     * This method can be called at most once in a given Java Virtual
     * Machine.
     *
     * <p> The {@code URLStreamHandlerFactory} instance is used to
     * construct a stream protocol handler from a protocol name.
     *
     * <p> If there is a security manager, this method first calls
     * the security manager's {@code checkSetFactory} method
     * to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param fac the desired factory.
     * @throws Error             if the application has already set a factory.
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkSetFactory} method doesn't allow
     *                           the operation.
     * @see SecurityManager#checkSetFactory
     */
    public static void setURLStreamHandlerFactory(URLStreamHandlerFactory fac) {
        synchronized (streamHandlerLock) {
            if (factory != null) {
                throw new Error("factory already defined");
            }
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkSetFactory();
            }
            handlers.clear();
            factory = fac;
        }
    }

    /**
     * Returns the Stream Handler.
     *
     * @param protocol the protocol to use
     */
    static URLStreamHandler getURLStreamHandler(String protocol) {

        URLStreamHandler handler = handlers.get(protocol);
        if (handler == null) {

            boolean checkedWithFactory = false;

            // Use the factory (if any)
            if (factory != null) {
                handler = factory.createURLStreamHandler(protocol);
                checkedWithFactory = true;
            }

            // Try java protocol handler
            if (handler == null) {
                String packagePrefixList = null;

                packagePrefixList
                        = java.security.AccessController.doPrivileged(
                        new sun.security.action.GetPropertyAction(
                                protocolPathProp, ""));
                if (packagePrefixList != "") {
                    packagePrefixList += "|";
                }

                // REMIND: decide whether to allow the "null" class prefix
                // or not.
                packagePrefixList += "sun.net.www.protocol";

                StringTokenizer packagePrefixIter =
                        new StringTokenizer(packagePrefixList, "|");

                while (handler == null &&
                        packagePrefixIter.hasMoreTokens()) {

                    String packagePrefix =
                            packagePrefixIter.nextToken().trim();
                    try {
                        String clsName = packagePrefix + "." + protocol +
                                ".Handler";
                        Class<?> cls = null;
                        try {
                            cls = Class.forName(clsName);
                        } catch (ClassNotFoundException e) {
                            ClassLoader cl = ClassLoader.getSystemClassLoader();
                            if (cl != null) {
                                cls = cl.loadClass(clsName);
                            }
                        }
                        if (cls != null) {
                            handler =
                                    (URLStreamHandler) cls.newInstance();
                        }
                    } catch (Exception e) {
                        // any number of exceptions can get thrown here
                    }
                }
            }

            synchronized (streamHandlerLock) {

                URLStreamHandler handler2 = null;

                // Check again with hashtable just in case another
                // thread created a handler since we last checked
                handler2 = handlers.get(protocol);

                if (handler2 != null) {
                    return handler2;
                }

                // Check with factory if another thread set a
                // factory since our last check
                if (!checkedWithFactory && factory != null) {
                    handler2 = factory.createURLStreamHandler(protocol);
                }

                if (handler2 != null) {
                    // The handler from the factory must be given more
                    // importance. Discard the default handler that
                    // this thread created.
                    handler = handler2;
                }

                // Insert this handler into the hashtable
                if (handler != null) {
                    handlers.put(protocol, handler);
                }

            }
        }

        return handler;

    }

    /*
     * Returns true if specified string is a valid protocol name.
     */
    private boolean isValidProtocol(String protocol) {
        int len = protocol.length();
        if (len < 1)
            return false;
        char c = protocol.charAt(0);
        if (!Character.isLetter(c))
            return false;
        for (int i = 1; i < len; i++) {
            c = protocol.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '.' && c != '+' &&
                    c != '-') {
                return false;
            }
        }
        return true;
    }

    /*
     * Checks for permission to specify a stream handler.
     */
    private void checkSpecifyHandler(SecurityManager sm) {
        sm.checkPermission(SecurityConstants.SPECIFY_HANDLER_PERMISSION);
    }

    /**
     * Sets the fields of the URL. This is not a public method so that
     * only URLStreamHandlers can modify URL fields. URLs are
     * otherwise constant.
     *
     * @param protocol the name of the protocol to use
     * @param host     the name of the host
     * @param port     the port number on the host
     * @param file     the file on the host
     * @param ref      the internal reference in the URL
     */
    void set(String protocol, String host, int port,
             String file, String ref) {
        synchronized (this) {
            this.protocol = protocol;
            this.host = host;
            authority = port == -1 ? host : host + ":" + port;
            this.port = port;
            this.file = file;
            this.ref = ref;
            /* This is very important. We must recompute this after the
             * URL has been changed. */
            hashCode = -1;
            hostAddress = null;
            int q = file.lastIndexOf('?');
            if (q != -1) {
                query = file.substring(q + 1);
                path = file.substring(0, q);
            } else
                path = file;
        }
    }

    /**
     * Sets the specified 8 fields of the URL. This is not a public method so
     * that only URLStreamHandlers can modify URL fields. URLs are otherwise
     * constant.
     *
     * @param protocol  the name of the protocol to use
     * @param host      the name of the host
     * @param port      the port number on the host
     * @param authority the authority part for the url
     * @param userInfo  the username and password
     * @param path      the file on the host
     * @param ref       the internal reference in the URL
     * @param query     the query part of this URL
     * @since 1.3
     */
    void set(String protocol, String host, int port,
             String authority, String userInfo, String path,
             String query, String ref) {
        synchronized (this) {
            this.protocol = protocol;
            this.host = host;
            this.port = port;
            this.file = query == null ? path : path + "?" + query;
            this.userInfo = userInfo;
            this.path = path;
            this.ref = ref;
            /* This is very important. We must recompute this after the
             * URL has been changed. */
            hashCode = -1;
            hostAddress = null;
            this.query = query;
            this.authority = authority;
        }
    }

    /**
     * Gets the query part of this {@code URL}.
     *
     * @return the query part of this {@code URL},
     * or <CODE>null</CODE> if one does not exist
     * @since 1.3
     */
    public String getQuery() {
        return query;
    }

    /**
     * Gets the path part of this {@code URL}.
     *
     * @return the path part of this {@code URL}, or an
     * empty string if one does not exist
     * @since 1.3
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the userInfo part of this {@code URL}.
     *
     * @return the userInfo part of this {@code URL}, or
     * <CODE>null</CODE> if one does not exist
     * @since 1.3
     */
    public String getUserInfo() {
        return userInfo;
    }

    /**
     * Gets the authority part of this {@code URL}.
     *
     * @return the authority part of this {@code URL}
     * @since 1.3
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * Gets the port number of this {@code URL}.
     *
     * @return the port number, or -1 if the port is not set
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the default port number of the protocol associated
     * with this {@code URL}. If the URL scheme or the URLStreamHandler
     * for the URL do not define a default port number,
     * then -1 is returned.
     *
     * @return the port number
     * @since 1.4
     */
    public int getDefaultPort() {
        return handler.getDefaultPort();
    }

    /**
     * Gets the protocol name of this {@code URL}.
     *
     * @return the protocol of this {@code URL}.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Gets the host name of this {@code URL}, if applicable.
     * The format of the host conforms to RFC 2732, i.e. for a
     * literal IPv6 address, this method will return the IPv6 address
     * enclosed in square brackets ({@code '['} and {@code ']'}).
     *
     * @return the host name of this {@code URL}.
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the file name of this {@code URL}.
     * The returned file portion will be
     * the same as <CODE>getPath()</CODE>, plus the concatenation of
     * the value of <CODE>getQuery()</CODE>, if any. If there is
     * no query portion, this method and <CODE>getPath()</CODE> will
     * return identical results.
     *
     * @return the file name of this {@code URL},
     * or an empty string if one does not exist
     */
    public String getFile() {
        return file;
    }

    /**
     * Gets the anchor (also known as the "reference") of this
     * {@code URL}.
     *
     * @return the anchor (also known as the "reference") of this
     * {@code URL}, or <CODE>null</CODE> if one does not exist
     */
    public String getRef() {
        return ref;
    }

    /**
     * Compares this URL for equality with another object.<p>
     * <p>
     * If the given object is not a URL then this method immediately returns
     * {@code false}.<p>
     * <p>
     * Two URL objects are equal if they have the same protocol, reference
     * equivalent hosts, have the same port number on the host, and the same
     * file and fragment of the file.<p>
     * <p>
     * Two hosts are considered equivalent if both host names can be resolved
     * into the same IP addresses; else if either host name can't be
     * resolved, the host names must be equal without regard to case; or both
     * host names equal to null.<p>
     * <p>
     * Since hosts comparison requires name resolution, this operation is a
     * blocking operation. <p>
     * <p>
     * Note: The defined behavior for {@code equals} is known to
     * be inconsistent with virtual hosting in HTTP.
     *
     * @param obj the URL to compare against.
     * @return {@code true} if the objects are the same;
     * {@code false} otherwise.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof URL))
            return false;
        URL u2 = (URL) obj;

        return handler.equals(this, u2);
    }

    /**
     * Creates an integer suitable for hash table indexing.<p>
     * <p>
     * The hash code is based upon all the URL components relevant for URL
     * comparison. As such, this operation is a blocking operation.<p>
     *
     * @return a hash code for this {@code URL}.
     */
    public synchronized int hashCode() {
        if (hashCode != -1)
            return hashCode;

        hashCode = handler.hashCode(this);
        return hashCode;
    }

    /**
     * Compares two URLs, excluding the fragment component.<p>
     * <p>
     * Returns {@code true} if this {@code URL} and the
     * {@code other} argument are equal without taking the
     * fragment component into consideration.
     *
     * @param other the {@code URL} to compare against.
     * @return {@code true} if they reference the same remote object;
     * {@code false} otherwise.
     */
    public boolean sameFile(URL other) {
        return handler.sameFile(this, other);
    }

    /**
     * Constructs a string representation of this {@code URL}. The
     * string is created by calling the {@code toExternalForm}
     * method of the stream protocol handler for this object.
     *
     * @return a string representation of this object.
     */
    public String toString() {
        return toExternalForm();
    }

    /**
     * Constructs a string representation of this {@code URL}. The
     * string is created by calling the {@code toExternalForm}
     * method of the stream protocol handler for this object.
     *
     * @return a string representation of this object.
     */
    public String toExternalForm() {
        return handler.toExternalForm(this);
    }

    public URI toURI() throws URISyntaxException {
        return new URI(toString());
    }

    public URLConnection openConnection() throws java.io.IOException {
        return handler.openConnection(this);
    }

    /**
     * Same as {@link #openConnection()}, except that the connection will be
     * made through the specified proxy; Protocol handlers that do not
     * support proxing will ignore the proxy parameter and make a
     * normal connection.
     * <p>
     * Invoking this method preempts the system's default ProxySelector
     * settings.
     *
     * @param proxy the Proxy through which this connection
     *              will be made. If direct connection is desired,
     *              Proxy.NO_PROXY should be specified.
     * @return a {@code URLConnection} to the URL.
     * @throws IOException                   if an I/O exception occurs.
     * @throws SecurityException             if a security manager is present
     *                                       and the caller doesn't have permission to connect
     *                                       to the proxy.
     * @throws IllegalArgumentException      will be thrown if proxy is null,
     *                                       or proxy has the wrong type
     * @throws UnsupportedOperationException if the subclass that
     *                                       implements the protocol handler doesn't support
     *                                       this method.
     * @since 1.5
     */
    public URLConnection openConnection(Proxy proxy)
            throws java.io.IOException {
        if (proxy == null) {
            throw new IllegalArgumentException("proxy can not be null");
        }

        // Create a copy of Proxy as a security measure
        Proxy p = proxy == Proxy.NO_PROXY ? Proxy.NO_PROXY : ApplicationProxy.create(proxy);
        SecurityManager sm = System.getSecurityManager();
        if (p.type() != Proxy.Type.DIRECT && sm != null) {
            InetSocketAddress epoint = (InetSocketAddress) p.address();
            if (epoint.isUnresolved())
                sm.checkConnect(epoint.getHostName(), epoint.getPort());
            else
                sm.checkConnect(epoint.getAddress().getHostAddress(),
                        epoint.getPort());
        }
        return handler.openConnection(this, p);
    }

    /**
     * Opens a connection to this {@code URL} and returns an
     * {@code InputStream} for reading from that connection. This
     * method is a shorthand for:
     * <blockquote><pre>
     *     openConnection().getInputStream()
     * </pre></blockquote>
     *
     * @return an input stream for reading from the URL connection.
     * @throws IOException if an I/O exception occurs.
     */
    public final InputStream openStream() throws java.io.IOException {
        return openConnection().getInputStream();
    }

    /**
     * Gets the contents of this URL. This method is a shorthand for:
     * <blockquote><pre>
     *     openConnection().getContent()
     * </pre></blockquote>
     *
     * @return the contents of this URL.
     * @throws IOException if an I/O exception occurs.
     */
    public final Object getContent() throws java.io.IOException {
        return openConnection().getContent();
    }

    /**
     * Gets the contents of this URL. This method is a shorthand for:
     * <blockquote><pre>
     *     openConnection().getContent(Class[])
     * </pre></blockquote>
     *
     * @param classes an array of Java types
     * @return the content object of this URL that is the first match of
     * the types specified in the classes array.
     * null if none of the requested types are supported.
     * @throws IOException if an I/O exception occurs.
     * @since 1.3
     */
    public final Object getContent(Class[] classes)
            throws java.io.IOException {
        return openConnection().getContent(classes);
    }

    /**
     * WriteObject is called to save the state of the URL to an
     * ObjectOutputStream. The handler is not saved since it is
     * specific to this system.
     *
     * @serialData the default write object value. When read back in,
     * the reader must ensure that calling getURLStreamHandler with
     * the protocol variable returns a valid URLStreamHandler and
     * throw an IOException if it does not.
     */
    private synchronized void writeObject(java.io.ObjectOutputStream s)
            throws IOException {
        s.defaultWriteObject(); // write the fields
    }

    /**
     * readObject is called to restore the state of the URL from the
     * stream.  It reads the components of the URL and finds the local
     * stream handler.
     */
    private synchronized void readObject(java.io.ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();  // read the fields
        if ((handler = getURLStreamHandler(protocol)) == null) {
            throw new IOException("unknown protocol: " + protocol);
        }

        // Construct authority part
        if (authority == null &&
                ((host != null && host.length() > 0) || port != -1)) {
            if (host == null)
                host = "";
            authority = (port == -1) ? host : host + ":" + port;

            // Handle hosts with userInfo in them
            int at = host.lastIndexOf('@');
            if (at != -1) {
                userInfo = host.substring(0, at);
                host = host.substring(at + 1);
            }
        } else if (authority != null) {
            // Construct user info part
            int ind = authority.indexOf('@');
            if (ind != -1)
                userInfo = authority.substring(0, ind);
        }

        // Construct path and query part
        path = null;
        query = null;
        if (file != null) {
            // Fix: only do this if hierarchical?
            int q = file.lastIndexOf('?');
            if (q != -1) {
                query = file.substring(q + 1);
                path = file.substring(0, q);
            } else
                path = file;
        }
    }
}

