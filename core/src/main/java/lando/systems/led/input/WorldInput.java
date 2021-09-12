package lando.systems.led.input;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.github.xpenatan.imgui.ImGui;
import com.github.xpenatan.imgui.ImGuiString;
import lando.systems.led.Assets;
import lando.systems.led.utils.Point;
import lando.systems.led.utils.RectI;
import lando.systems.led.world.Layer;
import lando.systems.led.world.Level;
import lando.systems.led.world.World;
import space.earlygrey.shapedrawer.ShapeDrawer;

import static lando.systems.led.world.Level.DragHandle.Dir.*;

public class WorldInput extends InputAdapter {

    final OrthographicCamera camera;
    final MouseButtons mouse_buttons;
    final World world;

    final Vector3 touch_screen;
    final Vector3 touch_world;

    boolean show_new_level_button;
    public Point new_level_pos;
    public ImGuiString imgui_level_name_string;
    public ImGuiString imgui_world_name_string;

    private Level.DragHandle active_handle;
    private Point move_center;
    private boolean painting;

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
        this.imgui_level_name_string = new ImGuiString();
        this.imgui_world_name_string = new ImGuiString(world.name);
        this.active_handle = null;
        this.show_new_level_button = false;
        this.painting = false;
    }

    public void update(float dt) {
        var active_level = world.get_active_level();
        if (active_level != null) {
            // scale the font
            Assets.font.getData().setScale(camera.zoom * 0.5f);

            // update handles
            for (var handle : active_level.drag_handles.values()) {
                // update hover state for rendering
                var contains_mouse = (handle.dir == center)
                        ? active_level.pixel_bounds.contains(Inputs.mouse_world)
                        : handle.circle.contains(Inputs.mouse_world.x, Inputs.mouse_world.y);
                handle.hovered = contains_mouse || handle == active_handle;

                // update effective radius to maintain consistent size on screen
                handle.circle.radius = handle.world_radius * camera.zoom;
            }
            active_level.update_handles();

            if (!show_new_level_button) {
                if (mouse_buttons.left_mouse_down && active_handle != null) {
                    var mouse = Inputs.mouse_world;
                    var bounds = active_level.pixel_bounds;
                    var radius = active_handle.circle.radius;

                    // drag to move the active handle, relative to touch point
                    // NOTE: the +1s are to adjust for fractional positions on a pixel grid
                    var dx = move_center.x - touch_world.x;
                    var dy = move_center.y - touch_world.y;

                    if (active_handle.dir == left) active_level.set_left_bound(mouse.x + dx + radius + 1);
                    if (active_handle.dir == down) active_level.set_down_bound(mouse.y + dy + radius + 1);

                    if (active_handle.dir == up)    active_level.set_up_bound   (mouse.y + dy - radius + 1);
                    if (active_handle.dir == right) active_level.set_right_bound(mouse.x + dx - radius + 1);

                    if (active_handle.dir == center) {
                        active_level.set_center_pos(
                                mouse.x + dx - bounds.w / 2f,
                                mouse.y + dy - bounds.h / 2f);
                    }
                }
            }

            // check for tile layer touch
            // TODO: splat all selected tiles; see render_selected_paint_tiles()
            if (painting && mouse_buttons.left_mouse_down) {
                var selected_tiles = Inputs.tileset_input.selected_tiles;
                var tiles_layer = active_level.get_layer(Layer.Tiles.class);
                if (tiles_layer != null && !selected_tiles.isEmpty()) {
                    var tile_data = (Layer.TileData) tiles_layer.data;
                    var grid_attrib = tiles_layer.get_attribute(Layer.GridAttrib.class);
                    if (tile_data.visible && grid_attrib != null) {
                        var grid_size = grid_attrib.size;
                        var tile_rect = RectI.pool.obtain();
                        {
                            for (var tile : tile_data.tiles) {
                                tile_rect.set(
                                        active_level.pixel_bounds.x + tile.grid.x * grid_size,
                                        active_level.pixel_bounds.y + tile.grid.y * grid_size,
                                        grid_size, grid_size);
                                if (tile_rect.contains(Inputs.mouse_world)) {
                                    // TODO: splat all selected tiles at the mouse position
                                    tile.tileset_index = selected_tiles.first();
                                    break;
                                }
                            }
                        }
                        RectI.pool.free(tile_rect);
                    }
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
                new_level_pos = Point.pool.obtain().set(touch_world);
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
                    // check for tile layer touch to draw tiles
                    //   also done in update() to handle drag painting,
                    //   could tighten it up a bit, but the flag is set here
                    {
                        var tiles_layer = active_level.get_layer(Layer.Tiles.class);
                        if (tiles_layer != null && !Inputs.tileset_input.selected_tiles.isEmpty()) {
                            var tile_data = (Layer.TileData) tiles_layer.data;
                            var grid_attrib = tiles_layer.get_attribute(Layer.GridAttrib.class);
                            if (tile_data.visible && grid_attrib != null) {
                                var grid_size = grid_attrib.size;
                                var tile_rect = RectI.pool.obtain();
                                for (var tile : tile_data.tiles) {
                                    tile_rect.set(
                                            active_level.pixel_bounds.x + tile.grid.x * grid_size,
                                            active_level.pixel_bounds.y + tile.grid.y * grid_size,
                                            grid_size, grid_size);
                                    if (tile_rect.contains(touch_world)) {
                                        painting = true;
                                        RectI.pool.free(tile_rect);
                                        return true;
                                    }
                                }
                                RectI.pool.free(tile_rect);
                            }
                        }
                    }

                    // check for drag handle touch
                    for (var handle : active_level.drag_handles.values()) {
                        var contains_touch = (handle.dir == center)
                                ? active_level.pixel_bounds.contains(touch_world)
                                : handle.circle.contains(touch_world.x, touch_world.y);
                        if (contains_touch) {
                            active_handle = handle;
                            touched_handle = true;
                            move_center = Point.at(active_handle.circle.x, active_handle.circle.y);

                            var tile_layer = (Layer.Tiles) active_level.get_layer(Layer.Tiles.class);
                            if (tile_layer != null) {
                                var tile_data = (Layer.TileData) tile_layer.data;
                                tile_data.visible = false;
                            }
                            break;
                        }
                    }
                }

                if (!touched_handle) {
                    // check for touch on a different level
                    var clicked_level = world.pick_level_at(touch_world);
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
            painting = false;

            // let go of drag handle
            if (active_handle != null) {
                active_handle = null;

                var active_level = world.get_active_level();
                if (active_level != null) {
                    // reconcile a same-axis edge crossover
                    // ie. maintain positive width, height
                    var bounds = active_level.pixel_bounds;
                    if (bounds.w < 0) {
                        bounds.w = Math.abs(bounds.w);
                        bounds.x -= bounds.w;
                    }
                    if (bounds.h < 0) {
                        bounds.h = Math.abs(bounds.h);
                        bounds.y -= bounds.h;
                    }

                    // if this level has a tile layer, regenerate it based on the new size
                    // TODO: at the moment this just blows away existing tiles,
                    //  it should try to keep them if possible, otherwise prompt for destructive edit
                    var tile_layer = (Layer.Tiles) active_level.get_layer(Layer.Tiles.class);
                    if (tile_layer != null) {
                        tile_layer.regenerate();
                        var tile_data = (Layer.TileData) tile_layer.data;
                        tile_data.visible = true;
                    }
                }
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

    public void render_new_level_button(ShapeDrawer drawer) {
        if (show_new_level_button) {
            var radius = 3;
            var pos = new_level_pos;
            drawer.filledCircle(pos.x, pos.y, radius, Color.LIME);
        }
    }

    public void render_selected_paint_tiles(SpriteBatch batch) {
        var active_level = world.get_active_level();
        if (active_level == null) return;

        var selected_tiles = Inputs.tileset_input.selected_tiles;
        var has_selected_tiles = !selected_tiles.isEmpty();

        var tile_layer = active_level.get_layer(Layer.Tiles.class);
        var has_tile_layer = tile_layer != null;

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
            for (int i = 0; i < selected_tiles.size; i++) {
                var selected_tile_id = selected_tiles.get(i);
                var ix = selected_tile_id % tileset.cols;
                var iy = selected_tile_id / tileset.cols;
                batch.setColor(1f, 1f, 1f, 0.5f);
                batch.draw(tileset.get(selected_tile_id), x + ix * grid_size, y - iy * grid_size, grid_size, grid_size);
                batch.setColor(Color.WHITE);
            }
        }
    }

}
