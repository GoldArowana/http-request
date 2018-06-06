package com.king.jdk.net;

import java.io.*;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.*;

public final class SocketPermissionCollection extends PermissionCollection
        implements Serializable {
    private static final long serialVersionUID = 2787186408602843674L;
    /**
     * @serialField permissions java.util.Vector
     * A list of the SocketPermissions for this set.
     */
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("permissions", Vector.class),
    };
    // Not serialized; see serialization section at end of class
    private transient List<SocketPermission> perms;

    /**
     * Create an empty SocketPermissions object.
     */

    public SocketPermissionCollection() {
        perms = new ArrayList<SocketPermission>();
    }

    /**
     * Adds a permission to the SocketPermissions. The key for the hash is
     * the name in the case of wildcards, or all the IP addresses.
     *
     * @param permission the Permission object to add.
     * @throws IllegalArgumentException - if the permission is not a
     *                                  SocketPermission
     * @throws SecurityException        - if this SocketPermissionCollection object
     *                                  has been marked readonly
     */
    public void add(Permission permission) {
        if (!(permission instanceof SocketPermission))
            throw new IllegalArgumentException("invalid permission: " +
                    permission);
        if (isReadOnly())
            throw new SecurityException(
                    "attempt to add a Permission to a readonly PermissionCollection");

        // optimization to ensure perms most likely to be tested
        // show up early (4301064)
        synchronized (this) {
            perms.add(0, (SocketPermission) permission);
        }
    }

    /**
     * Check and see if this collection of permissions implies the permissions
     * expressed in "permission".
     *
     * @param permission the Permission object to compare
     * @return true if "permission" is a proper subset of a permission in
     * the collection, false if not.
     */

    public boolean implies(Permission permission) {
        if (!(permission instanceof SocketPermission))
            return false;

        SocketPermission np = (SocketPermission) permission;

        int desired = np.getMask();
        int effective = 0;
        int needed = desired;

        synchronized (this) {
            int len = perms.size();
            //System.out.println("implies "+np);
            for (int i = 0; i < len; i++) {
                SocketPermission x = perms.get(i);
                //System.out.println("  trying "+x);
                if (((needed & x.getMask()) != 0) && x.impliesIgnoreMask(np)) {
                    effective |= x.getMask();
                    if ((effective & desired) == desired)
                        return true;
                    needed = (desired ^ effective);
                }
            }
        }
        return false;
    }

    // Need to maintain serialization interoperability with earlier releases,
    // which had the serializable field:

    //
    // The SocketPermissions for this set.
    // @serial
    //
    // private Vector permissions;

    /**
     * Returns an enumeration of all the SocketPermission objects in the
     * container.
     *
     * @return an enumeration of all the SocketPermission objects.
     */

    @SuppressWarnings("unchecked")
    public Enumeration<Permission> elements() {
        // Convert Iterator into Enumeration
        synchronized (this) {
            return Collections.enumeration((List<Permission>) (List) perms);
        }
    }

    /**
     * @serialData "permissions" field (a Vector containing the SocketPermissions).
     */
    /*
     * Writes the contents of the perms field out as a Vector for
     * serialization compatibility with earlier releases.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Don't call out.defaultWriteObject()

        // Write out Vector
        Vector<SocketPermission> permissions = new Vector<>(perms.size());

        synchronized (this) {
            permissions.addAll(perms);
        }

        ObjectOutputStream.PutField pfields = out.putFields();
        pfields.put("permissions", permissions);
        out.writeFields();
    }

    /*
     * Reads in a Vector of SocketPermissions and saves them in the perms field.
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        // Don't call in.defaultReadObject()

        // Read in serialized fields
        ObjectInputStream.GetField gfields = in.readFields();

        // Get the one we want
        @SuppressWarnings("unchecked")
        Vector<SocketPermission> permissions = (Vector<SocketPermission>) gfields.get("permissions", null);
        perms = new ArrayList<SocketPermission>(permissions.size());
        perms.addAll(permissions);
    }
}


