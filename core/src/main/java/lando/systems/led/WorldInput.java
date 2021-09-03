package lando.systems.led;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.github.xpenatan.imgui.ImGui;
import lando.systems.led.utils.RectI;

public class WorldInput extends InputAdapter {

    final OrthographicCamera camera;
    final Vector3 touch_screen;
    final Vector3 touch_world;
    final Vector3 mouse_screen;
    final Vector3 mouse_world;
    final MouseButtons mouse_buttons;
    final World world;

    public RectI selected_level;
    public boolean show_new_level_button;

    static class MouseButtons {
        boolean left_mouse_down;
        boolean middle_mouse_down;
        boolean right_mouse_down;
    }

    public WorldInput(World world, OrthographicCamera camera) {
        this.world = world;
        this.camera = camera;
        this.touch_screen = new Vector3();
        this.touch_world = new Vector3();
        this.mouse_screen = new Vector3();
        this.mouse_world = new Vector3();
        this.mouse_buttons = new MouseButtons();
        this.selected_level = null;
        this.show_new_level_button = false;
    }

    public void update(float dt) {
        mouse_screen.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        mouse_world.set(mouse_screen);
        camera.unproject(mouse_world);

        if (selected_level != null) {
            if (mouse_buttons.left_mouse_down) {
                var grid_size = world.grid.getValue();

                // round appropriately to make sure the cell
                // that contains the mouse is always included
                // in the selection
                int grid_x = (mouse_world.x <= 0f)
                        ? MathUtils.floor(mouse_world.x / grid_size)
                        : MathUtils.ceil(mouse_world.x / grid_size);
                int grid_y = (mouse_world.y <= 0f)
                        ? MathUtils.floor(mouse_world.y / grid_size)
                        : MathUtils.ceil(mouse_world.y / grid_size);

                var selected_grid_x = (selected_level.x / grid_size);
                var selected_grid_y = (selected_level.y / grid_size);
                var w = (grid_x - selected_grid_x) * grid_size;
                var h = (grid_y - selected_grid_y) * grid_size;

                // require a minimum of 1 cell highlighted
                selected_level.w = (Math.abs(w) >= grid_size) ? w : grid_size;
                selected_level.h = (Math.abs(h) >= grid_size) ? h : grid_size;
            }
        }
    }

    public void build_imgui_data() {
        if (show_new_level_button) {
            var button_w = 200;
            var button_h = 50;
            var button_x = touch_screen.x;
            var button_y = touch_screen.y;
            ImGui.SetNextWindowBgAlpha(0.5f);
            ImGui.SetNextWindowPos(button_x, button_y);
            ImGui.Begin("New Level");
            {
                if (ImGui.Button("Add Level", button_w, button_h))
                {
                    show_new_level_button = false;
                }
            }
            ImGui.End();
        }
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        touch_screen.set(x, y, 0);
        touch_world.set(touch_screen);
        camera.unproject(touch_world);

        switch (button) {
            case Input.Buttons.LEFT   -> mouse_buttons.left_mouse_down   = true;
            case Input.Buttons.MIDDLE -> mouse_buttons.middle_mouse_down = true;
            case Input.Buttons.RIGHT  -> mouse_buttons.right_mouse_down  = true;
        }

        if (button == Input.Buttons.RIGHT) {
            show_new_level_button = !show_new_level_button;
            return true;
        }

        if (button == Input.Buttons.MIDDLE) {
            show_new_level_button = false;
            return true;
        }

        if (selected_level == null && mouse_buttons.left_mouse_down) {
            var grid_size = world.grid.getValue();
            int grid_x = MathUtils.floor(mouse_world.x / grid_size);
            int grid_y = MathUtils.floor(mouse_world.y / grid_size);
            var clamp_x = grid_x * grid_size;
            var clamp_y = grid_y * grid_size;
            selected_level = RectI.of(clamp_x, clamp_y, grid_size, grid_size);
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        switch (button) {
            case Input.Buttons.LEFT   -> mouse_buttons.left_mouse_down   = false;
            case Input.Buttons.MIDDLE -> mouse_buttons.middle_mouse_down = false;
            case Input.Buttons.RIGHT  -> mouse_buttons.right_mouse_down  = false;
        }

        if (button == Input.Buttons.LEFT) {
            show_new_level_button = false;

            // TODO: create new level here with default params

            // TODO: shouldn't always add, sometimes a click gets through from imGui when it shouldn't like a reset button click
            if (selected_level != null) {
                world.add_level(selected_level);
                selected_level = null;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        mouse_world.set(x, y, 0);
        camera.unproject(mouse_world);
        return false;
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        mouse_world.set(x, y, 0);
        camera.unproject(mouse_world);
        return false;
    }

    @Override
    public boolean scrolled(float x, float y) {
        return false;
    }

}
