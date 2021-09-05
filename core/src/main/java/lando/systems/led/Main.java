package lando.systems.led;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.xpenatan.imgui.ImGui;
import com.github.xpenatan.imgui.ImGuiExt;
import com.github.xpenatan.imgui.enums.ImGuiConfigFlags;
import com.github.xpenatan.imgui.enums.ImGuiInputTextFlags;
import com.github.xpenatan.imgui.gdx.ImGuiGdxImpl;
import com.github.xpenatan.imgui.gdx.ImGuiGdxInput;
import lando.systems.led.input.CameraInput;
import lando.systems.led.input.Inputs;
import lando.systems.led.input.WorldInput;
import lando.systems.led.world.Level;
import lando.systems.led.world.World;
import space.earlygrey.shapedrawer.ShapeDrawer;

/* https://github.com/xpenatan/jDear-imgui/ */
public class Main extends ApplicationAdapter {

    SpriteBatch batch;
    ShapeDrawer drawer;
    Texture pixel;

    ImGuiGdxImpl imgui;
    ImGuiGdxInput imgui_input;

    OrthographicCamera camera;
    CameraInput camera_input;

    World world;
    WorldInput world_input;

    private Texture background;
    private Matrix4 screen_matrix;

    @Override
    public void create() {
        batch = new SpriteBatch();
        drawer = new ShapeDrawer(batch);

        background = new Texture(Gdx.files.internal("background.png"));
        screen_matrix = new Matrix4().setToOrtho2D(0, 0, Config.viewport_width, Config.viewport_height);

        // build a temp 1 pixel texture for testing shapedrawer
        var pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.drawPixel(0, 0);
        pixel = new Texture(pixmap);
        pixmap.dispose();

        drawer.setTextureRegion(new TextureRegion(pixel));

        ImGui.init();
        ImGuiExt.init();
        ImGui.GetIO().SetConfigFlags(ImGuiConfigFlags.DockingEnable);
        ImGui.GetIO().SetDockingFlags(false, false, false, false);

        imgui = new ImGuiGdxImpl();

        camera = new OrthographicCamera();
        world = new World();

        camera_input = new CameraInput(camera);
        world_input = new WorldInput(world, camera);
        imgui_input = new ImGuiGdxInput();
        Inputs.init(world_input, camera_input);

        var input_mux = new InputMultiplexer(
                  imgui_input
                , camera_input // as InputProcessor
                , new GestureDetector(camera_input)
                , world_input
        );
        Gdx.input.setInputProcessor(input_mux);
    }

    @Override
    public void dispose() {
        ImGui.dispose();
        imgui.dispose();
        batch.dispose();
        pixel.dispose();
        background.dispose();
    }

    @Override
    public void resize(int width, int height) {
        // ...
    }

    public void update() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        var dt = Gdx.graphics.getDeltaTime();

        Inputs.update();

        camera_input.update(dt);
        world_input.update(dt);

        imgui.update();
    }

    @Override
    public void render() {
        update();

        ScreenUtils.clear(0.2f, 0.2f, 0.2f, 1f);
        batch.setProjectionMatrix(camera.combined);

        // build imgui frame
        {
            build_imgui_sidebar();
            world_input.update_gui();
        }
        ImGui.Render();

        // draw background
        batch.setProjectionMatrix(screen_matrix);
        batch.begin();
        {
            batch.draw(background, -Config.window_width / 2f, -Config.window_height / 2f, Config.window_width, Config.window_height);
        }
        batch.end();

        // draw scene
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        {
            // draw a dimming overlay
            if (camera_input.panning) {
                batch.setColor(0f, 0f, 0f, 0.2f);
                batch.draw(pixel,
                        camera.position.x - camera_input.effective_viewport.x / 2f,
                        camera.position.y - camera_input.effective_viewport.y / 2f,
                        camera_input.effective_viewport.x,
                        camera_input.effective_viewport.y);
                batch.setColor(Color.WHITE);
            }

            // draw coordinate axes
            {
                var scale = 2000f;
                drawer.line(-scale, 0, scale, 0, Color.FIREBRICK, 2);
                drawer.line(0, -scale, 0, scale, Color.FOREST, 2);
            }

            // draw the world
            {
                world.render(drawer, batch);

                if (world_input.is_showing_new_level_button()) {
                    var radius = 3;
                    var pos = world_input.new_level_pos;
                    drawer.filledCircle(pos.x, pos.y, radius, Color.LIME);
                }
            }
        }
        batch.end();

        // draw ui
        imgui.render(ImGui.GetDrawData());
    }

    private void build_imgui_sidebar() {
        float sidebar_w = 200;
        float sidebar_h = Gdx.graphics.getHeight();
        float titlebar_h = 40;
        ImGui.SetNextWindowPos(0, 0);
        ImGui.SetNextWindowContentSize(sidebar_w, sidebar_h - titlebar_h);
        ImGui.SetNextWindowSizeConstraints(sidebar_w, sidebar_h, sidebar_w, sidebar_h);
        ImGui.Begin("Level Editor");
        {
            ImGui.LabelText(" ", String.format(" pos: (%d, %d)", (int) Inputs.mouse_world.x, (int) Inputs.mouse_world.y));
            ImGui.LabelText(" ", String.format("tile: (%d, %d)", (int) Inputs.mouse_world.x / world.grid.getValue(), (int) Inputs.mouse_world.y / world.grid.getValue()));
            ImGui.LabelText(" ", String.format("zoom: %.2f", camera.zoom));

            if (ImGui.Button("Reset Camera", 100, 25)) {
                world.grid.setValue(Level.default_grid_size);
                camera_input.reset_camera();
            }

            ImGui.Separator();

            var active_level = world.get_active_level();
            if (active_level != null) {
                if (ImGui.InputText("Level", world_input.imgui_level_name_string, ImGuiInputTextFlags.EnterReturnsTrue)) {
                    active_level.name = world_input.imgui_level_name_string.getValue();
                }
            }

            {
                if (ImGui.Button("Save World")) {
                    world.save();
                }
                ImGui.SameLine();
                if (ImGui.Button("Load World")) {
                    world.load();
                }
            }

            ImGui.Separator();

            ImGui.Text("Grid Size");
            ImGui.InputInt("", world.grid, 4, 16);
            ImGui.SliderInt("", world.grid, 4, 256);
        }
        ImGui.End();
    }

}