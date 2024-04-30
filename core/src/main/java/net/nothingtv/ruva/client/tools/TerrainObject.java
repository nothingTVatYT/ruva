package net.nothingtv.ruva.client.tools;

import net.nothingtv.ruva.client.terrain.Terrain;
import net.nothingtv.ruva.client.terrain.TerrainInstance;

public class TerrainObject extends SceneObject {

    public Terrain terrain;

    public TerrainObject(String name, TerrainInstance modelInstance, Terrain terrain) {
        super(name, modelInstance);
        this.terrain = terrain;
    }

    public TerrainInstance getTerrainInstance() {
        return (TerrainInstance) modelInstance;
    }
}
