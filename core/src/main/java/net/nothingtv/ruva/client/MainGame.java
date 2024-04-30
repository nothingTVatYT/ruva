package net.nothingtv.ruva.client;

import com.badlogic.gdx.Game;
import net.nothingtv.ruva.client.screens.SelectScreen;

public class MainGame extends Game {

    @Override
    public void create() {
        setScreen(new SelectScreen(this));
    }
}
