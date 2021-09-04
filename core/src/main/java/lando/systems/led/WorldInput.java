package lando.systems.led;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.github.xpenatan.imgui.ImGui;
import lando.systems.led.utils.Point;

public class WorldInput extends InputAdapter {

    final OrthographicCamera camera;
    final MouseButtons mouse_buttons;
    final World world;

    // TODO: put in a global input class?
    final Vector3 touch_screen;
    final Vector3 touch_world;
    final Vector3 mouse_screen;
    final Vector3 mouse_world;

    // exposed because it impacts camera input, handled in Main
    public boolean show_new_level_button;

    private Point new_level_pos;

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
        this.show_new_level_button = false;
    }

    public void update(float dt) {
        mouse_screen.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        mouse_world.set(mouse_screen);
        camera.unproject(mouse_world);
    }

    public void build_imgui_data() {
        if (show_new_level_button) {
            var button_w = 200;
            var button_h = 40;
            var button_x = touch_screen.x;
            var button_y = touch_screen.y;
            ImGui.SetNextWindowBgAlpha(0.5f);
            ImGui.SetNextWindowPos(button_x, button_y);
            ImGui.Begin("New Level");
            {
                if (ImGui.Button("Add Level", button_w, button_h))
                {
                    // create a new level with default size at current location
                    var level = new Level(new_level_pos);
                    world.add_level(level);

                    hide_new_level_button();
                }
            }
            ImGui.End();
        }
    }

    private void hide_new_level_button() {
        show_new_level_button = false;
        Point.pool.free(new_level_pos);
    }

    // ------------------------------------------------------------------------
    // InputAdapter implementation

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

            if (show_new_level_button) {
                new_level_pos = Point.pool.obtain().set((int) touch_world.x, (int) touch_world.y);
            } else {
                hide_new_level_button();
            }
            return true;
        }

        if (button == Input.Buttons.MIDDLE) {
            hide_new_level_button();
            return true;
        }

        if (button == Input.Buttons.LEFT) {
            // ...
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
        return false;
    }

}
