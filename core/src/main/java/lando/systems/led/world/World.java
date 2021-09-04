package lando.systems.led.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.github.xpenatan.imgui.ImGuiInt;
import space.earlygrey.shapedrawer.JoinType;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class World {

    private final Color corner = new Color(0x32cd32ff);
    private final Color corner_dim = new Color(0x129d1233);
    private final Color outline = new Color(0xffd700ff);
    private final Color outline_dim = new Color(0xaf770033);
    private final Color highlight = new Color(0xdaa5203f);
    private final Color highlight_dim = new Color(0xca951033);

    public final ImGuiInt grid;

    final Array<Level> levels;
    Level active_level;

    public World() {
        this.levels = new Array<>();
        this.grid = new ImGuiInt(Level.default_grid_size);
        this.active_level = null;
    }

    public void update(float dt) {

    }

    public void add_level(Level new_level) {
        levels.add(new_level);
        active_level = new_level;
    }

    public void render(ShapeDrawer drawer) {
        // draw outlines for the existing levels in the world
        for (var level : levels) {
            var is_active = (level == active_level);

            drawer.filledRectangle(level.pixel_bounds.x, level.pixel_bounds.y, level.pixel_bounds.w, level.pixel_bounds.h, is_active ? highlight : highlight_dim);
            drawer.setColor(is_active ? outline : outline_dim);
            drawer.rectangle(level.pixel_bounds.x, level.pixel_bounds.y, level.pixel_bounds.w, level.pixel_bounds.h, 2f, JoinType.SMOOTH);
            drawer.setColor(Color.WHITE);
            drawer.filledCircle(level.pixel_bounds.x ,level.pixel_bounds.y, 3f, is_active ? corner : corner_dim);
        }
    }

    public boolean is_active(Level level) {
        return (level == active_level);
    }

    public void make_active(Level level) {
        if (level != null) {
            if (levels.contains(level, true)) {
                active_level = level;
            } else {
                add_level(level);
            }
        }
    }

    public Level pick_level_at(int x, int y) {
        for (var level : levels) {
            if (level.pixel_bounds.contains(x, y)) {
                return level;
            }
        }
        return null;
    }

    public void delete_active_world() {
        if (active_level != null) {
            levels.removeValue(active_level, true);
            active_level = null;

            var next_active_level = levels.random();
            if (next_active_level != null) {
                active_level = next_active_level;
            }
        }
    }

}
