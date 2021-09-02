package lando.systems.led;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.xpenatan.imgui.ImGui;
import com.github.xpenatan.imgui.ImGuiExt;
import com.github.xpenatan.imgui.enums.ImGuiConfigFlags;
import com.github.xpenatan.imgui.gdx.ImGuiGdxImpl;
import com.github.xpenatan.imgui.gdx.ImGuiGdxInput;

/* https://github.com/xpenatan/jDear-imgui/ */
public class Main extends ApplicationAdapter {

    SpriteBatch batch;
    ImGuiGdxImpl imgui;
    ImGuiGdxInput imgui_input;

    @Override
    public void create() {
        batch = new SpriteBatch();

        ImGui.init();
        ImGuiExt.init();
        ImGui.GetIO().SetConfigFlags(ImGuiConfigFlags.DockingEnable);
        ImGui.GetIO().SetDockingFlags(false, false, false, false);

        imgui = new ImGuiGdxImpl();
        // TODO: add ui controls

        imgui_input = new ImGuiGdxInput();
        Gdx.input.setInputProcessor(imgui_input);
    }

    @Override
    public void dispose() {
        ImGui.dispose();
        imgui.dispose();
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        // ...
    }

    @Override
    public void render() {
        imgui.update();

        ScreenUtils.clear(Color.SKY);

        // build imgui frame
        ImGui.Begin("Level Editor (LED)");
        ImGui.Text("beep");
        ImGui.End();
        ImGui.Render();

        imgui.render(ImGui.GetDrawData());
    }

}