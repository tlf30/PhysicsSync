package io.tlf.jme.physics.msg;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

@Serializable
public class PhysicsSyncObjMessage  extends AbstractMessage {

    /**
     * Maximum number of object that can be placed in a UDP packet
     */
    public static final int MAX_PACK = 100;

    private String[] name;
    private long[] id;
    private boolean[] remove;

    public PhysicsSyncObjMessage() {
        this.setReliable(true);
    }

    public String[] getName() {
        return name;
    }

    public void setName(String[] name) {
        this.name = name;
    }

    public long[] getId() {
        return id;
    }

    public void setId(long[] id) {
        this.id = id;
    }

    public boolean[] getRemove() {
        return remove;
    }

    public void setRemove(boolean[] remove) {
        this.remove = remove;
    }
}
