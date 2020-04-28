package io.tlf.jme.physics.test;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.network.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.system.AppSettings;
import io.tlf.jme.physics.PhysicsSyncClient;

import java.awt.*;
import java.io.IOException;

/**
 * Test Physics Client
 * @author Trevor Flynn <trevorflynn@liquidcrystalstudios.com>
 */
public class TestClient extends SimpleApplication {

    public static void main(String[] args) {

        TestClient app = new TestClient();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Network Physics Sync Client");
        settings.setSamples(16);
        settings.setGammaCorrection(true);
        app.setSettings(settings);

        app.start();

    }

    private Geometry boxGeometry;
    private PhysicsSyncClient physicsSync;

    @Override
    public void simpleInitApp() {

        // Configure Physics
        physicsSync = new PhysicsSyncClient();
        stateManager.attach(physicsSync);

        // Configure the scene for PBR
        getRenderManager().setPreferredLightMode(TechniqueDef.LightMode.SinglePassAndImageBased);
        getRenderManager().setSinglePassLightBatchSize(10);

        // change the viewport background color.
        viewPort.setBackgroundColor(new ColorRGBA(0.03f, 0.03f, 0.03f, 1.0f));

        // Add a simple box.
        Box b = new Box(1, 1, 1);
        boxGeometry = new Geometry("Box", b);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat.setColor("BaseColor", new ColorRGBA(0.2f, 0.2f, 0.2f, 0.5f));
        mat.setFloat("Roughness", 0.2f);
        mat.setFloat("Metallic", 0.0001f);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        boxGeometry.setMaterial(mat);
        boxGeometry.setQueueBucket(RenderQueue.Bucket.Transparent);
        //Add to scene
        rootNode.attachChild(boxGeometry);

        // Add some lights
        DirectionalLight directionalLight = new DirectionalLight(
                new Vector3f(-1, -1, -1).normalizeLocal(),
                new ColorRGBA(1,1,1,1)
        );

        rootNode.addLight(directionalLight);

        // load a lightprobe from test-data
        Node probeNode = (Node) assetManager.loadModel("Scenes/defaultProbe.j3o");
        LightProbe lightProbe = (LightProbe) probeNode.getLocalLightList().get(0);
        rootNode.addLight(lightProbe);

        // Add some post-processor effects.
        initPostFx(directionalLight);

        //Make connection to server
        try {
            Client myClient = Network.connectToServer("localhost", 6143);
            myClient.addMessageListener(physicsSync);
            myClient.start();

            System.out.println("Connected: " + myClient.isConnected());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Enable physics debugging
        physicsSync.setPhysicsDebugging(true);
    }

    @Override
    public void simpleUpdate(float tpf) {
    }

    private void initPostFx(DirectionalLight directionalLight) {

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, 4096, 3);
        dlsf.setLight(directionalLight);
        fpp.addFilter(dlsf);

        SSAOFilter ssaoFilter = new SSAOFilter();
        fpp.addFilter(ssaoFilter);

        FXAAFilter fxaaFilter = new FXAAFilter();
        fpp.addFilter(fxaaFilter);

        ToneMapFilter toneMapFilter = new ToneMapFilter();
        fpp.addFilter(toneMapFilter);

        viewPort.addProcessor(fpp);

    }
}