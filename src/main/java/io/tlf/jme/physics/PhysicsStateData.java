package io.tlf.jme.physics;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.serializing.Serializable;

@Serializable
public class PhysicsStateData {
    private Vector3f location;
    private Quaternion rotation;
    private long id;

    public PhysicsStateData() {

    }

    public PhysicsStateData(Long id, Vector3f pos, Quaternion rot) {
        location = pos;
        rotation = rot;
        this.id = id;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
