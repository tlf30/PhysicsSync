package io.tlf.jme.physics;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.scene.Spatial;

import java.util.Collections;
import java.util.HashMap;

public class PhysicsSyncClient extends BaseAppState implements MessageListener<Client> {

    private SimpleApplication app;
    private HashMap<String, Long> timestamps = new HashMap<>();
    private long counter = 0;
    private Object lock = new Object();
    private boolean loaded = false;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    public void update(float tpf) {
        counter += tpf;

        if (counter > 60000) { //We do not need to check this very often, mostly just for cleanup so our hashmap does not get too large.
            counter = 0;
            long current = System.currentTimeMillis();
            synchronized (lock) {
                for (String key : Collections.unmodifiableCollection(timestamps.keySet())) {
                    if (timestamps.get(key) + 60000 < current) { //We have not received updates for over a minute.
                        timestamps.remove(key); //Remove the timestamp entry.
                    }
                }
            }
        }
    }

    @Override
    protected void onEnable() {
        loaded = true;
    }

    @Override
    protected void onDisable() {
        loaded = false;
    }

    @Override
    public void messageReceived(Client source, Message m) {
        if (!loaded) {
            return; //We do not sync if we are not loaded.
        }

        //Check Message Type
        if (m instanceof PhysicsSyncMessage) { //Sync Physics
            synchronized (lock) {
                app.enqueue(() -> {
                    for (PhysicsStateData state : ((PhysicsSyncMessage) m).getPhysicsData()) {
                        if (timestamps.containsKey(state.getSpatial()) && timestamps.get(state.getSpatial()) > ((PhysicsSyncMessage) m).getTimestamp()) {
                            continue; //This is an old message
                        } else {
                            timestamps.put(state.getSpatial(), ((PhysicsSyncMessage) m).getTimestamp());
                        }
                        Spatial obj = app.getRootNode().getChild(state.getSpatial());
                        PhysicsControl control = obj.getControl(PhysicsControl.class);
                        if (control == null) { //Client is not performing physics on the object
                            obj.setLocalTranslation(state.getLocation());
                            obj.setLocalRotation(state.getRotation());
                        } else {
                            //The client is performing physics on the object. We will ignore it.
                        }
                    }
                });
            }
        } else if (m instanceof PhysicsEchoMessage) { //Echo
            source.send(m); //Echo message back
        }
    }
}
