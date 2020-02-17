package io.tlf.jme.physics.test;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import io.tlf.jme.physics.PhysicsSyncServer;

import java.io.IOException;

/**
 * Test Physics Server
 * @author Trevor Flynn <trevorflynn@liquidcrystalstudios.com>
 */
public class TestServer extends SimpleApplication implements ConnectionListener {

    public static void main(String[] args) {

        TestServer app = new TestServer();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Network Physics Sync Server");
        settings.setSamples(16);
        settings.setGammaCorrection(true);
        app.setSettings(settings);
        app.start(JmeContext.Type.Headless);

    }

    private Geometry boxGeometry;
    private BulletAppState bulletAppState;
    private PhysicsSyncServer physicsSync;

    @Override
    public void simpleInitApp() {
        System.out.println("Server Loading");
        //Configure Physics
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL); //<-- Causes NPE
        stateManager.attach(bulletAppState);
        bulletAppState.setDebugEnabled(true);

        physicsSync = new PhysicsSyncServer(bulletAppState);
        stateManager.attach(physicsSync);

        //Configure Network
        try {
            Server myServer = Network.createServer(6143);
            physicsSync.registerMessages();
            myServer.addConnectionListener(this);
            myServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Add a simple box.
        Box b = new Box(1, 1, 1);
        boxGeometry = new Geometry("Box", b);
        RigidBodyControl control = new RigidBodyControl( 1f );
        boxGeometry.addControl(control);
        control.setKinematic(true);

        //Add to scene
        rootNode.attachChild(boxGeometry);

        // Add to physics sync
        physicsSync.add(boxGeometry);
        //
        System.out.println("Server Running");
    }

    private boolean direction = true;

    @Override
    public void simpleUpdate(float tpf) {
        boxGeometry.rotate(0, tpf * 2f, 0);
        float y = boxGeometry.getLocalTranslation().y + tpf * 5;
        if (boxGeometry.getLocalTranslation().y > 5) {
            direction = false;
        } else if (boxGeometry.getLocalTranslation().y < 1f) {
            direction = true;
        }
        y = boxGeometry.getLocalTranslation().y + tpf * (direction ? 1f : -1f);
        boxGeometry.setLocalTranslation(0,  y, 0);
    }

    @Override
    public void connectionAdded(Server server, HostedConnection conn) {
        System.out.println("Got connection: " + conn.getAddress());
        //Add some delay to let the client load. Ideally you would not start syncing until after some form of a hand shake
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        physicsSync.add(conn);
    }

    @Override
    public void connectionRemoved(Server server, HostedConnection conn) {
        System.out.println("Lost connection: " + conn.getAddress());
        physicsSync.remove(conn);
    }
}