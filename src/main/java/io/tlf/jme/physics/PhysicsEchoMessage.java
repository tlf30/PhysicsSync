package io.tlf.jme.physics;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

@Serializable
public class PhysicsEchoMessage extends AbstractMessage {
    private long serverTime;
    private long clientTime;

    public PhysicsEchoMessage() {
        this.setReliable(false);
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    public long getClientTime() {
        return clientTime;
    }

    public void setClientTime(long clientTime) {
        this.clientTime = clientTime;
    }
}
