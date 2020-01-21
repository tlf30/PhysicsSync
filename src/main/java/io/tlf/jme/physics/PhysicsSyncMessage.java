package io.tlf.jme.physics;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.scene.Spatial;

@Serializable
public class PhysicsSyncMessage extends AbstractMessage {
    private Vector3f location;
    private Quaternion rotation;
    private String spatial;

    public PhysicsSyncMessage() {

    }

    public PhysicsSyncMessage(Spatial s) {
        location = s.getLocalTranslation();
        rotation = s.getLocalRotation();
        spatial = s.getName();
        this.setReliable(false);
    }

    public Vector3f getLocation() {
        return location;
    }

    public void setLocation(Vector3f location) {
        this.location = location;
    }

    public Quaternion getRotation() {
        return rotation;
    }

    public void setRotation(Quaternion rotation) {
        this.rotation = rotation;
    }

    public String getSpatial() {
        return spatial;
    }

    public void setSpatial(String spatial) {
        this.spatial = spatial;
    }
}
