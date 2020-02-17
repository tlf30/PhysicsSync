package io.tlf.jme.physics;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
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
    //Interpolation Values
    private float interpMaxDistance = 1f;
    private float interpMaxRot = 90f; //Degrees
    private long interpMaxDelay = 100; //milliseconds
    private boolean interp = true;
    private HashMap<String, InterpData> interpData = new HashMap<>();

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

        synchronized (lock) { //Prevent network message from modifying update state info during processing the info
            if (counter > 60000) { //We do not need to check this very often, mostly just for cleanup so our hashmap does not get too large.
                counter = 0;
                long current = System.currentTimeMillis();
                for (String key : Collections.unmodifiableCollection(timestamps.keySet())) {
                    if (timestamps.get(key) + 60000 < current) { //We have not received updates for over a minute.
                        timestamps.remove(key); //Remove the timestamp entry.
                    }
                }
            }

            if (interp) {
                for (InterpData data : Collections.unmodifiableCollection(interpData.values())) {
                    if (data.update(tpf)) {
                        interpData.remove(data);
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
                        long lastUpdate = timestamps.containsKey(state.getSpatial()) ? timestamps.get(state.getSpatial()) : -1;
                        if (lastUpdate > ((PhysicsSyncMessage) m).getTimestamp()) {
                            continue; //This is an old message
                        } else {
                            timestamps.put(state.getSpatial(), ((PhysicsSyncMessage) m).getTimestamp());
                        }
                        Spatial obj = app.getRootNode().getChild(state.getSpatial());
                        PhysicsControl control = obj.getControl(PhysicsControl.class);
                        long updateDelay = ((PhysicsSyncMessage) m).getTimestamp() - lastUpdate;
                        float updateDistance = obj.getLocalTranslation().distance(state.getLocation());
                        float updateAngle = (float) (Math.acos(obj.getLocalRotation().dot(state.getRotation())) * 2.0);

                        if (control == null) { //Client is not performing physics on the object
                            //If interpolation is enabled, and the last update we got was recent enough
                            //Also checking distanced moved and amount rotated
                            if (interp && updateDelay < interpMaxDelay && updateDistance < interpMaxDistance && updateAngle < interpMaxRot) {
                                InterpData interpObj = new InterpData();
                                interpObj.delta = (float) updateDelay;
                                interpObj.current = 0f;
                                interpObj.obj = obj;
                                interpObj.targetPos = state.getLocation();
                                interpObj.targetRot = state.getRotation();
                                interpObj.startPos = obj.getLocalTranslation();
                                interpObj.startRot = obj.getLocalRotation();

                                //Check if we are still interpolating, if so move the object to where it should be by now
                                /*if (interpData.containsKey(state.getSpatial())) {
                                    InterpData d = interpData.get(state.getSpatial());
                                    obj.setLocalTranslation(d.targetPos);
                                    obj.setLocalRotation(d.targetRot);
                                    System.out.println("Rapid update, forced sync");
                                }*/
                                interpData.put(state.getSpatial(), interpObj);
                            } else {
                                obj.setLocalTranslation(state.getLocation());
                                obj.setLocalRotation(state.getRotation());
                            }
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

    private class InterpData {
        public float delta;
        public float current;
        public Vector3f targetPos;
        public Vector3f startPos;
        public Quaternion targetRot;
        public Quaternion startRot;
        public Spatial obj;

        public boolean update(float tpf) {
            current += tpf * 1000;
            float percentInterp = current / delta;
            if (percentInterp >= 1f) {
                obj.setLocalTranslation(targetPos);
                obj.setLocalRotation(targetRot);
                return true;
            } else {
                obj.setLocalTranslation(startPos.interpolateLocal(targetPos, percentInterp));
                obj.setLocalRotation(new Quaternion().slerp(startRot, targetRot, percentInterp));
                return false;
            }

        }
    }
}
