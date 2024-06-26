package net.nothingtv.ruva.client.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.nothingtv.ruva.client.GeneralSettings;
import net.nothingtv.ruva.client.inventory.GameItem;
import net.nothingtv.ruva.client.inventory.Inventory;
import net.nothingtv.ruva.client.objects.PlayerInfo;
import net.nothingtv.ruva.client.terrain.*;
import net.nothingtv.ruva.client.tools.*;
import net.nothingtv.ruva.client.ui.InventoryView;
import net.nothingtv.ruva.client.ui.PlayerConsole;
import net.nothingtv.ruva.client.ui.PlayerInfoView;
import net.nothingtv.ruva.client.ui.TestConsoleListener;

public class MainGameScreen extends BasicSceneManagerScreen {
    private SceneObject ball;
    private TerrainObject terrainObject;
    private Foliage foliage;
    private boolean lightControlsOn = false;
    private boolean useSplatGenerator = false;
    private final Vector3 initialPos = new Vector3(200, 0, 200);
    protected BasePlayerController playerController;
    private FootStepsSFX footSteps;
    private Label speedLabel;
    private Decal playerDecal;
    private InventoryView inventoryView;
    private PlayerInfo playerInfo;
    private PlayerInfoView playerInfoView;
    private PlayerConsole console;
    private final Schedule schedule = new Schedule();

    public MainGameScreen(Game game) {
        super(game);
    }

    @Override
    protected void init() {
        screenConfig.useSkybox = false;
        screenConfig.useShadows = GeneralSettings.isAtLeast(GeneralSettings.Setting.High);
        screenConfig.usePlayerController = true;
        screenConfig.ambientLightBrightness = 0.3f;
        screenConfig.showStats = false;
        screenConfig.showFPS = false;
        screenConfig.useIBL = true;
        //Gdx.app.setLogLevel(Application.LOG_DEBUG);
        super.init();
    }

    @Override
    public void initScene() {
        super.initScene();

        Inventory.init();
        inventoryView = new InventoryView(stage, skin);

        ball = add("ball", BaseModels.createSphere(1, BaseMaterials.emit(Color.GREEN)));
        wrapRigidBody(ball, 1, BaseShapes.createSphereShape(ball.modelInstance));
        ball.moveTo(new Vector3(30, 30, 30));

        boolean hi = GeneralSettings.isAtLeast(GeneralSettings.Setting.High);
        FileHandle layer1Tex = hi ? Gdx.files.internal("assets/textures/Ground023_2K_Color.png") : Gdx.files.internal("assets/textures/Ground023_1K_Color.png");
        FileHandle layer2Tex = hi ? Gdx.files.internal("assets/textures/Grass004_2K_Color.png") : Gdx.files.internal("assets/textures/Grass004_1K_Color.png");
        FileHandle layer3Tex = hi ? Gdx.files.internal("assets/textures/Ground048_2K_Color.jpg") : Gdx.files.internal("assets/textures/Ground048_1K_Color.png");
        FileHandle layer4Tex = hi ? Gdx.files.internal("assets/textures/Rock031_2K_Color.png") : Gdx.files.internal("assets/textures/Rock031_1K_Color.png");

        int tf = 1 << (GeneralSettings.current.setting.ordinal() -1);
        float uvScale = 1f;
        TerrainConfig terrainConfig = new TerrainConfig(1024/tf, 1024/tf, tf);
        terrainConfig.terrainDivideFactor = 8;
        terrainConfig.chunkLoadDistance = 100;
        terrainConfig.heightSampler = new CachingHeightSampler(new NoiseHeightSampler(1, 5, 4, 8, 4f), 10000);

        terrainConfig.addLayer(new Texture(layer1Tex), uvScale);
        terrainConfig.addLayer(new Texture(layer2Tex), uvScale);
        terrainConfig.addLayer(new Texture(layer3Tex), uvScale);
        terrainConfig.addLayer(new Texture(layer4Tex), uvScale);
        Terrain terrain = new Terrain(terrainConfig);
        terrainObject = add("terrain", terrain.createModelInstance(), terrain);

        player = addPlayer(BaseModels.createCapsule(0.3f, 1.3f, BaseMaterials.color(Color.WHITE)));
        player.mass = 75;
        if (!screenConfig.useKinematicController)
            wrapRigidBody(player, player.mass, BaseShapes.createCapsuleShape(player.modelInstance));
        player.setAngularFactor(SceneObject.LockAll);

        initialPos.y = terrain.getHeightAt(initialPos.x, initialPos.z) + 1.3f;
        player.moveTo(initialPos);
        // make sure the collision object is at the initial position
        terrain.init(initialPos);
        schedule.everyMilliSeconds(250, () -> terrain.update(player.getPosition()));

        playerInfo = new PlayerInfo();
        playerInfo.playerObject = player;
        playerInfo.displayName = player.name;
        playerInfo.icon = new TextureRegion(new Texture(Gdx.files.internal("textures/head-profile.png")));

        playerInfoView = new PlayerInfoView(playerInfo, skin);
        playerInfoView.setPosition(0, Gdx.graphics.getHeight(), Align.topLeft);
        stage.addActor(playerInfoView);

        console = new PlayerConsole(skin);
        console.setPosition(0, 0, Align.bottomLeft);
        stage.addActor(console);
        PlayerConsole.setListener(new TestConsoleListener());

        // test the console
        PlayerConsole.printf("Welcome, %s%n", player.name);

        if (screenConfig.usePlayerController) {
            BasePlayerController.ControllerConfig controllerConfig = new BasePlayerController.ControllerConfig(player, camera);
            playerController = new FirstPersonPhysicsController(controllerConfig);
            playerController.getPlayer().moveTo(initialPos);
            playerController.init();
            //playerController.grabMouse();
            addInputController(playerController);

            footSteps = new FootStepsSFX(playerController);
            footSteps.initializeDefaults();

            showCrossHair(true);

            /*
            speedLabel = new Label("00.00 m/s", skin);
            speedLabel.addAction(new Action() {
                @Override
                public boolean act(float delta) {
                    speedLabel.setText(String.format("%2.2f m/s (%d/%d)", playerController.getCurrentSpeed(),
                            (int)(player.getPosition().x / terrainConfig.chunkEdgeLength),
                            (int)(player.getPosition().z / terrainConfig.chunkEdgeLength)
                            ));
                    return false;
                }
            });
            table.row();
            table.add(speedLabel);
             */
        } else {
            camera.position.set(0, 24, -5);
            camera.lookAt(0, 23, 0);
            camera.up.set(Vector3.Y);
            camera.update();
        }

        // add some test foliage types
        foliage = new Foliage();
        Vector3 foliageCenter = new Vector3(initialPos);
        Model tree1 = new GLBLoader().load(Gdx.files.internal("models/tree1.glb")).scene.model;
        Model grass0 = new GLBLoader().load(Gdx.files.internal("models/grass0.glb")).scene.model;
        Model grass1 = new GLBLoader().load(Gdx.files.internal("models/grass1.glb")).scene.model;
        Model grass2 = new GLBLoader().load(Gdx.files.internal("models/grass2.glb")).scene.model;
        foliage.add(tree1, foliageCenter, 30, 30, terrain, Foliage.RandomizeYRotation);
        foliageCenter.x += 0.5f;
        foliage.add(grass0, foliageCenter, 100, 3000, terrain, Foliage.RandomizeYRotation);
        foliageCenter.z += 0.5f;
        foliage.add(grass1, foliageCenter, 100, 3000, terrain, Foliage.RandomizeYRotation);
        foliageCenter.x += 0.5f;
        foliage.add(grass2, foliageCenter, 100, 3000, terrain, Foliage.RandomizeYRotation);
        foliage.add(tree1, new Vector3(94, 0, 201), 60, 120, terrain, Foliage.RandomizeYRotation);
        foliage.setCamera(camera);
        foliage.setCameraMaxDist(GeneralSettings.current.foliageMaxDistance);
        add(foliage);

        // add a model
        Vector3 npcLocation = new Vector3(initialPos).add(0, 4, 7);
        SceneAsset sceneAsset = new GLBLoader().load(Gdx.files.internal("models/MH-male-Walk.glb"));
        AnimatedModelInstance npcInstance = new AnimatedModelInstance(sceneAsset.scene.model, "Game_engine");
        npcInstance.animationController.setAnimation(npcInstance.animations.first().id, -1);
        npcInstance.transform.setTranslation(npcLocation);
        sceneManager.getRenderableProviders().add(npcInstance);
        NpcObject npc1 = addNpc("npc1", npcInstance, 0.3f, 1.68f);
        npc1.moveTo(npcLocation);

        showStats(screenConfig.showStats);
    }

    @Override
    protected String updateStats() {
        return " T:" + terrainObject.getTerrainInstance().vertices + " " + terrainObject.getTerrainInstance().visibleNodes;
    }

    @Override
    protected void initDecals() {
        super.initDecals();
        playerDecal = Decal.newDecal(new TextureRegion(new Texture(Gdx.files.internal("assets/textures/questionmark.png"))), true);
        playerDecal.setDimensions(0.5f, 0.5f);
    }

    @Override
    public void updateController(float delta) {
        if (playerController != null) {
            playerController.update(delta);
            footSteps.update(delta);
            if (playerController.getCameraToPlayerDistance() > 0) {
                playerDecal.setPosition(player.getPosition());
                playerDecal.translate(0, 1.5f, 0);
                playerDecal.lookAt(camera.position, camera.up);
                decalBatch.add(playerDecal);
            }
        }
    }

    @Override
    public void updateScene(float delta) {
        super.updateScene(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.L) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            lightControlsOn = !lightControlsOn;
            showLightControls(lightControlsOn);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            if (sceneManager.getRenderableProviders().contains(foliage, true))
                sceneManager.getRenderableProviders().removeValue(foliage, true);
            else sceneManager.getRenderableProviders().add(foliage);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
            terrainObject.getTerrainInstance().debugBounds = true;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.D) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            if (debugDrawer.getDebugMode() > 0)
                debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_NoDebug);
            else
                debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawAabb);
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            PickResult picked = Physics.pick(camera, 100);
            if (picked != null && picked.hasHit() && picked.pickedObject != null) {
                if (picked.pickedObject == terrainObject) {
                    Vector3 terrainLocal = new Vector3(picked.hitPosition);
                    terrainObject.worldToLocalLocation(terrainLocal);
                    int splat = terrainObject.terrain.getSplatAt(terrainLocal.x, terrainLocal.z);
                    Vector3 n = new Vector3();
                    terrainObject.terrain.getNormalAt(terrainLocal.x, terrainLocal.z, n);
                    float h = terrainObject.terrain.getHeightAt(terrainLocal.x, terrainLocal.z);
                    System.out.printf("splat at %s: %08x, height %f, normal %s%n", terrainLocal, splat, h, n);
                    debug.drawLine("hit normal", picked.hitPosition, new Vector3(picked.hitPosition).add(n), Color.YELLOW);
                } else {
                    System.out.printf("We hit something! (%s)%n", picked.pickedObject.name);
                    BaseShapes.dumpRigidBody(picked.pickedObject.rigidBody);
                }
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.I) && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))) {
            if (inventoryView.isVisible())
                inventoryView.hideInventory();
            else
                inventoryView.showPlayerInventory();
        }

        // test item pickup
        if (Gdx.input.isKeyJustPressed(Input.Keys.N) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            GameItem bread = Inventory.PlayerInventory.newGameItemInstance("Bread", 0, 0, 1, 1);
            Inventory.PlayerInventory.requestItemPickup(bread);
            inventoryView.showInventory(Inventory.PlayerInventory);
        }

        if (screenConfig.usePlayerController && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            if (playerController.isMouseGrabbed()) {
                playerController.releaseMouse();
                enableMouseInUI();
            } else {
                playerController.grabMouse();
                disableMouseInUI();
            }
            showCrossHair(playerController.isMouseGrabbed());
        }

        // add a mini game ;)
        if (ball.getPosition().y < -200) {
            Vector3 newPos = new Vector3(camera.position).add(MathUtils.random(-3f, 3f), 10, MathUtils.random(-3f, 3f));
            ball.moveTo(newPos);
        }
        if (player.getPosition().y < -200) {
            terrainObject.terrain.ensureChunkLoaded(initialPos, true);
            player.moveTo(initialPos);
        }

        schedule.update();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void updatePostRender(float delta) {
        super.updatePostRender(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new SelectScreen(game));
        }
    }

    @Override
    public void hide() {
        if (screenConfig.usePlayerController)
            playerController.releaseMouse();
        super.hide();
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
