package net.nothingtv.ruva.client.tools;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import net.mgsx.gltf.scene3d.scene.Updatable;

public class NpcObject extends SceneObject implements Updatable {

    public boolean walking = false;
    private final Vector3 velocityXZ = new Vector3();
    private final Vector3 pos = new Vector3();
    private final Vector3 physicsPos = new Vector3();
    protected AnimatedModelInstance animatedModelInstance;
    private final Matrix4 rootTargetTransform = new Matrix4();

    public NpcObject(String name, AnimatedModelInstance modelInstance) {
        super(name, modelInstance);
        this.animatedModelInstance = modelInstance;
        rootTargetTransform.set(modelInstance.transform);
        this.animatedModelInstance.setRootTransform(rootTargetTransform);
    }

    public NpcObject(String name, ModelInstance modelInstance, btRigidBody rigidBody, BoundingBox boundingBox) {
        super(name, modelInstance, rigidBody, boundingBox);
    }

    @Override
    public void moveTo(Vector3 position) {
        super.moveTo(position);
        rootTargetTransform.setTranslation(position);
    }

    @Override
    public void update(Camera camera, float v) {
        rigidBody.getWorldTransform().getTranslation(physicsPos);
        rootTargetTransform.getTranslation(pos);
        pos.y = physicsPos.y;
        rootTargetTransform.setTranslation(pos);
        animatedModelInstance.update(v);
        rootTargetTransform.getTranslation(pos);
        pos.sub(rigidBody.getWorldTransform().getTranslation(physicsPos)).add(((DefaultMotionState)motionState).rigidBodyOffset).scl(10 * mass);
        pos.y = 0;
        addForce(pos);
        if (!walking) {
            // counteract horizontal sliding
            velocityXZ.set(rigidBody.getLinearVelocity());
            velocityXZ.y = 0;
            velocityXZ.scl(-10 * mass);
            addForce(velocityXZ);
        }
    }
}