package io.tlf.jme.physics.msg;

import com.jme3.export.JmeExporter;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.scene.Mesh;
import io.tlf.jme.physics.DebugData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Serializable
public class PhysicsDebugMessage extends AbstractMessage {

    private byte[] meshData;
    private long id;

    public PhysicsDebugMessage() {
        this.setReliable(true);
    }

    public PhysicsDebugMessage(DebugData data) {
        this.setReliable(true);
        setData(data);
    }

    public void setData(DebugData data) {
        BinaryExporter binWriter = BinaryExporter.getInstance();
        ByteArrayOutputStream meshOut = new ByteArrayOutputStream();
        ByteArrayOutputStream matOut = new ByteArrayOutputStream();
        try {
            //binWriter.save(data.mat, matOut);
            binWriter.save(data.mesh, meshOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
        meshData = meshOut.toByteArray();
        id = data.id;
    }

    public DebugData getData() {
        BinaryImporter binReader = BinaryImporter.getInstance();
        DebugData dd = new DebugData();
        try {
            dd.mesh = (Mesh) binReader.load(meshData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        dd.id = id;
        return dd;
    }
}
