package net.nothingtv.ruva.client.tools;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;

public class PlayerObject extends SceneObject {

    public PlayerObject(String name, ModelInstance modelInstance) {
        super(name, modelInstance);
    }

    public btCollisionObject getCollisionObject() {
        return rigidBody;
    }
}
