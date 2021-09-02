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
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.xpenatan.imgui.ImGui;
import com.github.xpenatan.imgui.ImGuiExt;
import com.github.xpenatan.imgui.ImGuiInt;
import com.github.xpenatan.imgui.enums.ImGuiConfigFlags;
import com.github.xpenatan.imgui.gdx.ImGuiGdxImpl;
import com.github.xpenatan.imgui.gdx.ImGuiGdxInput;
import space.earlygrey.shapedrawer.ShapeDrawer;

/* https://github.com/xpenatan/jDear-imgui/ */
public class Main extends ApplicationAdapter {

    SpriteBatch batch;
    ShapeDrawer drawer;
    Texture pixel;
    ImGuiGdxImpl imgui;
    ImGuiGdxInput imgui_input;

    final int default_grid = 16;
    ImGuiInt grid;

    OrthographicCamera camera;
    CameraInput camera_input;

    @Override
    public void create() {
        batch = new SpriteBatch();
        drawer = new ShapeDrawer(batch);

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
        grid = new ImGuiInt(default_grid);

        camera = new OrthographicCamera();

        camera_input = new CameraInput(camera);
        imgui_input = new ImGuiGdxInput();
        var input_mux = new InputMultiplexer(
                imgui_input,
                camera_input,
                new GestureDetector(camera_input)
        );
        Gdx.input.setInputProcessor(input_mux);
    }

    @Override
    public void dispose() {
        ImGui.dispose();
        imgui.dispose();
        batch.dispose();
        pixel.dispose();
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

        camera_input.update(dt);

        imgui.update();
    }

    @Override
    public void render() {
        update();

        ScreenUtils.clear(0.2f, 0.2f, 0.2f, 1f);

        // build imgui frame
        ImGui.Begin("Level Editor (LED)");
        {
            if (ImGui.Button("Reset", 100, 25)) {
                grid.setValue(default_grid);
                camera_input.reset_camera();
            }

            ImGui.Separator();

            ImGui.Text("Grid Size");
            ImGui.InputInt("", grid, 4, 16);
            ImGui.SliderInt("", grid, 4, 256);
        }
        ImGui.End();
        ImGui.Render();

        // draw scene
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        {
            // draw grid
            var steps = 300;
            var scale = 2000f;
            var width = 1f;
            var color = Color.DARK_GRAY;
            int grid_size = grid.getValue();
            // TODO: clip to only draw range within current camera bounds
            for (int y = -steps; y <= steps; y++) {
                for (int x = -steps; x <= steps; x++) {
                    drawer.line(x * grid_size, -scale, x * grid_size, scale, color, width);
                    drawer.line(-scale, y * grid_size, scale, y * grid_size, color, width);
                }
            }

            if (camera_input.panning) {
                // draw a dimming overlay
                batch.setColor(0f, 0f, 0f, 0.2f);
                batch.draw(pixel,
                        camera.position.x - camera_input.effective_viewport.x / 2f,
                        camera.position.y - camera_input.effective_viewport.y / 2f,
                        camera_input.effective_viewport.x,
                        camera_input.effective_viewport.y);
                batch.setColor(Color.WHITE);
            }

            drawer.line(-scale, 0, scale, 0, Color.FIREBRICK, 2);
            drawer.line(0, -scale, 0, scale, Color.FOREST, 2);
        }
        batch.end();

        // draw ui
        imgui.render(ImGui.GetDrawData());
    }

}