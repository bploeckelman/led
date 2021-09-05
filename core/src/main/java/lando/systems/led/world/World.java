package lando.systems.led.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.github.xpenatan.imgui.ImGuiInt;
import com.github.xpenatan.imgui.ImGuiString;
import lando.systems.led.input.Inputs;
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
        make_active(new_level);
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

        if (active_level != null) {
            // NOTE: calling setValue() on an ImGuiString doesn't entirely clear the buffer
            //  so as a workaround recreate it with the appropriate name instead
            Inputs.world_input.imgui_level_name_string = new ImGuiString(active_level.name);
            Inputs.camera_input.center_on_level(active_level);
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
                Inputs.camera_input.center_on_level(active_level);
            }
        }
    }

    public Level get_active_level() {
        return active_level;
    }

    public void save() {
        for (var level : levels) {
            level.save_to_file();
        }
    }

    private Json json_wrangler = new Json();
    public void load() {
        var levels_folder = Gdx.files.local("levels");
        if (levels_folder.isDirectory()) {
            var files = levels_folder.list(".json");
            for (var file : files) {
                var json = file.readString();
                var info = json_wrangler.fromJson(LevelJson.class, json);
                if (info != null) {
                    var level = new Level(info);
                    if (level != null) {
                        add_level(level);
                    }
                }
            }
        }
    }

}
