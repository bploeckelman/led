package lando.systems.led.world;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.github.xpenatan.imgui.ImGuiInt;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class World {

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

    public void render(ShapeDrawer drawer, SpriteBatch batch) {
        for (var level : levels) {
            var is_active = (level == active_level);
            level.render(drawer, batch, is_active);
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

    public void delete_active_level() {
        if (active_level != null) {
            levels.removeValue(active_level, true);
            active_level = null;

            var next_active_level = levels.random();
            if (next_active_level != null) {
                active_level = next_active_level;
            }
        }
    }

    public Level get_active_level() {
        return active_level;
    }

}
