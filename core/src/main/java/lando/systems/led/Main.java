package lando.systems.led;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.xpenatan.imgui.ImGui;
import com.github.xpenatan.imgui.ImGuiExt;
import com.github.xpenatan.imgui.ImGuiInt;
import com.github.xpenatan.imgui.enums.ImGuiConfigFlags;
import com.github.xpenatan.imgui.enums.ImGuiInputTextFlags;
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
        camera.setToOrtho(false, Config.viewport_width, Config.viewport_height);

        imgui_input = new ImGuiGdxInput();
        Gdx.input.setInputProcessor(imgui_input);
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

        imgui.update();
        camera.update();
    }

    @Override
    public void render() {
        update();

        ScreenUtils.clear(Color.DARK_GRAY);

        // build imgui frame
        ImGui.Begin("Level Editor (LED)");
        {
            ImGui.Text("Grid Size");
            ImGui.InputInt("", grid, 4, 16);
            ImGui.SliderInt("", grid, 4, 256);
            if (ImGui.Button("Reset", 115, 20)) {
                grid.setValue(default_grid);
            }
        }
        ImGui.End();
        ImGui.Render();

        // draw scene
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        {
            var steps = 300;
            var scale = 2000f;
            var width = 1f;
            var color = Color.LIGHT_GRAY;
            int grid_size = grid.getValue();
            // TODO: clip to only draw range within current camera bounds
            for (int y = -steps; y <= steps; y++) {
                for (int x = -steps; x <= steps; x++) {
                    drawer.line(x * grid_size, -scale, x * grid_size, scale, color, width);
                    drawer.line(-scale, y * grid_size, scale, y * grid_size, color, width);
                }
            }
            drawer.line(-scale, 0, scale, 0, Color.RED, 4);
            drawer.line(0, -scale, 0, scale, Color.GREEN, 4);
        }
        batch.end();

        // draw ui
        imgui.render(ImGui.GetDrawData());
    }

}