package lando.systems.led.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.github.xpenatan.imgui.ImGui;
import lando.systems.led.utils.Point;
import lando.systems.led.world.Level;
import lando.systems.led.world.World;

public class WorldInput extends InputAdapter {

    final OrthographicCamera camera;
    final MouseButtons mouse_buttons;
    final World world;

    // TODO: put in a global input class?
    final Vector3 touch_screen;
    final Vector3 touch_world;

    // TODO: temporarily exposed until moved to a global input class
    public final Vector3 mouse_screen;
    public final Vector3 mouse_world;

    // exposed because it impacts camera input, handled in Main
    public boolean show_new_level_button;

    public Point new_level_pos;

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

        var active_level = world.get_active_level();
        if (active_level != null) {
            active_level.left_handle_active   = active_level.left_handle   .contains(mouse_world.x, mouse_world.y);
            active_level.right_handle_active  = active_level.right_handle  .contains(mouse_world.x, mouse_world.y);
            active_level.top_handle_active    = active_level.top_handle    .contains(mouse_world.x, mouse_world.y);
            active_level.bottom_handle_active = active_level.bottom_handle .contains(mouse_world.x, mouse_world.y);
        }
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

    private boolean hide_new_level_button() {
        boolean did_hide = show_new_level_button;
        show_new_level_button = false;
        if (new_level_pos != null) {
            Point.pool.free(new_level_pos);
        }
        return did_hide;
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
        else if (button == Input.Buttons.MIDDLE) {
            hide_new_level_button();
            return true;
        }
        else if (button == Input.Buttons.LEFT) {
            var dismissed_button = hide_new_level_button();
            if (!dismissed_button) {
                // check whether we clicked on an existing level
                var clicked_level = world.pick_level_at((int) touch_world.x, (int) touch_world.y);
                if (clicked_level != null && !world.is_active(clicked_level)) {
                    world.make_active(clicked_level);
                }
            }
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

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.DEL
         || keycode == Input.Keys.FORWARD_DEL) {
            // TODO: prompt before deleting
            world.delete_active_level();
            return true;
        }
        return false;
    }

}
