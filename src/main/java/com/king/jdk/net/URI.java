package com.king.jdk.net;


import com.king.jdk.net.exception.MalformedURLException;
import com.king.jdk.net.exception.URISyntaxException;
import sun.nio.cs.ThreadLocalCoders;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.text.Normalizer;

public final class URI
        implements Comparable<URI>, Serializable {

    // Note: Comments containing the word "ASSERT" indicate places where a
    // throw of an InternalError should be replaced by an appropriate assertion
    // statement once asserts are enabled in the build.

    static final long serialVersionUID = -6052424284110960213L;


    // -- Properties and components of this instance --
    // digit    = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" |
    //            "8" | "9"
    private static final long L_DIGIT = lowMask('0', '9');
    private static final long H_DIGIT = 0L;
    // upalpha  = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" |
    //            "J" | "K" | "L" | "M" | "N" | "O" | "P" | "Q" | "R" |
    //            "S" | "T" | "U" | "V" | "W" | "X" | "Y" | "Z"
    private static final long L_UPALPHA = 0L;
    private static final long H_UPALPHA = highMask('A', 'Z');
    // lowalpha = "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" |
    //            "j" | "k" | "l" | "m" | "n" | "o" | "p" | "q" | "r" |
    //            "s" | "t" | "u" | "v" | "w" | "x" | "y" | "z"
    private static final long L_LOWALPHA = 0L;
    private static final long H_LOWALPHA = highMask('a', 'z');
    // alpha         = lowalpha | upalpha
    private static final long L_ALPHA = L_LOWALPHA | L_UPALPHA;
    private static final long H_ALPHA = H_LOWALPHA | H_UPALPHA;

    // The remaining fields may be computed on demand
    // alphanum      = alpha | digit
    private static final long L_ALPHANUM = L_DIGIT | L_ALPHA;
    private static final long H_ALPHANUM = H_DIGIT | H_ALPHA;
    // hex           = digit | "A" | "B" | "C" | "D" | "E" | "F" |
    //                         "a" | "b" | "c" | "d" | "e" | "f"
    private static final long L_HEX = L_DIGIT;
    private static final long H_HEX = highMask('A', 'F') | highMask('a', 'f');
    // mark          = "-" | "_" | "." | "!" | "~" | "*" | "'" |
    //                 "(" | ")"
    private static final long L_MARK = lowMask("-_.!~*'()");
    private static final long H_MARK = highMask("-_.!~*'()");
    // unreserved    = alphanum | mark
    private static final long L_UNRESERVED = L_ALPHANUM | L_MARK;
    private static final long H_UNRESERVED = H_ALPHANUM | H_MARK;
    // reserved      = ";" | "/" | "?" | ":" | "@" | "&" | "=" | "+" |
    //                 "$" | "," | "[" | "]"
    // Added per RFC2732: "[", "]"
    private static final long L_RESERVED = lowMask(";/?:@&=+$,[]");


    // -- Constructors and factories --
    private static final long H_RESERVED = highMask(";/?:@&=+$,[]");
    // The zero'th bit is used to indicate that escape pairs and non-US-ASCII
    // characters are allowed; this is handled by the scanEscape method below.
    private static final long L_ESCAPED = 1L;
    private static final long H_ESCAPED = 0L;
    // uric          = reserved | unreserved | escaped
    private static final long L_URIC = L_RESERVED | L_UNRESERVED | L_ESCAPED;
    private static final long H_URIC = H_RESERVED | H_UNRESERVED | H_ESCAPED;
    // pchar         = unreserved | escaped |
    //                 ":" | "@" | "&" | "=" | "+" | "$" | ","
    private static final long L_PCHAR
            = L_UNRESERVED | L_ESCAPED | lowMask(":@&=+$,");
    private static final long H_PCHAR
            = H_UNRESERVED | H_ESCAPED | highMask(":@&=+$,");


    // -- Operations --
    // All valid path characters
    private static final long L_PATH = L_PCHAR | lowMask(";/");
    private static final long H_PATH = H_PCHAR | highMask(";/");
    // Dash, for use in domainlabel and toplabel
    private static final long L_DASH = lowMask("-");
    private static final long H_DASH = highMask("-");
    // Dot, for use in hostnames
    private static final long L_DOT = lowMask(".");
    private static final long H_DOT = highMask(".");

    // -- Component access methods --
    // userinfo      = *( unreserved | escaped |
    //                    ";" | ":" | "&" | "=" | "+" | "$" | "," )
    private static final long L_USERINFO
            = L_UNRESERVED | L_ESCAPED | lowMask(";:&=+$,");
    private static final long H_USERINFO
            = H_UNRESERVED | H_ESCAPED | highMask(";:&=+$,");
    // reg_name      = 1*( unreserved | escaped | "$" | "," |
    //                     ";" | ":" | "@" | "&" | "=" | "+" )
    private static final long L_REG_NAME
            = L_UNRESERVED | L_ESCAPED | lowMask("$,;:@&=+");
    private static final long H_REG_NAME
            = H_UNRESERVED | H_ESCAPED | highMask("$,;:@&=+");
    // All valid characters for server-based authorities
    private static final long L_SERVER
            = L_USERINFO | L_ALPHANUM | L_DASH | lowMask(".:@[]");
    private static final long H_SERVER
            = H_USERINFO | H_ALPHANUM | H_DASH | highMask(".:@[]");
    // Special case of server authority that represents an IPv6 address
    // In this case, a % does not signify an escape sequence
    private static final long L_SERVER_PERCENT
            = L_SERVER | lowMask("%");
    private static final long H_SERVER_PERCENT
            = H_SERVER | highMask("%");
    private static final long L_LEFT_BRACKET = lowMask("[");
    private static final long H_LEFT_BRACKET = highMask("[");
    // scheme        = alpha *( alpha | digit | "+" | "-" | "." )
    private static final long L_SCHEME = L_ALPHA | L_DIGIT | lowMask("+-.");
    private static final long H_SCHEME = H_ALPHA | H_DIGIT | highMask("+-.");
    // uric_no_slash = unreserved | escaped | ";" | "?" | ":" | "@" |
    //                 "&" | "=" | "+" | "$" | ","
    private static final long L_URIC_NO_SLASH
            = L_UNRESERVED | L_ESCAPED | lowMask(";?:@&=+$,");
    private static final long H_URIC_NO_SLASH
            = H_UNRESERVED | H_ESCAPED | highMask(";?:@&=+$,");
    private final static char[] hexDigits = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    // Components of all URIs: [<scheme>:]<scheme-specific-part>[#<fragment>]
    private transient String scheme;            // null ==> relative URI
    private transient String fragment;


    // -- Equality, comparison, hash code, toString, and serialization --
    // Hierarchical URI components: [//<authority>]<path>[?<query>]
    private transient String authority;         // Registry or server
    // Server-based authority: [<userInfo>@]<host>[:<port>]
    private transient String userInfo;
    private transient String host;              // null ==> registry-based
    private transient int port = -1;            // -1 ==> undefined
    // Remaining components of hierarchical URIs
    private transient String path;              // null ==> opaque


    // -- Serialization support --
    private transient String query;
    private volatile transient String schemeSpecificPart;


    // -- End of public methods --


    // -- Utility methods for string-field comparison and hashing --

    // These methods return appropriate values for null string arguments,
    // thereby simplifying the equals, hashCode, and compareTo methods.
    //
    // The case-ignoring methods should only be applied to strings whose
    // characters are all known to be US-ASCII.  Because of this restriction,
    // these methods are faster than the similar methods in the String class.
    private volatile transient int hash;        // Zero ==> undefined
    private volatile transient String decodedUserInfo = null;
    private volatile transient String decodedAuthority = null;
    private volatile transient String decodedPath = null;
    private volatile transient String decodedQuery = null;
    private volatile transient String decodedFragment = null;
    private volatile transient String decodedSchemeSpecificPart = null;

    private volatile String string;             // The only serializable field

    private URI() {
    }                           // Used internally


    // -- String construction --

    public URI(String str) throws URISyntaxException {
        new Parser(str).parse(false);
    }

    public URI(String scheme,
               String userInfo, String host, int port,
               String path, String query, String fragment)
            throws URISyntaxException {
        String s = toString(scheme, null,
                null, userInfo, host, port,
                path, query, fragment);
        checkPath(s, scheme, path);
        new Parser(s).parse(true);
    }

    public URI(String scheme,
               String authority,
               String path, String query, String fragment)
            throws URISyntaxException {
        String s = toString(scheme, null,
                authority, null, null, -1,
                path, query, fragment);
        checkPath(s, scheme, path);
        new Parser(s).parse(false);
    }

    public URI(String scheme, String host, String path, String fragment)
            throws URISyntaxException {
        this(scheme, null, host, -1, path, null, fragment);
    }

    public URI(String scheme, String ssp, String fragment)
            throws URISyntaxException {
        new Parser(toString(scheme, ssp,
                null, null, null, -1,
                null, null, fragment))
                .parse(false);
    }

    public static URI create(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException x) {
            throw new IllegalArgumentException(x.getMessage(), x);
        }
    }

    // US-ASCII only
    private static int toLower(char c) {
        if ((c >= 'A') && (c <= 'Z'))
            return c + ('a' - 'A');
        return c;
    }


    // -- Normalization, resolution, and relativization --

    // US-ASCII only
    private static int toUpper(char c) {
        if ((c >= 'a') && (c <= 'z'))
            return c - ('a' - 'A');
        return c;
    }

    private static boolean equal(String s, String t) {
        if (s == t) return true;
        if ((s != null) && (t != null)) {
            if (s.length() != t.length())
                return false;
            if (s.indexOf('%') < 0)
                return s.equals(t);
            int n = s.length();
            for (int i = 0; i < n; ) {
                char c = s.charAt(i);
                char d = t.charAt(i);
                if (c != '%') {
                    if (c != d)
                        return false;
                    i++;
                    continue;
                }
                if (d != '%')
                    return false;
                i++;
                if (toLower(s.charAt(i)) != toLower(t.charAt(i)))
                    return false;
                i++;
                if (toLower(s.charAt(i)) != toLower(t.charAt(i)))
                    return false;
                i++;
            }
            return true;
        }
        return false;
    }

    // US-ASCII only
    private static boolean equalIgnoringCase(String s, String t) {
        if (s == t) return true;
        if ((s != null) && (t != null)) {
            int n = s.length();
            if (t.length() != n)
                return false;
            for (int i = 0; i < n; i++) {
                if (toLower(s.charAt(i)) != toLower(t.charAt(i)))
                    return false;
            }
            return true;
        }
        return false;
    }

    private static int hash(int hash, String s) {
        if (s == null) return hash;
        return s.indexOf('%') < 0 ? hash * 127 + s.hashCode()
                : normalizedHash(hash, s);
    }



    private static int normalizedHash(int hash, String s) {
        int h = 0;
        for (int index = 0; index < s.length(); index++) {
            char ch = s.charAt(index);
            h = 31 * h + ch;
            if (ch == '%') {
                /*
                 * Process the next two encoded characters
                 */
                for (int i = index + 1; i < index + 3; i++)
                    h = 31 * h + toUpper(s.charAt(i));
                index += 2;
            }
        }
        return hash * 127 + h;
    }

    // US-ASCII only
    private static int hashIgnoringCase(int hash, String s) {
        if (s == null) return hash;
        int h = hash;
        int n = s.length();
        for (int i = 0; i < n; i++)
            h = 31 * h + toLower(s.charAt(i));
        return h;
    }

    private static int compare(String s, String t) {
        if (s == t) return 0;
        if (s != null) {
            if (t != null)
                return s.compareTo(t);
            else
                return +1;
        } else {
            return -1;
        }
    }

    // US-ASCII only
    private static int compareIgnoringCase(String s, String t) {
        if (s == t) return 0;
        if (s != null) {
            if (t != null) {
                int sn = s.length();
                int tn = t.length();
                int n = sn < tn ? sn : tn;
                for (int i = 0; i < n; i++) {
                    int c = toLower(s.charAt(i)) - toLower(t.charAt(i));
                    if (c != 0)
                        return c;
                }
                return sn - tn;
            }
            return +1;
        } else {
            return -1;
        }
    }

    // If a scheme is given then the path, if given, must be absolute
    //
    private static void checkPath(String s, String scheme, String path)
            throws URISyntaxException {
        if (scheme != null) {
            if ((path != null)
                    && ((path.length() > 0) && (path.charAt(0) != '/')))
                throw new URISyntaxException(s,
                        "Relative path in absolute URI");
        }
    }

    // RFC2396 5.2 (6)
    private static String resolvePath(String base, String child,
                                      boolean absolute) {
        int i = base.lastIndexOf('/');
        int cn = child.length();
        String path = "";

        if (cn == 0) {
            // 5.2 (6a)
            if (i >= 0)
                path = base.substring(0, i + 1);
        } else {
            StringBuffer sb = new StringBuffer(base.length() + cn);
            // 5.2 (6a)
            if (i >= 0)
                sb.append(base.substring(0, i + 1));
            // 5.2 (6b)
            sb.append(child);
            path = sb.toString();
        }

        // 5.2 (6c-f)
        String np = normalize(path);

        // 5.2 (6g): If the result is absolute but the path begins with "../",
        // then we simply leave the path as-is

        return np;
    }



    // RFC2396 5.2
    private static URI resolve(URI base, URI child) {
        // check if child if opaque first so that NPE is thrown
        // if child is null.
        if (child.isOpaque() || base.isOpaque())
            return child;

        // 5.2 (2): Reference to current document (lone fragment)
        if ((child.scheme == null) && (child.authority == null)
                && child.path.equals("") && (child.fragment != null)
                && (child.query == null)) {
            if ((base.fragment != null)
                    && child.fragment.equals(base.fragment)) {
                return base;
            }
            URI ru = new URI();
            ru.scheme = base.scheme;
            ru.authority = base.authority;
            ru.userInfo = base.userInfo;
            ru.host = base.host;
            ru.port = base.port;
            ru.path = base.path;
            ru.fragment = child.fragment;
            ru.query = base.query;
            return ru;
        }

        // 5.2 (3): Child is absolute
        if (child.scheme != null)
            return child;

        URI ru = new URI();             // Resolved URI
        ru.scheme = base.scheme;
        ru.query = child.query;
        ru.fragment = child.fragment;

        // 5.2 (4): Authority
        if (child.authority == null) {
            ru.authority = base.authority;
            ru.host = base.host;
            ru.userInfo = base.userInfo;
            ru.port = base.port;

            String cp = (child.path == null) ? "" : child.path;
            if ((cp.length() > 0) && (cp.charAt(0) == '/')) {
                // 5.2 (5): Child path is absolute
                ru.path = child.path;
            } else {
                // 5.2 (6): Resolve relative path
                ru.path = resolvePath(base.path, cp, base.isAbsolute());
            }
        } else {
            ru.authority = child.authority;
            ru.host = child.host;
            ru.userInfo = child.userInfo;
            ru.host = child.host;
            ru.port = child.port;
            ru.path = child.path;
        }

        // 5.2 (7): Recombine (nothing to do here)
        return ru;
    }

    // If the given URI's path is normal then return the URI;
    // o.w., return a new URI containing the normalized path.
    //
    private static URI normalize(URI u) {
        if (u.isOpaque() || (u.path == null) || (u.path.length() == 0))
            return u;

        String np = normalize(u.path);
        if (np == u.path)
            return u;

        URI v = new URI();
        v.scheme = u.scheme;
        v.fragment = u.fragment;
        v.authority = u.authority;
        v.userInfo = u.userInfo;
        v.host = u.host;
        v.port = u.port;
        v.path = np;
        v.query = u.query;
        return v;
    }

    // If both URIs are hierarchical, their scheme and authority components are
    // identical, and the base path is a prefix of the child's path, then
    // return a relative URI that, when resolved against the base, yields the
    // child; otherwise, return the child.
    //
    private static URI relativize(URI base, URI child) {
        // check if child if opaque first so that NPE is thrown
        // if child is null.
        if (child.isOpaque() || base.isOpaque())
            return child;
        if (!equalIgnoringCase(base.scheme, child.scheme)
                || !equal(base.authority, child.authority))
            return child;

        String bp = normalize(base.path);
        String cp = normalize(child.path);
        if (!bp.equals(cp)) {
            if (!bp.endsWith("/"))
                bp = bp + "/";
            if (!cp.startsWith(bp))
                return child;
        }

        URI v = new URI();
        v.path = cp.substring(bp.length());
        v.query = child.query;
        v.fragment = child.fragment;
        return v;
    }


    static private int needsNormalization(String path) {
        boolean normal = true;
        int ns = 0;                     // Number of segments
        int end = path.length() - 1;    // Index of last char in path
        int p = 0;                      // Index of next char in path

        // Skip initial slashes
        while (p <= end) {
            if (path.charAt(p) != '/') break;
            p++;
        }
        if (p > 1) normal = false;

        // Scan segments
        while (p <= end) {

            // Looking at "." or ".." ?
            if ((path.charAt(p) == '.')
                    && ((p == end)
                    || ((path.charAt(p + 1) == '/')
                    || ((path.charAt(p + 1) == '.')
                    && ((p + 1 == end)
                    || (path.charAt(p + 2) == '/')))))) {
                normal = false;
            }
            ns++;

            // Find beginning of next segment
            while (p <= end) {
                if (path.charAt(p++) != '/')
                    continue;

                // Skip redundant slashes
                while (p <= end) {
                    if (path.charAt(p) != '/') break;
                    normal = false;
                    p++;
                }

                break;
            }
        }

        return normal ? -1 : ns;
    }


    static private void split(char[] path, int[] segs) {
        int end = path.length - 1;      // Index of last char in path
        int p = 0;                      // Index of next char in path
        int i = 0;                      // Index of current segment

        // Skip initial slashes
        while (p <= end) {
            if (path[p] != '/') break;
            path[p] = '\0';
            p++;
        }

        while (p <= end) {

            // Note start of segment
            segs[i++] = p++;

            // Find beginning of next segment
            while (p <= end) {
                if (path[p++] != '/')
                    continue;
                path[p - 1] = '\0';

                // Skip redundant slashes
                while (p <= end) {
                    if (path[p] != '/') break;
                    path[p++] = '\0';
                }
                break;
            }
        }

        if (i != segs.length)
            throw new InternalError();  // ASSERT
    }


    static private int join(char[] path, int[] segs) {
        int ns = segs.length;           // Number of segments
        int end = path.length - 1;      // Index of last char in path
        int p = 0;                      // Index of next path char to write

        if (path[p] == '\0') {
            // Restore initial slash for absolute paths
            path[p++] = '/';
        }

        for (int i = 0; i < ns; i++) {
            int q = segs[i];            // Current segment
            if (q == -1)
                // Ignore this segment
                continue;

            if (p == q) {
                // We're already at this segment, so just skip to its end
                while ((p <= end) && (path[p] != '\0'))
                    p++;
                if (p <= end) {
                    // Preserve trailing slash
                    path[p++] = '/';
                }
            } else if (p < q) {
                // Copy q down to p
                while ((q <= end) && (path[q] != '\0'))
                    path[p++] = path[q++];
                if (q <= end) {
                    // Preserve trailing slash
                    path[p++] = '/';
                }
            } else
                throw new InternalError(); // ASSERT false
        }

        return p;
    }

    // Remove "." segments from the given path, and remove segment pairs
    // consisting of a non-".." segment followed by a ".." segment.
    //
    private static void removeDots(char[] path, int[] segs) {
        int ns = segs.length;
        int end = path.length - 1;

        for (int i = 0; i < ns; i++) {
            int dots = 0;               // Number of dots found (0, 1, or 2)

            // Find next occurrence of "." or ".."
            do {
                int p = segs[i];
                if (path[p] == '.') {
                    if (p == end) {
                        dots = 1;
                        break;
                    } else if (path[p + 1] == '\0') {
                        dots = 1;
                        break;
                    } else if ((path[p + 1] == '.')
                            && ((p + 1 == end)
                            || (path[p + 2] == '\0'))) {
                        dots = 2;
                        break;
                    }
                }
                i++;
            } while (i < ns);
            if ((i > ns) || (dots == 0))
                break;

            if (dots == 1) {
                // Remove this occurrence of "."
                segs[i] = -1;
            } else {
                // If there is a preceding non-".." segment, remove both that
                // segment and this occurrence of ".."; otherwise, leave this
                // ".." segment as-is.
                int j;
                for (j = i - 1; j >= 0; j--) {
                    if (segs[j] != -1) break;
                }
                if (j >= 0) {
                    int q = segs[j];
                    if (!((path[q] == '.')
                            && (path[q + 1] == '.')
                            && (path[q + 2] == '\0'))) {
                        segs[i] = -1;
                        segs[j] = -1;
                    }
                }
            }
        }
    }

    // DEVIATION: If the normalized path is relative, and if the first
    // segment could be parsed as a scheme name, then prepend a "." segment
    //
    private static void maybeAddLeadingDot(char[] path, int[] segs) {

        if (path[0] == '\0')
            // The path is absolute
            return;

        int ns = segs.length;
        int f = 0;                      // Index of first segment
        while (f < ns) {
            if (segs[f] >= 0)
                break;
            f++;
        }
        if ((f >= ns) || (f == 0))
            // The path is empty, or else the original first segment survived,
            // in which case we already know that no leading "." is needed
            return;

        int p = segs[f];
        while ((p < path.length) && (path[p] != ':') && (path[p] != '\0')) p++;
        if (p >= path.length || path[p] == '\0')
            // No colon in first segment, so no "." needed
            return;

        // At this point we know that the first segment is unused,
        // hence we can insert a "." segment at that position
        path[0] = '.';
        path[1] = '\0';
        segs[0] = 0;
    }

    // Normalize the given path string.  A normal path string has no empty
    // segments (i.e., occurrences of "//"), no segments equal to ".", and no
    // segments equal to ".." that are preceded by a segment not equal to "..".
    // In contrast to Unix-style pathname normalization, for URI paths we
    // always retain trailing slashes.
    //
    private static String normalize(String ps) {

        // Does this path need normalization?
        int ns = needsNormalization(ps);        // Number of segments
        if (ns < 0)
            // Nope -- just return it
            return ps;

        char[] path = ps.toCharArray();         // Path in char-array form

        // Split path into segments
        int[] segs = new int[ns];               // Segment-index array
        split(path, segs);

        // Remove dots
        removeDots(path, segs);

        // Prevent scheme-name confusion
        maybeAddLeadingDot(path, segs);

        // Join the remaining segments and return the result
        String s = new String(path, 0, join(path, segs));
        if (s.equals(ps)) {
            // string was already normalized
            return ps;
        }
        return s;
    }

    // Compute the low-order mask for the characters in the given string
    private static long lowMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if (c < 64)
                m |= (1L << c);
        }
        return m;
    }

    // Compute the high-order mask for the characters in the given string
    private static long highMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if ((c >= 64) && (c < 128))
                m |= (1L << (c - 64));
        }
        return m;
    }

    // Compute a low-order mask for the characters
    // between first and last, inclusive
    private static long lowMask(char first, char last) {
        long m = 0;
        int f = Math.max(Math.min(first, 63), 0);
        int l = Math.max(Math.min(last, 63), 0);
        for (int i = f; i <= l; i++)
            m |= 1L << i;
        return m;
    }

    // Compute a high-order mask for the characters
    // between first and last, inclusive
    private static long highMask(char first, char last) {
        long m = 0;
        int f = Math.max(Math.min(first, 127), 64) - 64;
        int l = Math.max(Math.min(last, 127), 64) - 64;
        for (int i = f; i <= l; i++)
            m |= 1L << i;
        return m;
    }

    // Tell whether the given character is permitted by the given mask pair
    private static boolean match(char c, long lowMask, long highMask) {
        if (c == 0) // 0 doesn't have a slot in the mask. So, it never matches.
            return false;
        if (c < 64)
            return ((1L << c) & lowMask) != 0;
        if (c < 128)
            return ((1L << (c - 64)) & highMask) != 0;
        return false;
    }

    private static void appendEscape(StringBuffer sb, byte b) {
        sb.append('%');
        sb.append(hexDigits[(b >> 4) & 0x0f]);
        sb.append(hexDigits[(b >> 0) & 0x0f]);
    }

    private static void appendEncoded(StringBuffer sb, char c) {
        ByteBuffer bb = null;
        try {
            bb = ThreadLocalCoders.encoderFor("UTF-8")
                    .encode(CharBuffer.wrap("" + c));
        } catch (CharacterCodingException x) {
            assert false;
        }
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xff;
            if (b >= 0x80)
                appendEscape(sb, (byte) b);
            else
                sb.append((char) b);
        }
    }

    // Quote any characters in s that are not permitted
    // by the given mask pair
    //
    private static String quote(String s, long lowMask, long highMask) {
        int n = s.length();
        StringBuffer sb = null;
        boolean allowNonASCII = ((lowMask & L_ESCAPED) != 0);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '\u0080') {
                if (!match(c, lowMask, highMask)) {
                    if (sb == null) {
                        sb = new StringBuffer();
                        sb.append(s.substring(0, i));
                    }
                    appendEscape(sb, (byte) c);
                } else {
                    if (sb != null)
                        sb.append(c);
                }
            } else if (allowNonASCII
                    && (Character.isSpaceChar(c)
                    || Character.isISOControl(c))) {
                if (sb == null) {
                    sb = new StringBuffer();
                    sb.append(s.substring(0, i));
                }
                appendEncoded(sb, c);
            } else {
                if (sb != null)
                    sb.append(c);
            }
        }
        return (sb == null) ? s : sb.toString();
    }

    // Encodes all characters >= \u0080 into escaped, normalized UTF-8 octets,
    // assuming that s is otherwise legal
    //
    private static String encode(String s) {
        int n = s.length();
        if (n == 0)
            return s;

        // First check whether we actually need to encode
        for (int i = 0; ; ) {
            if (s.charAt(i) >= '\u0080')
                break;
            if (++i >= n)
                return s;
        }

        String ns = Normalizer.normalize(s, Normalizer.Form.NFC);
        ByteBuffer bb = null;
        try {
            bb = ThreadLocalCoders.encoderFor("UTF-8")
                    .encode(CharBuffer.wrap(ns));
        } catch (CharacterCodingException x) {
            assert false;
        }

        StringBuffer sb = new StringBuffer();
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xff;
            if (b >= 0x80)
                appendEscape(sb, (byte) b);
            else
                sb.append((char) b);
        }
        return sb.toString();
    }

    private static int decode(char c) {
        if ((c >= '0') && (c <= '9'))
            return c - '0';
        if ((c >= 'a') && (c <= 'f'))
            return c - 'a' + 10;
        if ((c >= 'A') && (c <= 'F'))
            return c - 'A' + 10;
        assert false;
        return -1;
    }

    private static byte decode(char c1, char c2) {
        return (byte) (((decode(c1) & 0xf) << 4)
                | ((decode(c2) & 0xf) << 0));
    }

    // Evaluates all escapes in s, applying UTF-8 decoding if needed.  Assumes
    // that escapes are well-formed syntactically, i.e., of the form %XX.  If a
    // sequence of escaped octets is not valid UTF-8 then the erroneous octets
    // are replaced with '\uFFFD'.
    // Exception: any "%" found between "[]" is left alone. It is an IPv6 literal
    //            with a scope_id
    //
    private static String decode(String s) {
        if (s == null)
            return s;
        int n = s.length();
        if (n == 0)
            return s;
        if (s.indexOf('%') < 0)
            return s;

        StringBuffer sb = new StringBuffer(n);
        ByteBuffer bb = ByteBuffer.allocate(n);
        CharBuffer cb = CharBuffer.allocate(n);
        CharsetDecoder dec = ThreadLocalCoders.decoderFor("UTF-8")
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);

        // This is not horribly efficient, but it will do for now
        char c = s.charAt(0);
        boolean betweenBrackets = false;

        for (int i = 0; i < n; ) {
            assert c == s.charAt(i);    // Loop invariant
            if (c == '[') {
                betweenBrackets = true;
            } else if (betweenBrackets && c == ']') {
                betweenBrackets = false;
            }
            if (c != '%' || betweenBrackets) {
                sb.append(c);
                if (++i >= n)
                    break;
                c = s.charAt(i);
                continue;
            }
            bb.clear();
            int ui = i;
            for (; ; ) {
                assert (n - i >= 2);
                bb.put(decode(s.charAt(++i), s.charAt(++i)));
                if (++i >= n)
                    break;
                c = s.charAt(i);
                if (c != '%')
                    break;
            }
            bb.flip();
            cb.clear();
            dec.reset();
            CoderResult cr = dec.decode(bb, cb, true);
            assert cr.isUnderflow();
            cr = dec.flush(cb);
            assert cr.isUnderflow();
            sb.append(cb.flip().toString());
        }

        return sb.toString();
    }

    /**
     * Attempts to parse this URI's authority component, if defined, into
     * user-information, host, and port components.
     *
     * <p> If this URI's authority component has already been recognized as
     * being server-based then it will already have been parsed into
     * user-information, host, and port components.  In this case, or if this
     * URI has no authority component, this method simply returns this URI.
     *
     * <p> Otherwise this method attempts once more to parse the authority
     * component into user-information, host, and port components, and throws
     * an exception describing why the authority component could not be parsed
     * in that way.
     *
     * <p> This method is provided because the generic URI syntax specified in
     * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
     * cannot always distinguish a malformed server-based authority from a
     * legitimate registry-based authority.  It must therefore treat some
     * instances of the former as instances of the latter.  The authority
     * component in the URI string {@code "//foo:bar"}, for example, is not a
     * legal server-based authority but it is legal as a registry-based
     * authority.
     *
     * <p> In many common situations, for example when working URIs that are
     * known to be either URNs or URLs, the hierarchical URIs being used will
     * always be server-based.  They therefore must either be parsed as such or
     * treated as an error.  In these cases a statement such as
     *
     * <blockquote>
     * {@code URI }<i>u</i>{@code  = new URI(str).parseServerAuthority();}
     * </blockquote>
     *
     * <p> can be used to ensure that <i>u</i> always refers to a URI that, if
     * it has an authority component, has a server-based authority with proper
     * user-information, host, and port components.  Invoking this method also
     * ensures that if the authority could not be parsed in that way then an
     * appropriate diagnostic message can be issued based upon the exception
     * that is thrown. </p>
     *
     * @return A URI whose authority field has been parsed
     * as a server-based authority
     * @throws URISyntaxException If the authority component of this URI is defined
     *                            but cannot be parsed as a server-based authority
     *                            according to RFC&nbsp;2396
     */
    public URI parseServerAuthority()
            throws URISyntaxException {
        // We could be clever and cache the error message and index from the
        // exception thrown during the original parse, but that would require
        // either more fields or a more-obscure representation.
        if ((host != null) || (authority == null))
            return this;
        defineString();
        new Parser(string).parse(true);
        return this;
    }

    /**
     * Normalizes this URI's path.
     *
     * <p> If this URI is opaque, or if its path is already in normal form,
     * then this URI is returned.  Otherwise a new URI is constructed that is
     * identical to this URI except that its path is computed by normalizing
     * this URI's path in a manner consistent with <a
     * href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>,
     * section&nbsp;5.2, step&nbsp;6, sub-steps&nbsp;c through&nbsp;f; that is:
     * </p>
     *
     * <ol>
     *
     * <li><p> All {@code "."} segments are removed. </p></li>
     *
     * <li><p> If a {@code ".."} segment is preceded by a non-{@code ".."}
     * segment then both of these segments are removed.  This step is
     * repeated until it is no longer applicable. </p></li>
     *
     * <li><p> If the path is relative, and if its first segment contains a
     * colon character ({@code ':'}), then a {@code "."} segment is
     * prepended.  This prevents a relative URI with a path such as
     * {@code "a:b/c/d"} from later being re-parsed as an opaque URI with a
     * scheme of {@code "a"} and a scheme-specific part of {@code "b/c/d"}.
     * <b><i>(Deviation from RFC&nbsp;2396)</i></b> </p></li>
     *
     * </ol>
     *
     * <p> A normalized path will begin with one or more {@code ".."} segments
     * if there were insufficient non-{@code ".."} segments preceding them to
     * allow their removal.  A normalized path will begin with a {@code "."}
     * segment if one was inserted by step 3 above.  Otherwise, a normalized
     * path will not contain any {@code "."} or {@code ".."} segments. </p>
     *
     * @return A URI equivalent to this URI,
     * but whose path is in normal form
     */
    public URI normalize() {
        return normalize(this);
    }

    /**
     * Resolves the given URI against this URI.
     *
     * <p> If the given URI is already absolute, or if this URI is opaque, then
     * the given URI is returned.
     *
     * <p><a name="resolve-frag"></a> If the given URI's fragment component is
     * defined, its path component is empty, and its scheme, authority, and
     * query components are undefined, then a URI with the given fragment but
     * with all other components equal to those of this URI is returned.  This
     * allows a URI representing a standalone fragment reference, such as
     * {@code "#foo"}, to be usefully resolved against a base URI.
     *
     * <p> Otherwise this method constructs a new hierarchical URI in a manner
     * consistent with <a
     * href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>,
     * section&nbsp;5.2; that is: </p>
     *
     * <ol>
     *
     * <li><p> A new URI is constructed with this URI's scheme and the given
     * URI's query and fragment components. </p></li>
     *
     * <li><p> If the given URI has an authority component then the new URI's
     * authority and path are taken from the given URI. </p></li>
     *
     * <li><p> Otherwise the new URI's authority component is copied from
     * this URI, and its path is computed as follows: </p>
     *
     * <ol>
     *
     * <li><p> If the given URI's path is absolute then the new URI's path
     * is taken from the given URI. </p></li>
     *
     * <li><p> Otherwise the given URI's path is relative, and so the new
     * URI's path is computed by resolving the path of the given URI
     * against the path of this URI.  This is done by concatenating all but
     * the last segment of this URI's path, if any, with the given URI's
     * path and then normalizing the result as if by invoking the {@link
     * #normalize() normalize} method. </p></li>
     *
     * </ol></li>
     *
     * </ol>
     *
     * <p> The result of this method is absolute if, and only if, either this
     * URI is absolute or the given URI is absolute.  </p>
     *
     * @param uri The URI to be resolved against this URI
     * @return The resulting URI
     * @throws NullPointerException If {@code uri} is {@code null}
     */
    public URI resolve(URI uri) {
        return resolve(this, uri);
    }

    /**
     * Constructs a new URI by parsing the given string and then resolving it
     * against this URI.
     *
     * <p> This convenience method works as if invoking it were equivalent to
     * evaluating the expression {@code (URI.}{@link #create(String) create}{@code (str))}. </p>
     *
     * @param str The string to be parsed into a URI
     * @return The resulting URI
     * @throws NullPointerException     If {@code str} is {@code null}
     * @throws IllegalArgumentException If the given string violates RFC&nbsp;2396
     */
    public URI resolve(String str) {
        return resolve(URI.create(str));
    }

    /**
     * Relativizes the given URI against this URI.
     *
     * <p> The relativization of the given URI against this URI is computed as
     * follows: </p>
     *
     * <ol>
     *
     * <li><p> If either this URI or the given URI are opaque, or if the
     * scheme and authority components of the two URIs are not identical, or
     * if the path of this URI is not a prefix of the path of the given URI,
     * then the given URI is returned. </p></li>
     *
     * <li><p> Otherwise a new relative hierarchical URI is constructed with
     * query and fragment components taken from the given URI and with a path
     * component computed by removing this URI's path from the beginning of
     * the given URI's path. </p></li>
     *
     * </ol>
     *
     * @param uri The URI to be relativized against this URI
     * @return The resulting URI
     * @throws NullPointerException If {@code uri} is {@code null}
     */
    public URI relativize(URI uri) {
        return relativize(this, uri);
    }

    /**
     * Constructs a URL from this URI.
     *
     * <p> This convenience method works as if invoking it were equivalent to
     * evaluating the expression {@code new URL(this.toString())} after
     * first checking that this URI is absolute. </p>
     *
     * @return A URL constructed from this URI
     * @throws IllegalArgumentException If this URL is not absolute
     * @throws MalformedURLException    If a protocol handler for the URL could not be found,
     *                                  or if some other error occurred while constructing the URL
     */
    public URL toURL()
            throws MalformedURLException {
        if (!isAbsolute())
            throw new IllegalArgumentException("URI is not absolute");
        return new URL(toString());
    }

    /**
     * Returns the scheme component of this URI.
     *
     * <p> The scheme component of a URI, if defined, only contains characters
     * in the <i>alphanum</i> category and in the string {@code "-.+"}.  A
     * scheme always starts with an <i>alpha</i> character. <p>
     * <p>
     * The scheme component of a URI cannot contain escaped octets, hence this
     * method does not perform any decoding.
     *
     * @return The scheme component of this URI,
     * or {@code null} if the scheme is undefined
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Tells whether or not this URI is absolute.
     *
     * <p> A URI is absolute if, and only if, it has a scheme component. </p>
     *
     * @return {@code true} if, and only if, this URI is absolute
     */
    public boolean isAbsolute() {
        return scheme != null;
    }

    /**
     * Tells whether or not this URI is opaque.
     *
     * <p> A URI is opaque if, and only if, it is absolute and its
     * scheme-specific part does not begin with a slash character ('/').
     * An opaque URI has a scheme, a scheme-specific part, and possibly
     * a fragment; all other components are undefined. </p>
     *
     * @return {@code true} if, and only if, this URI is opaque
     */
    public boolean isOpaque() {
        return path == null;
    }

    /**
     * Returns the raw scheme-specific part of this URI.  The scheme-specific
     * part is never undefined, though it may be empty.
     *
     * <p> The scheme-specific part of a URI only contains legal URI
     * characters. </p>
     *
     * @return The raw scheme-specific part of this URI
     * (never {@code null})
     */
    public String getRawSchemeSpecificPart() {
        defineSchemeSpecificPart();
        return schemeSpecificPart;
    }

    /**
     * Returns the decoded scheme-specific part of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawSchemeSpecificPart() getRawSchemeSpecificPart} method
     * except that all sequences of escaped octets are <a
     * href="#decode">decoded</a>.  </p>
     *
     * @return The decoded scheme-specific part of this URI
     * (never {@code null})
     */
    public String getSchemeSpecificPart() {
        if (decodedSchemeSpecificPart == null)
            decodedSchemeSpecificPart = decode(getRawSchemeSpecificPart());
        return decodedSchemeSpecificPart;
    }

    /**
     * Returns the raw authority component of this URI.
     *
     * <p> The authority component of a URI, if defined, only contains the
     * commercial-at character ({@code '@'}) and characters in the
     * <i>unreserved</i>, <i>punct</i>, <i>escaped</i>, and <i>other</i>
     * categories.  If the authority is server-based then it is further
     * constrained to have valid user-information, host, and port
     * components. </p>
     *
     * @return The raw authority component of this URI,
     * or {@code null} if the authority is undefined
     */
    public String getRawAuthority() {
        return authority;
    }

    /**
     * Returns the decoded authority component of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawAuthority() getRawAuthority} method except that all
     * sequences of escaped octets are <a href="#decode">decoded</a>.  </p>
     *
     * @return The decoded authority component of this URI,
     * or {@code null} if the authority is undefined
     */
    public String getAuthority() {
        if (decodedAuthority == null)
            decodedAuthority = decode(authority);
        return decodedAuthority;
    }

    /**
     * Returns the raw user-information component of this URI.
     *
     * <p> The user-information component of a URI, if defined, only contains
     * characters in the <i>unreserved</i>, <i>punct</i>, <i>escaped</i>, and
     * <i>other</i> categories. </p>
     *
     * @return The raw user-information component of this URI,
     * or {@code null} if the user information is undefined
     */
    public String getRawUserInfo() {
        return userInfo;
    }

    /**
     * Returns the decoded user-information component of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawUserInfo() getRawUserInfo} method except that all
     * sequences of escaped octets are <a href="#decode">decoded</a>.  </p>
     *
     * @return The decoded user-information component of this URI,
     * or {@code null} if the user information is undefined
     */
    public String getUserInfo() {
        if ((decodedUserInfo == null) && (userInfo != null))
            decodedUserInfo = decode(userInfo);
        return decodedUserInfo;
    }

    /**
     * Returns the host component of this URI.
     *
     * <p> The host component of a URI, if defined, will have one of the
     * following forms: </p>
     *
     * <ul>
     *
     * <li><p> A domain name consisting of one or more <i>labels</i>
     * separated by period characters ({@code '.'}), optionally followed by
     * a period character.  Each label consists of <i>alphanum</i> characters
     * as well as hyphen characters ({@code '-'}), though hyphens never
     * occur as the first or last characters in a label. The rightmost
     * label of a domain name consisting of two or more labels, begins
     * with an <i>alpha</i> character. </li>
     *
     * <li><p> A dotted-quad IPv4 address of the form
     * <i>digit</i>{@code +.}<i>digit</i>{@code +.}<i>digit</i>{@code +.}<i>digit</i>{@code +},
     * where no <i>digit</i> sequence is longer than three characters and no
     * sequence has a value larger than 255. </p></li>
     *
     * <li><p> An IPv6 address enclosed in square brackets ({@code '['} and
     * {@code ']'}) and consisting of hexadecimal digits, colon characters
     * ({@code ':'}), and possibly an embedded IPv4 address.  The full
     * syntax of IPv6 addresses is specified in <a
     * href="http://www.ietf.org/rfc/rfc2373.txt"><i>RFC&nbsp;2373: IPv6
     * Addressing Architecture</i></a>.  </p></li>
     *
     * </ul>
     * <p>
     * The host component of a URI cannot contain escaped octets, hence this
     * method does not perform any decoding.
     *
     * @return The host component of this URI,
     * or {@code null} if the host is undefined
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port number of this URI.
     *
     * <p> The port component of a URI, if defined, is a non-negative
     * integer. </p>
     *
     * @return The port component of this URI,
     * or {@code -1} if the port is undefined
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the raw path component of this URI.
     *
     * <p> The path component of a URI, if defined, only contains the slash
     * character ({@code '/'}), the commercial-at character ({@code '@'}),
     * and characters in the <i>unreserved</i>, <i>punct</i>, <i>escaped</i>,
     * and <i>other</i> categories. </p>
     *
     * @return The path component of this URI,
     * or {@code null} if the path is undefined
     */
    public String getRawPath() {
        return path;
    }

    /**
     * Returns the decoded path component of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawPath() getRawPath} method except that all sequences of
     * escaped octets are <a href="#decode">decoded</a>.  </p>
     *
     * @return The decoded path component of this URI,
     * or {@code null} if the path is undefined
     */
    public String getPath() {
        if ((decodedPath == null) && (path != null))
            decodedPath = decode(path);
        return decodedPath;
    }

    /**
     * Returns the raw query component of this URI.
     *
     * <p> The query component of a URI, if defined, only contains legal URI
     * characters. </p>
     *
     * @return The raw query component of this URI,
     * or {@code null} if the query is undefined
     */
    public String getRawQuery() {
        return query;
    }

    /**
     * Returns the decoded query component of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawQuery() getRawQuery} method except that all sequences of
     * escaped octets are <a href="#decode">decoded</a>.  </p>
     *
     * @return The decoded query component of this URI,
     * or {@code null} if the query is undefined
     */
    public String getQuery() {
        if ((decodedQuery == null) && (query != null))
            decodedQuery = decode(query);
        return decodedQuery;
    }

    /**
     * Returns the raw fragment component of this URI.
     *
     * <p> The fragment component of a URI, if defined, only contains legal URI
     * characters. </p>
     *
     * @return The raw fragment component of this URI,
     * or {@code null} if the fragment is undefined
     */
    public String getRawFragment() {
        return fragment;
    }

    /**
     * Returns the decoded fragment component of this URI.
     *
     * <p> The string returned by this method is equal to that returned by the
     * {@link #getRawFragment() getRawFragment} method except that all
     * sequences of escaped octets are <a href="#decode">decoded</a>.  </p>
     *
     * @return The decoded fragment component of this URI,
     * or {@code null} if the fragment is undefined
     */
    public String getFragment() {
        if ((decodedFragment == null) && (fragment != null))
            decodedFragment = decode(fragment);
        return decodedFragment;
    }

    /**
     * Tests this URI for equality with another object.
     *
     * <p> If the given object is not a URI then this method immediately
     * returns {@code false}.
     *
     * <p> For two URIs to be considered equal requires that either both are
     * opaque or both are hierarchical.  Their schemes must either both be
     * undefined or else be equal without regard to case. Their fragments
     * must either both be undefined or else be equal.
     *
     * <p> For two opaque URIs to be considered equal, their scheme-specific
     * parts must be equal.
     *
     * <p> For two hierarchical URIs to be considered equal, their paths must
     * be equal and their queries must either both be undefined or else be
     * equal.  Their authorities must either both be undefined, or both be
     * registry-based, or both be server-based.  If their authorities are
     * defined and are registry-based, then they must be equal.  If their
     * authorities are defined and are server-based, then their hosts must be
     * equal without regard to case, their port numbers must be equal, and
     * their user-information components must be equal.
     *
     * <p> When testing the user-information, path, query, fragment, authority,
     * or scheme-specific parts of two URIs for equality, the raw forms rather
     * than the encoded forms of these components are compared and the
     * hexadecimal digits of escaped octets are compared without regard to
     * case.
     *
     * <p> This method satisfies the general contract of the {@link
     * java.lang.Object#equals(Object) Object.equals} method. </p>
     *
     * @param ob The object to which this object is to be compared
     * @return {@code true} if, and only if, the given object is a URI that
     * is identical to this URI
     */
    public boolean equals(Object ob) {
        if (ob == this)
            return true;
        if (!(ob instanceof URI))
            return false;
        URI that = (URI) ob;
        if (this.isOpaque() != that.isOpaque()) return false;
        if (!equalIgnoringCase(this.scheme, that.scheme)) return false;
        if (!equal(this.fragment, that.fragment)) return false;

        // Opaque
        if (this.isOpaque())
            return equal(this.schemeSpecificPart, that.schemeSpecificPart);

        // Hierarchical
        if (!equal(this.path, that.path)) return false;
        if (!equal(this.query, that.query)) return false;

        // Authorities
        if (this.authority == that.authority) return true;
        if (this.host != null) {
            // Server-based
            if (!equal(this.userInfo, that.userInfo)) return false;
            if (!equalIgnoringCase(this.host, that.host)) return false;
            if (this.port != that.port) return false;
        } else if (this.authority != null) {
            // Registry-based
            if (!equal(this.authority, that.authority)) return false;
        } else if (this.authority != that.authority) {
            return false;
        }

        return true;
    }

    /**
     * Returns a hash-code value for this URI.  The hash code is based upon all
     * of the URI's components, and satisfies the general contract of the
     * {@link java.lang.Object#hashCode() Object.hashCode} method.
     *
     * @return A hash-code value for this URI
     */
    public int hashCode() {
        if (hash != 0)
            return hash;
        int h = hashIgnoringCase(0, scheme);
        h = hash(h, fragment);
        if (isOpaque()) {
            h = hash(h, schemeSpecificPart);
        } else {
            h = hash(h, path);
            h = hash(h, query);
            if (host != null) {
                h = hash(h, userInfo);
                h = hashIgnoringCase(h, host);
                h += 1949 * port;
            } else {
                h = hash(h, authority);
            }
        }
        hash = h;
        return h;
    }

    /**
     * Compares this URI to another object, which must be a URI.
     *
     * <p> When comparing corresponding components of two URIs, if one
     * component is undefined but the other is defined then the first is
     * considered to be less than the second.  Unless otherwise noted, string
     * components are ordered according to their natural, case-sensitive
     * ordering as defined by the {@link java.lang.String#compareTo(Object)
     * String.compareTo} method.  String components that are subject to
     * encoding are compared by comparing their raw forms rather than their
     * encoded forms.
     *
     * <p> The ordering of URIs is defined as follows: </p>
     *
     * <ul>
     *
     * <li><p> Two URIs with different schemes are ordered according the
     * ordering of their schemes, without regard to case. </p></li>
     *
     * <li><p> A hierarchical URI is considered to be less than an opaque URI
     * with an identical scheme. </p></li>
     *
     * <li><p> Two opaque URIs with identical schemes are ordered according
     * to the ordering of their scheme-specific parts. </p></li>
     *
     * <li><p> Two opaque URIs with identical schemes and scheme-specific
     * parts are ordered according to the ordering of their
     * fragments. </p></li>
     *
     * <li><p> Two hierarchical URIs with identical schemes are ordered
     * according to the ordering of their authority components: </p>
     *
     * <ul>
     *
     * <li><p> If both authority components are server-based then the URIs
     * are ordered according to their user-information components; if these
     * components are identical then the URIs are ordered according to the
     * ordering of their hosts, without regard to case; if the hosts are
     * identical then the URIs are ordered according to the ordering of
     * their ports. </p></li>
     *
     * <li><p> If one or both authority components are registry-based then
     * the URIs are ordered according to the ordering of their authority
     * components. </p></li>
     *
     * </ul></li>
     *
     * <li><p> Finally, two hierarchical URIs with identical schemes and
     * authority components are ordered according to the ordering of their
     * paths; if their paths are identical then they are ordered according to
     * the ordering of their queries; if the queries are identical then they
     * are ordered according to the order of their fragments. </p></li>
     *
     * </ul>
     *
     * <p> This method satisfies the general contract of the {@link
     * java.lang.Comparable#compareTo(Object) Comparable.compareTo}
     * method. </p>
     *
     * @param that The object to which this URI is to be compared
     * @return A negative integer, zero, or a positive integer as this URI is
     * less than, equal to, or greater than the given URI
     * @throws ClassCastException If the given object is not a URI
     */
    public int compareTo(URI that) {
        int c;

        if ((c = compareIgnoringCase(this.scheme, that.scheme)) != 0)
            return c;

        if (this.isOpaque()) {
            if (that.isOpaque()) {
                // Both opaque
                if ((c = compare(this.schemeSpecificPart,
                        that.schemeSpecificPart)) != 0)
                    return c;
                return compare(this.fragment, that.fragment);
            }
            return +1;                  // Opaque > hierarchical
        } else if (that.isOpaque()) {
            return -1;                  // Hierarchical < opaque
        }

        // Hierarchical
        if ((this.host != null) && (that.host != null)) {
            // Both server-based
            if ((c = compare(this.userInfo, that.userInfo)) != 0)
                return c;
            if ((c = compareIgnoringCase(this.host, that.host)) != 0)
                return c;
            if ((c = this.port - that.port) != 0)
                return c;
        } else {
            // If one or both authorities are registry-based then we simply
            // compare them in the usual, case-sensitive way.  If one is
            // registry-based and one is server-based then the strings are
            // guaranteed to be unequal, hence the comparison will never return
            // zero and the compareTo and equals methods will remain
            // consistent.
            if ((c = compare(this.authority, that.authority)) != 0) return c;
        }

        if ((c = compare(this.path, that.path)) != 0) return c;
        if ((c = compare(this.query, that.query)) != 0) return c;
        return compare(this.fragment, that.fragment);
    }

    /**
     * Returns the content of this URI as a string.
     *
     * <p> If this URI was created by invoking one of the constructors in this
     * class then a string equivalent to the original input string, or to the
     * string computed from the originally-given components, as appropriate, is
     * returned.  Otherwise this URI was created by normalization, resolution,
     * or relativization, and so a string is constructed from this URI's
     * components according to the rules specified in <a
     * href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>,
     * section&nbsp;5.2, step&nbsp;7. </p>
     *
     * @return The string form of this URI
     */
    public String toString() {
        defineString();
        return string;
    }

    /**
     * Returns the content of this URI as a US-ASCII string.
     *
     * <p> If this URI does not contain any characters in the <i>other</i>
     * category then an invocation of this method will return the same value as
     * an invocation of the {@link #toString() toString} method.  Otherwise
     * this method works as if by invoking that method and then <a
     * href="#encode">encoding</a> the result.  </p>
     *
     * @return The string form of this URI, encoded as needed
     * so that it only contains characters in the US-ASCII
     * charset
     */
    public String toASCIIString() {
        defineString();
        return encode(string);
    }


    // -- Escaping and encoding --

    /**
     * Saves the content of this URI to the given serial stream.
     *
     * <p> The only serializable field of a URI instance is its {@code string}
     * field.  That field is given a value, if it does not have one already,
     * and then the {@link java.io.ObjectOutputStream#defaultWriteObject()}
     * method of the given object-output stream is invoked. </p>
     *
     * @param os The object-output stream to which this object
     *           is to be written
     */
    private void writeObject(ObjectOutputStream os)
            throws IOException {
        defineString();
        os.defaultWriteObject();        // Writes the string field only
    }

    /**
     * Reconstitutes a URI from the given serial stream.
     *
     * <p> The {@link java.io.ObjectInputStream#defaultReadObject()} method is
     * invoked to read the value of the {@code string} field.  The result is
     * then parsed in the usual way.
     *
     * @param is The object-input stream from which this object
     *           is being read
     */
    private void readObject(ObjectInputStream is)
            throws ClassNotFoundException, IOException {
        port = -1;                      // Argh
        is.defaultReadObject();
        try {
            new Parser(string).parse(false);
        } catch (URISyntaxException x) {
            IOException y = new InvalidObjectException("Invalid URI");
            y.initCause(x);
            throw y;
        }
    }

    private void appendAuthority(StringBuffer sb,
                                 String authority,
                                 String userInfo,
                                 String host,
                                 int port) {
        if (host != null) {
            sb.append("//");
            if (userInfo != null) {
                sb.append(quote(userInfo, L_USERINFO, H_USERINFO));
                sb.append('@');
            }
            boolean needBrackets = ((host.indexOf(':') >= 0)
                    && !host.startsWith("[")
                    && !host.endsWith("]"));
            if (needBrackets) sb.append('[');
            sb.append(host);
            if (needBrackets) sb.append(']');
            if (port != -1) {
                sb.append(':');
                sb.append(port);
            }
        } else if (authority != null) {
            sb.append("//");
            if (authority.startsWith("[")) {
                // authority should (but may not) contain an embedded IPv6 address
                int end = authority.indexOf("]");
                String doquote = authority, dontquote = "";
                if (end != -1 && authority.indexOf(":") != -1) {
                    // the authority contains an IPv6 address
                    if (end == authority.length()) {
                        dontquote = authority;
                        doquote = "";
                    } else {
                        dontquote = authority.substring(0, end + 1);
                        doquote = authority.substring(end + 1);
                    }
                }
                sb.append(dontquote);
                sb.append(quote(doquote,
                        L_REG_NAME | L_SERVER,
                        H_REG_NAME | H_SERVER));
            } else {
                sb.append(quote(authority,
                        L_REG_NAME | L_SERVER,
                        H_REG_NAME | H_SERVER));
            }
        }
    }

    private void appendSchemeSpecificPart(StringBuffer sb,
                                          String opaquePart,
                                          String authority,
                                          String userInfo,
                                          String host,
                                          int port,
                                          String path,
                                          String query) {
        if (opaquePart != null) {
            /* check if SSP begins with an IPv6 address
             * because we must not quote a literal IPv6 address
             */
            if (opaquePart.startsWith("//[")) {
                int end = opaquePart.indexOf("]");
                if (end != -1 && opaquePart.indexOf(":") != -1) {
                    String doquote, dontquote;
                    if (end == opaquePart.length()) {
                        dontquote = opaquePart;
                        doquote = "";
                    } else {
                        dontquote = opaquePart.substring(0, end + 1);
                        doquote = opaquePart.substring(end + 1);
                    }
                    sb.append(dontquote);
                    sb.append(quote(doquote, L_URIC, H_URIC));
                }
            } else {
                sb.append(quote(opaquePart, L_URIC, H_URIC));
            }
        } else {
            appendAuthority(sb, authority, userInfo, host, port);
            if (path != null)
                sb.append(quote(path, L_PATH, H_PATH));
            if (query != null) {
                sb.append('?');
                sb.append(quote(query, L_URIC, H_URIC));
            }
        }
    }

    private void appendFragment(StringBuffer sb, String fragment) {
        if (fragment != null) {
            sb.append('#');
            sb.append(quote(fragment, L_URIC, H_URIC));
        }
    }

    private String toString(String scheme,
                            String opaquePart,
                            String authority,
                            String userInfo,
                            String host,
                            int port,
                            String path,
                            String query,
                            String fragment) {
        StringBuffer sb = new StringBuffer();
        if (scheme != null) {
            sb.append(scheme);
            sb.append(':');
        }
        appendSchemeSpecificPart(sb, opaquePart,
                authority, userInfo, host, port,
                path, query);
        appendFragment(sb, fragment);
        return sb.toString();
    }

    private void defineSchemeSpecificPart() {
        if (schemeSpecificPart != null) return;
        StringBuffer sb = new StringBuffer();
        appendSchemeSpecificPart(sb, null, getAuthority(), getUserInfo(),
                host, port, getPath(), getQuery());
        if (sb.length() == 0) return;
        schemeSpecificPart = sb.toString();
    }

    private void defineString() {
        if (string != null) return;

        StringBuffer sb = new StringBuffer();
        if (scheme != null) {
            sb.append(scheme);
            sb.append(':');
        }
        if (isOpaque()) {
            sb.append(schemeSpecificPart);
        } else {
            if (host != null) {
                sb.append("//");
                if (userInfo != null) {
                    sb.append(userInfo);
                    sb.append('@');
                }
                boolean needBrackets = ((host.indexOf(':') >= 0)
                        && !host.startsWith("[")
                        && !host.endsWith("]"));
                if (needBrackets) sb.append('[');
                sb.append(host);
                if (needBrackets) sb.append(']');
                if (port != -1) {
                    sb.append(':');
                    sb.append(port);
                }
            } else if (authority != null) {
                sb.append("//");
                sb.append(authority);
            }
            if (path != null)
                sb.append(path);
            if (query != null) {
                sb.append('?');
                sb.append(query);
            }
        }
        if (fragment != null) {
            sb.append('#');
            sb.append(fragment);
        }
        string = sb.toString();
    }


    // -- Parsing --

    // For convenience we wrap the input URI string in a new instance of the
    // following internal class.  This saves always having to pass the input
    // string as an argument to each internal scan/parse method.

    private class Parser {

        private String input;           // URI input string
        private boolean requireServerAuthority = false;
        private int ipv6byteCount = 0;

        // -- Methods for throwing URISyntaxException in various ways --

        Parser(String s) {
            input = s;
            string = s;
        }

        private void fail(String reason) throws URISyntaxException {
            throw new URISyntaxException(input, reason);
        }

        private void fail(String reason, int p) throws URISyntaxException {
            throw new URISyntaxException(input, reason, p);
        }

        private void failExpecting(String expected, int p)
                throws URISyntaxException {
            fail("Expected " + expected, p);
        }


        // -- Simple access to the input string --

        private void failExpecting(String expected, String prior, int p)
                throws URISyntaxException {
            fail("Expected " + expected + " following " + prior, p);
        }

        // Return a substring of the input string
        //
        private String substring(int start, int end) {
            return input.substring(start, end);
        }

        // Return the char at position p,
        // assuming that p < input.length()
        //
        private char charAt(int p) {
            return input.charAt(p);
        }

        // Tells whether start < end and, if so, whether charAt(start) == c
        //
        private boolean at(int start, int end, char c) {
            return (start < end) && (charAt(start) == c);
        }


        // -- Scanning --

        // The various scan and parse methods that follow use a uniform
        // convention of taking the current start position and end index as
        // their first two arguments.  The start is inclusive while the end is
        // exclusive, just as in the String class, i.e., a start/end pair
        // denotes the left-open interval [start, end) of the input string.
        //
        // These methods never proceed past the end position.  They may return
        // -1 to indicate outright failure, but more often they simply return
        // the position of the first char after the last char scanned.  Thus
        // a typical idiom is
        //
        //     int p = start;
        //     int q = scan(p, end, ...);
        //     if (q > p)
        //         // We scanned something
        //         ...;
        //     else if (q == p)
        //         // We scanned nothing
        //         ...;
        //     else if (q == -1)
        //         // Something went wrong
        //         ...;

        // Tells whether start + s.length() < end and, if so,
        // whether the chars at the start position match s exactly
        //
        private boolean at(int start, int end, String s) {
            int p = start;
            int sn = s.length();
            if (sn > end - p)
                return false;
            int i = 0;
            while (i < sn) {
                if (charAt(p++) != s.charAt(i)) {
                    break;
                }
                i++;
            }
            return (i == sn);
        }

        // Scan a specific char: If the char at the given start position is
        // equal to c, return the index of the next char; otherwise, return the
        // start position.
        //
        private int scan(int start, int end, char c) {
            if ((start < end) && (charAt(start) == c))
                return start + 1;
            return start;
        }

        // Scan forward from the given start position.  Stop at the first char
        // in the err string (in which case -1 is returned), or the first char
        // in the stop string (in which case the index of the preceding char is
        // returned), or the end of the input string (in which case the length
        // of the input string is returned).  May return the start position if
        // nothing matches.
        //
        private int scan(int start, int end, String err, String stop) {
            int p = start;
            while (p < end) {
                char c = charAt(p);
                if (err.indexOf(c) >= 0)
                    return -1;
                if (stop.indexOf(c) >= 0)
                    break;
                p++;
            }
            return p;
        }

        // Scan a potential escape sequence, starting at the given position,
        // with the given first char (i.e., charAt(start) == c).
        //
        // This method assumes that if escapes are allowed then visible
        // non-US-ASCII chars are also allowed.
        //
        private int scanEscape(int start, int n, char first)
                throws URISyntaxException {
            int p = start;
            char c = first;
            if (c == '%') {
                // Process escape pair
                if ((p + 3 <= n)
                        && match(charAt(p + 1), L_HEX, H_HEX)
                        && match(charAt(p + 2), L_HEX, H_HEX)) {
                    return p + 3;
                }
                fail("Malformed escape pair", p);
            } else if ((c > 128)
                    && !Character.isSpaceChar(c)
                    && !Character.isISOControl(c)) {
                // Allow unescaped but visible non-US-ASCII chars
                return p + 1;
            }
            return p;
        }

        // Scan chars that match the given mask pair
        //
        private int scan(int start, int n, long lowMask, long highMask)
                throws URISyntaxException {
            int p = start;
            while (p < n) {
                char c = charAt(p);
                if (match(c, lowMask, highMask)) {
                    p++;
                    continue;
                }
                if ((lowMask & L_ESCAPED) != 0) {
                    int q = scanEscape(p, n, c);
                    if (q > p) {
                        p = q;
                        continue;
                    }
                }
                break;
            }
            return p;
        }

        // Check that each of the chars in [start, end) matches the given mask
        //
        private void checkChars(int start, int end,
                                long lowMask, long highMask,
                                String what)
                throws URISyntaxException {
            int p = scan(start, end, lowMask, highMask);
            if (p < end)
                fail("Illegal character in " + what, p);
        }


        // -- Parsing --

        // Check that the char at position p matches the given mask
        //
        private void checkChar(int p,
                               long lowMask, long highMask,
                               String what)
                throws URISyntaxException {
            checkChars(p, p + 1, lowMask, highMask, what);
        }

        // [<scheme>:]<scheme-specific-part>[#<fragment>]
        //
        void parse(boolean rsa) throws URISyntaxException {
            requireServerAuthority = rsa;
            int ssp;                    // Start of scheme-specific part
            int n = input.length();
            int p = scan(0, n, "/?#", ":");
            if ((p >= 0) && at(p, n, ':')) {
                if (p == 0)
                    failExpecting("scheme name", 0);
                checkChar(0, L_ALPHA, H_ALPHA, "scheme name");
                checkChars(1, p, L_SCHEME, H_SCHEME, "scheme name");
                scheme = substring(0, p);
                p++;                    // Skip ':'
                ssp = p;
                if (at(p, n, '/')) {
                    p = parseHierarchical(p, n);
                } else {
                    int q = scan(p, n, "", "#");
                    if (q <= p)
                        failExpecting("scheme-specific part", p);
                    checkChars(p, q, L_URIC, H_URIC, "opaque part");
                    p = q;
                }
            } else {
                ssp = 0;
                p = parseHierarchical(0, n);
            }
            schemeSpecificPart = substring(ssp, p);
            if (at(p, n, '#')) {
                checkChars(p + 1, n, L_URIC, H_URIC, "fragment");
                fragment = substring(p + 1, n);
                p = n;
            }
            if (p < n)
                fail("end of URI", p);
        }

        // [//authority]<path>[?<query>]
        //
        // DEVIATION from RFC2396: We allow an empty authority component as
        // long as it's followed by a non-empty path, query component, or
        // fragment component.  This is so that URIs such as "file:///foo/bar"
        // will parse.  This seems to be the intent of RFC2396, though the
        // grammar does not permit it.  If the authority is empty then the
        // userInfo, host, and port components are undefined.
        //
        // DEVIATION from RFC2396: We allow empty relative paths.  This seems
        // to be the intent of RFC2396, but the grammar does not permit it.
        // The primary consequence of this deviation is that "#f" parses as a
        // relative URI with an empty path.
        //
        private int parseHierarchical(int start, int n)
                throws URISyntaxException {
            int p = start;
            if (at(p, n, '/') && at(p + 1, n, '/')) {
                p += 2;
                int q = scan(p, n, "", "/?#");
                if (q > p) {
                    p = parseAuthority(p, q);
                } else if (q < n) {
                    // DEVIATION: Allow empty authority prior to non-empty
                    // path, query component or fragment identifier
                } else
                    failExpecting("authority", p);
            }
            int q = scan(p, n, "", "?#"); // DEVIATION: May be empty
            checkChars(p, q, L_PATH, H_PATH, "path");
            path = substring(p, q);
            p = q;
            if (at(p, n, '?')) {
                p++;
                q = scan(p, n, "", "#");
                checkChars(p, q, L_URIC, H_URIC, "query");
                query = substring(p, q);
                p = q;
            }
            return p;
        }

        // authority     = server | reg_name
        //
        // Ambiguity: An authority that is a registry name rather than a server
        // might have a prefix that parses as a server.  We use the fact that
        // the authority component is always followed by '/' or the end of the
        // input string to resolve this: If the complete authority did not
        // parse as a server then we try to parse it as a registry name.
        //
        private int parseAuthority(int start, int n)
                throws URISyntaxException {
            int p = start;
            int q = p;
            URISyntaxException ex = null;

            boolean serverChars;
            boolean regChars;

            if (scan(p, n, "", "]") > p) {
                // contains a literal IPv6 address, therefore % is allowed
                serverChars = (scan(p, n, L_SERVER_PERCENT, H_SERVER_PERCENT) == n);
            } else {
                serverChars = (scan(p, n, L_SERVER, H_SERVER) == n);
            }
            regChars = (scan(p, n, L_REG_NAME, H_REG_NAME) == n);

            if (regChars && !serverChars) {
                // Must be a registry-based authority
                authority = substring(p, n);
                return n;
            }

            if (serverChars) {
                // Might be (probably is) a server-based authority, so attempt
                // to parse it as such.  If the attempt fails, try to treat it
                // as a registry-based authority.
                try {
                    q = parseServer(p, n);
                    if (q < n)
                        failExpecting("end of authority", q);
                    authority = substring(p, n);
                } catch (URISyntaxException x) {
                    // Undo results of failed parse
                    userInfo = null;
                    host = null;
                    port = -1;
                    if (requireServerAuthority) {
                        // If we're insisting upon a server-based authority,
                        // then just re-throw the exception
                        throw x;
                    } else {
                        // Save the exception in case it doesn't parse as a
                        // registry either
                        ex = x;
                        q = p;
                    }
                }
            }

            if (q < n) {
                if (regChars) {
                    // Registry-based authority
                    authority = substring(p, n);
                } else if (ex != null) {
                    // Re-throw exception; it was probably due to
                    // a malformed IPv6 address
                    throw ex;
                } else {
                    fail("Illegal character in authority", q);
                }
            }

            return n;
        }

        // [<userinfo>@]<host>[:<port>]
        //
        private int parseServer(int start, int n)
                throws URISyntaxException {
            int p = start;
            int q;

            // userinfo
            q = scan(p, n, "/?#", "@");
            if ((q >= p) && at(q, n, '@')) {
                checkChars(p, q, L_USERINFO, H_USERINFO, "user info");
                userInfo = substring(p, q);
                p = q + 1;              // Skip '@'
            }

            // hostname, IPv4 address, or IPv6 address
            if (at(p, n, '[')) {
                // DEVIATION from RFC2396: Support IPv6 addresses, per RFC2732
                p++;
                q = scan(p, n, "/?#", "]");
                if ((q > p) && at(q, n, ']')) {
                    // look for a "%" scope id
                    int r = scan(p, q, "", "%");
                    if (r > p) {
                        parseIPv6Reference(p, r);
                        if (r + 1 == q) {
                            fail("scope id expected");
                        }
                        checkChars(r + 1, q, L_ALPHANUM, H_ALPHANUM,
                                "scope id");
                    } else {
                        parseIPv6Reference(p, q);
                    }
                    host = substring(p - 1, q + 1);
                    p = q + 1;
                } else {
                    failExpecting("closing bracket for IPv6 address", q);
                }
            } else {
                q = parseIPv4Address(p, n);
                if (q <= p)
                    q = parseHostname(p, n);
                p = q;
            }

            // port
            if (at(p, n, ':')) {
                p++;
                q = scan(p, n, "", "/");
                if (q > p) {
                    checkChars(p, q, L_DIGIT, H_DIGIT, "port number");
                    try {
                        port = Integer.parseInt(substring(p, q));
                    } catch (NumberFormatException x) {
                        fail("Malformed port number", p);
                    }
                    p = q;
                }
            }
            if (p < n)
                failExpecting("port number", p);

            return p;
        }

        // Scan a string of decimal digits whose value fits in a byte
        //
        private int scanByte(int start, int n)
                throws URISyntaxException {
            int p = start;
            int q = scan(p, n, L_DIGIT, H_DIGIT);
            if (q <= p) return q;
            if (Integer.parseInt(substring(p, q)) > 255) return p;
            return q;
        }

        // Scan an IPv4 address.
        //
        // If the strict argument is true then we require that the given
        // interval contain nothing besides an IPv4 address; if it is false
        // then we only require that it start with an IPv4 address.
        //
        // If the interval does not contain or start with (depending upon the
        // strict argument) a legal IPv4 address characters then we return -1
        // immediately; otherwise we insist that these characters parse as a
        // legal IPv4 address and throw an exception on failure.
        //
        // We assume that any string of decimal digits and dots must be an IPv4
        // address.  It won't parse as a hostname anyway, so making that
        // assumption here allows more meaningful exceptions to be thrown.
        //
        private int scanIPv4Address(int start, int n, boolean strict)
                throws URISyntaxException {
            int p = start;
            int q;
            int m = scan(p, n, L_DIGIT | L_DOT, H_DIGIT | H_DOT);
            if ((m <= p) || (strict && (m != n)))
                return -1;
            for (; ; ) {
                // Per RFC2732: At most three digits per byte
                // Further constraint: Each element fits in a byte
                if ((q = scanByte(p, m)) <= p) break;
                p = q;
                if ((q = scan(p, m, '.')) <= p) break;
                p = q;
                if ((q = scanByte(p, m)) <= p) break;
                p = q;
                if ((q = scan(p, m, '.')) <= p) break;
                p = q;
                if ((q = scanByte(p, m)) <= p) break;
                p = q;
                if ((q = scan(p, m, '.')) <= p) break;
                p = q;
                if ((q = scanByte(p, m)) <= p) break;
                p = q;
                if (q < m) break;
                return q;
            }
            fail("Malformed IPv4 address", q);
            return -1;
        }

        // Take an IPv4 address: Throw an exception if the given interval
        // contains anything except an IPv4 address
        //
        private int takeIPv4Address(int start, int n, String expected)
                throws URISyntaxException {
            int p = scanIPv4Address(start, n, true);
            if (p <= start)
                failExpecting(expected, start);
            return p;
        }

        // Attempt to parse an IPv4 address, returning -1 on failure but
        // allowing the given interval to contain [:<characters>] after
        // the IPv4 address.
        //
        private int parseIPv4Address(int start, int n) {
            int p;

            try {
                p = scanIPv4Address(start, n, false);
            } catch (URISyntaxException x) {
                return -1;
            } catch (NumberFormatException nfe) {
                return -1;
            }

            if (p > start && p < n) {
                // IPv4 address is followed by something - check that
                // it's a ":" as this is the only valid character to
                // follow an address.
                if (charAt(p) != ':') {
                    p = -1;
                }
            }

            if (p > start)
                host = substring(start, p);

            return p;
        }


        // IPv6 address parsing, from RFC2373: IPv6 Addressing Architecture
        //
        // Bug: The grammar in RFC2373 Appendix B does not allow addresses of
        // the form ::12.34.56.78, which are clearly shown in the examples
        // earlier in the document.  Here is the original grammar:
        //
        //   IPv6address = hexpart [ ":" IPv4address ]
        //   hexpart     = hexseq | hexseq "::" [ hexseq ] | "::" [ hexseq ]
        //   hexseq      = hex4 *( ":" hex4)
        //   hex4        = 1*4HEXDIG
        //
        // We therefore use the following revised grammar:
        //
        //   IPv6address = hexseq [ ":" IPv4address ]
        //                 | hexseq [ "::" [ hexpost ] ]
        //                 | "::" [ hexpost ]
        //   hexpost     = hexseq | hexseq ":" IPv4address | IPv4address
        //   hexseq      = hex4 *( ":" hex4)
        //   hex4        = 1*4HEXDIG
        //
        // This covers all and only the following cases:
        //
        //   hexseq
        //   hexseq : IPv4address
        //   hexseq ::
        //   hexseq :: hexseq
        //   hexseq :: hexseq : IPv4address
        //   hexseq :: IPv4address
        //   :: hexseq
        //   :: hexseq : IPv4address
        //   :: IPv4address
        //   ::
        //
        // Additionally we constrain the IPv6 address as follows :-
        //
        //  i.  IPv6 addresses without compressed zeros should contain
        //      exactly 16 bytes.
        //
        //  ii. IPv6 addresses with compressed zeros should contain
        //      less than 16 bytes.

        // hostname      = domainlabel [ "." ] | 1*( domainlabel "." ) toplabel [ "." ]
        // domainlabel   = alphanum | alphanum *( alphanum | "-" ) alphanum
        // toplabel      = alpha | alpha *( alphanum | "-" ) alphanum
        //
        private int parseHostname(int start, int n)
                throws URISyntaxException {
            int p = start;
            int q;
            int l = -1;                 // Start of last parsed label

            do {
                // domainlabel = alphanum [ *( alphanum | "-" ) alphanum ]
                q = scan(p, n, L_ALPHANUM, H_ALPHANUM);
                if (q <= p)
                    break;
                l = p;
                if (q > p) {
                    p = q;
                    q = scan(p, n, L_ALPHANUM | L_DASH, H_ALPHANUM | H_DASH);
                    if (q > p) {
                        if (charAt(q - 1) == '-')
                            fail("Illegal character in hostname", q - 1);
                        p = q;
                    }
                }
                q = scan(p, n, '.');
                if (q <= p)
                    break;
                p = q;
            } while (p < n);

            if ((p < n) && !at(p, n, ':'))
                fail("Illegal character in hostname", p);

            if (l < 0)
                failExpecting("hostname", start);

            // for a fully qualified hostname check that the rightmost
            // label starts with an alpha character.
            if (l > start && !match(charAt(l), L_ALPHA, H_ALPHA)) {
                fail("Illegal character in hostname", l);
            }

            host = substring(start, p);
            return p;
        }

        private int parseIPv6Reference(int start, int n)
                throws URISyntaxException {
            int p = start;
            int q;
            boolean compressedZeros = false;

            q = scanHexSeq(p, n);

            if (q > p) {
                p = q;
                if (at(p, n, "::")) {
                    compressedZeros = true;
                    p = scanHexPost(p + 2, n);
                } else if (at(p, n, ':')) {
                    p = takeIPv4Address(p + 1, n, "IPv4 address");
                    ipv6byteCount += 4;
                }
            } else if (at(p, n, "::")) {
                compressedZeros = true;
                p = scanHexPost(p + 2, n);
            }
            if (p < n)
                fail("Malformed IPv6 address", start);
            if (ipv6byteCount > 16)
                fail("IPv6 address too long", start);
            if (!compressedZeros && ipv6byteCount < 16)
                fail("IPv6 address too short", start);
            if (compressedZeros && ipv6byteCount == 16)
                fail("Malformed IPv6 address", start);

            return p;
        }

        private int scanHexPost(int start, int n)
                throws URISyntaxException {
            int p = start;
            int q;

            if (p == n)
                return p;

            q = scanHexSeq(p, n);
            if (q > p) {
                p = q;
                if (at(p, n, ':')) {
                    p++;
                    p = takeIPv4Address(p, n, "hex digits or IPv4 address");
                    ipv6byteCount += 4;
                }
            } else {
                p = takeIPv4Address(p, n, "hex digits or IPv4 address");
                ipv6byteCount += 4;
            }
            return p;
        }

        // Scan a hex sequence; return -1 if one could not be scanned
        //
        private int scanHexSeq(int start, int n)
                throws URISyntaxException {
            int p = start;
            int q;

            q = scan(p, n, L_HEX, H_HEX);
            if (q <= p)
                return -1;
            if (at(q, n, '.'))          // Beginning of IPv4 address
                return -1;
            if (q > p + 4)
                fail("IPv6 hexadecimal digit sequence too long", p);
            ipv6byteCount += 2;
            p = q;
            while (p < n) {
                if (!at(p, n, ':'))
                    break;
                if (at(p + 1, n, ':'))
                    break;              // "::"
                p++;
                q = scan(p, n, L_HEX, H_HEX);
                if (q <= p)
                    failExpecting("digits for an IPv6 address", p);
                if (at(q, n, '.')) {    // Beginning of IPv4 address
                    p--;
                    break;
                }
                if (q > p + 4)
                    fail("IPv6 hexadecimal digit sequence too long", p);
                ipv6byteCount += 2;
                p = q;
            }

            return p;
        }

    }

}

