package io.tlf.jme.physics;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.scene.Spatial;

@Serializable
public class PhysicsSyncMessage extends AbstractMessage {

    private PhysicsStateData[] data;

    public PhysicsSyncMessage() {
        this.setReliable(false);
    }

    public void setPhysicsData(PhysicsStateData[] data) {
        this.data = data;
    }

    public PhysicsStateData[] getPhysicsData() {
        return data;
    }
}
