package net.nothingtv.ruva.client.objects;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import net.nothingtv.ruva.client.tools.PlayerObject;

public class PlayerInfo {
    public String displayName;
    public TextureRegion icon;
    public PlayerObject playerObject;
    private float currentHealth = 95;
    public float getMaxHealth() {
        return 100;
    }
    public float getCurrentHealth() {
        return currentHealth;
    }
}
