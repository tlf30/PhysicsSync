package io.tlf.jme.physics;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.serializing.Serializable;
import com.jme3.network.serializing.Serializer;
import com.jme3.scene.Spatial;

import java.util.*;

public class PhysicsSyncServer extends BaseAppState implements PhysicsTickListener, MessageListener<HostedConnection> {

    private Application app;
    private volatile boolean loaded = false;
    private volatile long lastUpdate = 0;
    private volatile long updateInterval = 25; //in milliseconds
    private volatile float syncDistance = 100f;
    private HashMap<String, Boolean> updateStates = new HashMap<>();
    private HashMap<String, Spatial> objects = new HashMap<>();
    private HashMap<HostedConnection, Vector3f> clients = new HashMap<>();
    private HashMap<HostedConnection, Spatial> clientRelations = new HashMap<>();
    private final Object lock = new Object();
    private BulletAppState physics;

    public PhysicsSyncServer(BulletAppState physics) {
        this.physics = physics;
    }

    /**
     * Add Spatial to the physics space and sync engine.
     * Note: This will attach the Spatial's PhysicsControl to the BulletPhysics' Space
     * @param s The Spatial to attach
     */
    public void add(Spatial s) {
        synchronized (lock) {
            if (s.getControl(PhysicsControl.class) != null) {
                PhysicsControl control = s.getControl(PhysicsControl.class);
                physics.getPhysicsSpace().add(control);
                updateStates.put(s.getName(), true);
                objects.put(s.getName(), s);
            }
        }
    }

    /**
     * Remove the Spatial from the physics sync engine, and the physics space.
     * @param s The Spatial to remove.
     */
    public void remove(Spatial s) {
        synchronized (lock) {
            if (s.getControl(PhysicsControl.class) != null) {
                PhysicsControl control = s.getControl(PhysicsControl.class);
                physics.getPhysicsSpace().remove(control);
            }
            updateStates.remove(s.getName());
            objects.remove(s.getName());
        }
    }

    /**
     * Remove all Spatials from the physics sync engine and physics space.
     */
    public void clearObjects() {
        for (Spatial obj : objects()) {
            remove(obj);
        }
    }

    /**
     * Add Network Client to Physics Sync Engine.
     * Once a client has been added, the physics sync engine will begin syncing physics states.
     * @param c The Client to add.
     */
    public void add(HostedConnection c) {
        synchronized (lock) {
            clients.put(c, Vector3f.NAN.clone());
        }
    }

    /**
     * Will set the location of the client.
     * If the client is not registered, it will be registered with the given position.
     * The Vector3f is cloned and the original is not directly referenced.
     * Distance of Vector3f.NAN will cause client to receive all sync messages.
     * @param c The Network Client
     * @param pos The Position of the Client
     */
    public void add(HostedConnection c, Vector3f pos) {
        setLocation(c, pos);
    }

    /**
     * Add Network Client to Physics Sync Engine.
     * Once a client has been added, the physics sync engine will begin syncing physics states.
     * The physics sync engine will get client position based on the position of the spatial.
     * The spatial does not need to be registered with the engine.
     * A client can only have a single relation, adding another will remove the prior relation.
     * @param c The Network Client
     * @param relation The Spatial to base client position from.
     */
    public void add(HostedConnection c, Spatial relation) {
        setRelation(c, relation);
    }

    /**
     * Remove the Client to Spatial position relation from the engine.
     * This does not remove the client from the engine.
     * @param c The Network Client
     */
    public void removeRelation(HostedConnection c) {
        synchronized (lock) {
            clientRelations.remove(c);
        }
    }

    /**
     * Remove a Network Client from Physics Sync.
     * The client will stop receiving sync messages.
     * @param c The Client to remove.
     */
    public void remove(HostedConnection c) {
        synchronized (lock) {
            clients.remove(c);
            clientRelations.remove(c);
        }
    }

    /**
     *
     * @return A Collection containing all clients currently in the physics sync engine.
     */
    public Collection<HostedConnection> clients() {
        synchronized (lock) {
            return Collections.unmodifiableCollection(clients.keySet());
        }
    }

    /**
     * Will set the location of the client.
     * If the client is not registered, it will be registered with the given position.
     * The Vector3f is cloned and the original is not directly referenced.
     * Distance of Vector3f.NAN will cause client to receive all sync messages.
     * @param c The Network Client
     * @param pos The Position of the Client
     */
    public void setLocation(HostedConnection c, Vector3f pos) {
        synchronized (lock) {
            if (clients.containsKey(c)) {
                clients.get(c).set(pos);
            } else {
                clients.put(c, pos.clone());
            }
        }
    }

    /**
     * Add Network Client to Physics Sync Engine.
     * Once a client has been added, the physics sync engine will begin syncing physics states.
     * The physics sync engine will get client position based on the position of the spatial.
     * The spatial does not need to be registered with the engine.
     * A client can only have a single relation, adding another will remove the prior relation.
     * @param c The Network Client
     * @param relation The Spatial to base client position from.
     */
    public void setRelation(HostedConnection c, Spatial relation) {
        synchronized (lock) {
            setLocation(c, relation.getWorldTranslation());
            clientRelations.put(c, relation);
        }
    }

    /**
     * Get the Spatial that has a relation with the client.
     * @param c The client to which the spatial has a relation to.
     * @return The Spatial that has a positional relation to the client.
     */
    public Spatial getRelation(HostedConnection c) {
        synchronized (lock) {
            return clientRelations.get(c);
        }
    }

    /**
     * Get the location of the Client as seen by the sync engine.
     * @param c The Client to get the location of
     * @return The position of the client as seen by the sync engine.
     */
    public Vector3f getLocation(HostedConnection c) {
        synchronized (lock) {
            return clients.get(c);
        }
    }

    /**
     * Remove all Network Clients from Physics Sync Engine.
     */
    public void clearClients() {
        synchronized (lock) {
            for (HostedConnection c : clients()) {
                remove(c);
            }
        }
    }

    /**
     * Set the target update interval for syncing physics objects with clients.
     * @param milliseconds
     */
    public void setUpdateInterval(long milliseconds) {
        updateInterval = milliseconds;
    }

    /**
     *
     * @return The target update interval for syncing physics objects with clients.
     */
    public long getUpdateInterval() {
        return updateInterval;
    }

    /**
     * Set the distance from the object that a client will receive a sync messages.
     * A distance less than 0 will cause all objects to by synced.
     * @param distance The distance from the object to the client to get sync messages.
     */
    public void setSyncDistance(float distance) {
        this.syncDistance = distance;
    }

    /**
     * Get the distance from the object that a client will receive a sync messages.
     * @return The distance from the object that a client will receive a sync messages.
     */
    public float getSyncDistance() {
        return syncDistance;
    }

    /**
     *
     * @return A Collection containing all Spatials currently in the physics sync engine.
     */
    public Collection<Spatial> objects() {
        synchronized (lock) {
            return Collections.unmodifiableCollection(objects.values());
        }
    }

    /**
     * Initialize the Physics Sync Engine.
     * This must be performed after the NetworkServer has been created but before it is started,
     * and after the BulletAppState has been attached to the world.
     * @param app The Application for the PhysicsSyncEngine
     */
    @Override
    protected void initialize(Application app) {
        this.app = app;
        //Register physics
        physics.getPhysicsSpace().addTickListener(this);
    }

    @Override
    protected void cleanup(Application app) {
        clearClients();
        clearObjects();
    }

    /**
     * Will enable syncing with clients.
     */
    @Override
    protected void onEnable() {
        loaded = physics != null;
    }

    /**
     * Will disable syncing with clients, but preserve state information.
     * To enable syncing again, call <code>onEnable()<code/>.
     */
    @Override
    protected void onDisable() {
        loaded = false;
    }

    /**
     * Check which Spatials are currently active and need synced.
     * @param space
     * @param timeStep
     */
    @Override
    public void prePhysicsTick(PhysicsSpace space, float timeStep) {
        //Currently don't have anything to do before physics tick
    }

    /**
     * Send updates out to all clients needing physics info synced.
     * @param space The current physics space
     * @param timeStep Delta from last tick
     */
    @Override
    public void physicsTick(PhysicsSpace space, float timeStep) {
        if (!loaded) { //Don't sync if not enabled.
            return;
        }
        synchronized (lock) {
            System.out.println("Updating physics");
            /*
             * We can check if a control is moving by the isActive() function.
             * In order to do this, check over the Physics Objects in the world
             * We will send one final update after the object has stopped moving.
             *
             * Store object that need updated on clients in <code>updateStates</code>
             */
            for (Spatial obj : objects()) {
                if (obj.getControl(PhysicsControl.class) != null) {
                    PhysicsControl control = obj.getControl(PhysicsControl.class);
                    if (control instanceof PhysicsRigidBody) {
                        if (((PhysicsRigidBody) control).isActive()) {
                            updateStates.put(obj.getName(), true); //Object state is active, and needs to be updated.
                        } else {
                            if (updateStates.containsKey(obj)) {
                                Boolean lastState = updateStates.get(obj);
                                if (!lastState) {
                                    //The object has been stale for an update already.
                                    //Will get removed on next sync
                                } else {
                                    updateStates.put(obj.getName(), false); //Object is stale, but state will still get synced.
                                }
                            }
                        }

                    }
                }
            }

            //Check for the last update time.
            long currentTime = System.currentTimeMillis();
            if (currentTime < lastUpdate + updateInterval) {
                return;
            }

            //Perform sync
            lastUpdate = currentTime;
            //We need to only update objects that are near the client player.
            for (HostedConnection c : clients()) {
                //Update client relation if one exists
                if (clientRelations.containsKey(c)) {
                    setLocation(c, clientRelations.get(c).getWorldTranslation());
                }
                //Find objects to sync
                Stack<PhysicsStateData> data = new Stack<>();
                for (String objName : updateStates.keySet()) {
                    Spatial obj = objects.get(objName);
                    //Check distance
                    Vector3f clientPos = clients.get(c);
                    if (clientPos.equals(Vector3f.NAN) || syncDistance < 0 || clientPos.distance(obj.getWorldTranslation()) < syncDistance) {
                        PhysicsStateData stateData = new PhysicsStateData(obj);
                        data.push(stateData);
                    }
                }
                while (data.size() > 0) {
                    int len = data.size() > PhysicsStateData.MAX_PACK ? PhysicsStateData.MAX_PACK : data.size();
                    PhysicsStateData[] dataArray = new PhysicsStateData[len];
                    for (int i = 0; i < len; i++) {
                        dataArray[i] = data.pop();
                    }
                    PhysicsSyncMessage msg = new PhysicsSyncMessage();
                    msg.setPhysicsData(dataArray);
                    try {
                        c.send(msg);
                    } catch (Exception ex) {
                        //This occurs when the client is no longer connected to the server.
                        ex.printStackTrace();
                    }
                }
            }
            //Cleanup stale objects from update states
            for (String objName : updateStates.keySet()) {
                //Check if the state is stale
                if (!updateStates.get(objName)) {
                    //Remove stale state
                    updateStates.remove(objName);
                }
            }
        }
    }

    /**
     * Receive message from server
     * @param source Client who sent message
     * @param m Message that was sent
     */
    @Override
    public void messageReceived(HostedConnection source, Message m) {
        //TODO: Add echo message to get client network lag.
    }

    /**
     * Register network messages with Serialzer.
     * Must be call appropriately when network message registration needs to occur for your setup.
     */
    public void registerMessages() {
        Serializer.registerClass(PhysicsStateData.class);
        Serializer.registerClass(PhysicsSyncMessage.class);
        Serializer.registerClass(PhysicsEchoMessage.class);
    }
}
