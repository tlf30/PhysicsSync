package io.tlf.jme.physics;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.scene.Spatial;

public class PhysicsSyncClient extends BaseAppState implements MessageListener<Client> {

    private SimpleApplication app;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }

    @Override
    public void messageReceived(Client source, Message m) {
        if (m instanceof PhysicsSyncMessage) {
            app.enqueue(() -> {
                for (PhysicsStateData state : ((PhysicsSyncMessage) m).getPhysicsData()) {
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
    }
}
