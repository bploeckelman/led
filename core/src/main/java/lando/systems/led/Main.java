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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.xpenatan.imgui.ImGui;
import com.github.xpenatan.imgui.ImGuiExt;
import com.github.xpenatan.imgui.ImGuiInt;
import com.github.xpenatan.imgui.enums.ImGuiConfigFlags;
import com.github.xpenatan.imgui.enums.ImGuiInputTextFlags;
import com.github.xpenatan.imgui.gdx.ImGuiGdxImpl;
import com.github.xpenatan.imgui.gdx.ImGuiGdxInput;
import lando.systems.led.input.CameraInput;
import lando.systems.led.input.Inputs;
import lando.systems.led.input.TilesetInput;
import lando.systems.led.input.WorldInput;
import lando.systems.led.world.Layer;
import lando.systems.led.world.World;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class Main extends ApplicationAdapter {

    SpriteBatch batch;
    ShapeDrawer drawer;
    Texture pixel;

    ImGuiGdxImpl imgui;
    ImGuiGdxInput imgui_input;

    OrthographicCamera camera;
    CameraInput camera_input;
    GestureDetector camera_input_gesture;

    World world;
    WorldInput world_input;
    TilesetInput tileset_input;

    private Texture background;
    private OrthographicCamera screen_camera;

    @Override
    public void create() {
        Assets.init();

        batch = new SpriteBatch();
        drawer = new ShapeDrawer(batch);

        background = new Texture(Gdx.files.internal("background.png"));
        screen_camera = new OrthographicCamera();
        screen_camera.setToOrtho(false, Config.window_width, Config.window_height);
        screen_camera.update();

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
        world = new World("New");

        camera_input = new CameraInput(camera);
        camera_input_gesture = new GestureDetector(camera_input);
        world_input = new WorldInput(world, camera);
        tileset_input = new TilesetInput(world, screen_camera);
        imgui_input = new ImGuiGdxInput();
        Inputs.init(world_input, camera_input, tileset_input);

        // NOTE: apparently ImGuiGdxInput doesn't respect the input mux bubbling order?
        //  touches that are eaten by TilesetInput still trigger button presses and such in ImGuiGdxInput
        var input_mux = new InputMultiplexer(
                  tileset_input
                , imgui_input
                , camera_input
                , camera_input_gesture
                , world_input
        );
        Gdx.input.setInputProcessor(input_mux);
    }

    @Override
    public void dispose() {
        Assets.dispose();
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
        tileset_input.update(dt);

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
        batch.setProjectionMatrix(screen_camera.combined);
        batch.begin();
        {
            // scale up so it's not as dense of a grid
            var scale = 4f;
            batch.draw(background, 0, 0,
                    screen_camera.viewportWidth * scale,
                    screen_camera.viewportHeight * scale);
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

                // it's a little silly to do this all inline here
                var active_level = world.get_active_level();
                if (active_level != null) {
                    var tile_layer = active_level.get_layer(Layer.Tiles.class);
                    var has_tile_layer = tile_layer != null;
                    var has_selected_tiles = !tileset_input.selected_tiles.isEmpty();
                    var paint_tiles = has_selected_tiles && has_tile_layer;
                    if (paint_tiles) {
                        var tileset = tile_layer.get_attribute(Layer.TilesetAttrib.class).tileset;
                        var grid_size = tile_layer.get_attribute(Layer.GridAttrib.class).size;

                        // round mouse position to grid boundaries in world space relative to the active level
                        var level_x = active_level.pixel_bounds.x;
                        var level_y = active_level.pixel_bounds.y;
                        var grid_x = MathUtils.floor((Inputs.mouse_world.x - level_x) / grid_size);
                        var grid_y = MathUtils.floor((Inputs.mouse_world.y - level_y) / grid_size);
                        var x = level_x + grid_x * grid_size;
                        var y = level_y + grid_y * grid_size;

                        // draw selected tiles in world space, clamped to grid boundaries
                        for (int i = 0; i < tileset_input.selected_tiles.size; i++) {
                            var selected_tile_id = tileset_input.selected_tiles.get(i);
                            var ix = selected_tile_id % tileset.cols;
                            var iy = selected_tile_id / tileset.cols;
                            batch.setColor(1f, 1f, 1f, 0.5f);
                            batch.draw(tileset.get(selected_tile_id), x + ix * grid_size, y - iy * grid_size, grid_size, grid_size);
                            batch.setColor(Color.WHITE);
                        }
                    }
                }
            }
        }
        batch.end();

        // draw ui
        imgui.render(ImGui.GetDrawData());

        // draw overlay
        batch.setProjectionMatrix(screen_camera.combined);
        batch.begin();
        {
            {
                tileset_input.render_gui(drawer, batch);
            }

            var font = Assets.font;
            var layout = Assets.layout;
            var prev_scale_x = font.getData().scaleX;
            var prev_scale_y = font.getData().scaleY;
            {
                var margin = 5;
                font.getData().setScale(1.4f);
                layout.setText(font, world.name, world_name_color, screen_camera.viewportWidth, Align.right, false);
                font.draw(batch, layout, -margin, screen_camera.viewportHeight - margin);
                var world_name_height = layout.height;

                var active_level = world.get_active_level();
                if (active_level != null) {
                    font.getData().setScale(1.33f);
                    layout.setText(font, active_level.name, level_name_color, screen_camera.viewportWidth, Align.right, false);
                    font.draw(batch, layout, -margin, screen_camera.viewportHeight - margin - world_name_height - margin);
                }
            }
            font.getData().setScale(prev_scale_x, prev_scale_y);
            font.setColor(Color.WHITE);
        }
        batch.end();
    }
    private final Color world_name_color = new Color(1f, 1f, 0.5f, 0.66f);
    private final Color level_name_color = new Color(0.8f, 0.8f, 0.4f, 0.4f);

    private void build_imgui_sidebar() {
        float sidebar_w = 200;
        float sidebar_h = Gdx.graphics.getHeight();
        float titlebar_h = 40;
        ImGui.SetNextWindowPos(0, 0);
        ImGui.SetNextWindowContentSize(sidebar_w, sidebar_h - titlebar_h);
        ImGui.SetNextWindowSizeConstraints(sidebar_w, sidebar_h, sidebar_w, sidebar_h);
        ImGui.Begin("Level Editor");
        {
            ImGui.LabelText("pos", String.format("(%d, %d)", (int) Inputs.mouse_world.x, (int) Inputs.mouse_world.y));
            ImGui.LabelText("zoom", String.format("%.2f", camera.zoom));

            if (ImGui.Button("Reset Camera", 100, 25)) {
                camera_input.reset_camera();
            }

            ImGui.Separator();

            if (ImGui.InputText("World", world_input.imgui_world_name_string, ImGuiInputTextFlags.EnterReturnsTrue)) {
                world.name = world_input.imgui_world_name_string.getValue();
            }

            var active_level = world.get_active_level();
            if (active_level != null) {
                if (ImGui.InputText("Level", world_input.imgui_level_name_string, ImGuiInputTextFlags.EnterReturnsTrue)) {
                    active_level.name = world_input.imgui_level_name_string.getValue();
                }
            }

            ImGui.Separator();

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

            {
                if (active_level != null) {
                    if (ImGui.Button("Add Tiles")) {
                        if (!active_level.has_layer(Layer.Tiles.class)) {
                            var layer = new Layer.Tiles(active_level);
                            var tileset_attrib = layer.get_attribute(Layer.TilesetAttrib.class);
                            tileset_attrib.tileset.load("tilesets/paper-pixels8.png", 8);
                            active_level.add_layer(layer);
                        }
                    }
                    ImGui.SameLine();
                    if (ImGui.Button("Add Entities")) {
                        if (!active_level.has_layer(Layer.Entities.class)) {
                            var layer = new Layer.Entities(active_level);
                            active_level.add_layer(layer);
                        }
                    }
                    // TODO: info about layers for active level
                    // [x] layers
                    //  - identifiers
                    //  - attributes
                    //  - metadata

                    if (!active_level.layers.isEmpty()) {
                        var label = "layers";
                        var current_item = new ImGuiInt(active_level.layers.size());
                        var items = active_level.layers.stream()
                                .map(Layer::name).toArray(String[]::new);
                        var item_count = items.length;
                        ImGui.ListBox(label, current_item, items, item_count);
                    }
                }
            }
        }
        ImGui.End();
    }

}