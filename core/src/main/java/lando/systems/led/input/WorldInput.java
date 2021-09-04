package lando.systems.led.input;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.github.xpenatan.imgui.ImGui;
import lando.systems.led.utils.Point;
import lando.systems.led.world.Level;
import lando.systems.led.world.World;

import static lando.systems.led.world.Level.DragHandle.Dir.*;

public class WorldInput extends InputAdapter {

    final OrthographicCamera camera;
    final MouseButtons mouse_buttons;
    final World world;

    // TODO: put in a global input class or have a separate one per *Input class?
    final Vector3 touch_screen;
    final Vector3 touch_world;

    private Level.DragHandle active_handle;

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
        this.mouse_buttons = new MouseButtons();
        this.active_handle = null;
        this.show_new_level_button = false;
    }

    public void update(float dt) {
        var active_level = world.get_active_level();
        if (active_level != null) {
            // scale the level's fonts
            Level.font.getData().setScale(camera.zoom * 0.5f);

            // update handles
            for (var handle : active_level.drag_handles.values()) {
                // update hover state for rendering
                var contains_mouse = (handle.dir == center)
                        ? active_level.pixel_bounds.contains(Inputs.mouse_world.x, Inputs.mouse_world.y)
                        : handle.circle.contains(Inputs.mouse_world.x, Inputs.mouse_world.y);
                handle.hovered = contains_mouse || handle == active_handle;
                // update effective radius to maintain consistent size on screen
                handle.circle.radius = handle.world_radius * camera.zoom;
            }
            active_level.update_handles();

            if (!show_new_level_button) {
                if (mouse_buttons.left_mouse_down && active_handle != null) {
                    var mouse = Inputs.mouse_world;

                    if (active_handle.dir == left)   active_level.set_left_bound(mouse.x);
                    if (active_handle.dir == right)  active_level.set_right_bound(mouse.x);
                    if (active_handle.dir == up)     active_level.set_up_bound(mouse.y);
                    if (active_handle.dir == down)   active_level.set_down_bound(mouse.y);

                    // TODO: don't use mouse.x/y directly as it's offset from the existing center by some amount
                    //   because of this, the level 'jumps' a bit when it's first made active in order to make
                    //   the level center be where the user just clicked.
                    //   instead account for that and offset the move based on the relative position of the mouse from the level center
                    if (active_handle.dir == center) active_level.set_center_bound(
                            mouse.x - active_level.pixel_bounds.w / 2f,
                            mouse.y - active_level.pixel_bounds.h / 2f);
                }
            }
        }
    }

    public void update_gui() {
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
                    ImGui.SetWindowFocus(null);

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
            case Buttons.LEFT   -> mouse_buttons.left_mouse_down   = true;
            case Buttons.MIDDLE -> mouse_buttons.middle_mouse_down = true;
            case Buttons.RIGHT  -> mouse_buttons.right_mouse_down  = true;
        }

        if (button == Buttons.RIGHT) {
            show_new_level_button = !show_new_level_button;

            if (show_new_level_button) {
                new_level_pos = Point.pool.obtain().set((int) touch_world.x, (int) touch_world.y);
            } else {
                hide_new_level_button();
            }
            return true;
        }
        else if (button == Buttons.MIDDLE) {
            hide_new_level_button();
            return true;
        }
        else if (button == Buttons.LEFT) {
            var dismissed_button = hide_new_level_button();
            if (!dismissed_button) {
                var touched_handle = false;
                var active_level = world.get_active_level();
                if (active_level != null) {
                    // check for drag handle touch
                    for (var handle : active_level.drag_handles.values()) {
                        var contains_touch = (handle.dir == center)
                                ? active_level.pixel_bounds.contains(touch_world.x, touch_world.y)
                                : handle.circle.contains(touch_world.x, touch_world.y);
                        if (contains_touch) {
                            active_handle = handle;
                            touched_handle = true;
                            break;
                        }
                    }
                }

                if (!touched_handle) {
                    // check for touch on a different level
                    var clicked_level = world.pick_level_at((int) touch_world.x, (int) touch_world.y);
                    if (clicked_level != null && !world.is_active(clicked_level)) {
                        world.make_active(clicked_level);
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        switch (button) {
            case Buttons.LEFT   -> mouse_buttons.left_mouse_down   = false;
            case Buttons.MIDDLE -> mouse_buttons.middle_mouse_down = false;
            case Buttons.RIGHT  -> mouse_buttons.right_mouse_down  = false;
        }

        if (button == Buttons.LEFT) {
            if (active_handle != null) {
                active_handle = null;
            }
        }

        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Keys.DEL
         || keycode == Keys.FORWARD_DEL) {
            // TODO: prompt before deleting
            world.delete_active_level();
            return true;
        }

        if (keycode == Keys.SPACE
         || keycode == Keys.ENTER
         || keycode == Keys.NUMPAD_ENTER) {
            Inputs.camera_input.center_on_level(world.get_active_level());
        }
        return false;
    }

}
