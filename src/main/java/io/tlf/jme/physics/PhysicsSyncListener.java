package io.tlf.jme.physics;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.scene.Spatial;

import java.util.HashMap;

public class PhysicsSyncListener implements PhysicsTickListener {

    private boolean loaded = false;
    private long lastUpdate = 0;
    private long updateInterval = 50; //in milliseconds
    private HashMap<Spatial, Boolean> updateStates = new HashMap<>();
    private PhysicsSyncServer engine;

    public PhysicsSyncListener(PhysicsSyncServer engine) {
        this.engine = engine;
    }

    @Override
    public void prePhysicsTick(PhysicsSpace space, float timeStep) {
        /*
         * We can check if a control is moving by the isActive() function.
         * In order to do this, check over the Physics Objects in the world
         * We will send one final update after the object has stopped moving
         */
        for (Spatial obj : engine.objects()) {
            if (obj.getControl(PhysicsControl.class) != null) {
                PhysicsControl control = obj.getControl(PhysicsControl.class);
                if (control instanceof PhysicsRigidBody) {
                    if (((PhysicsRigidBody) control).isActive()) {
                        updateStates.put(obj, true);
                    } else {
                        if (updateStates.containsKey(obj)) {
                            Boolean lastState = updateStates.get(obj);
                            if (!lastState) {
                                updateStates.remove(obj); //Remove the object because it has been stale for an update already.
                            } else {
                                updateStates.put(obj, false);
                            }
                        }
                    }

                }
            }
        }
    }

    @Override
    public void physicsTick(PhysicsSpace space, float timeStep) {
        long currentTime = System.currentTimeMillis();
        if (currentTime < lastUpdate + updateInterval) {
            return;
        }
        //Perform sync
        lastUpdate = currentTime;
        //TODO: Send packets out to client
        //We need to only update objects that are near the client player.
    }
}
