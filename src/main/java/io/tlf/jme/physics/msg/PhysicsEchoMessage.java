package io.tlf.jme.physics.msg;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

@Serializable
public class PhysicsEchoMessage extends AbstractMessage {
    private long serverTime;

    public PhysicsEchoMessage() {
        this.setReliable(false);
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }
}
