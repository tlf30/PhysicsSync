package io.tlf.jme.physics.msg;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.scene.Spatial;
import io.tlf.jme.physics.PhysicsStateData;

@Serializable
public class PhysicsSyncMessage extends AbstractMessage {

    /**
     * Maximum number of object that can be placed in a UDP packet
     */
    public static final int MAX_PACK = 1000;

    private PhysicsStateData[] data;
    private long timestamp;

    public PhysicsSyncMessage() {
        this.setReliable(false);
    }

    public void setPhysicsData(PhysicsStateData[] data) {
        this.data = data;
    }

    public PhysicsStateData[] getPhysicsData() {
        return data;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
