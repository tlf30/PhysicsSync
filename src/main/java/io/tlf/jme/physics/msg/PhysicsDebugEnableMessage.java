package io.tlf.jme.physics.msg;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

@Serializable
public class PhysicsDebugEnableMessage extends AbstractMessage {
    private boolean enabled;

    public PhysicsDebugEnableMessage() {
        this(true);
    }

    public PhysicsDebugEnableMessage(boolean enabled) {
        this.setEnabled(enabled);
        this.setReliable(true);
    }


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
