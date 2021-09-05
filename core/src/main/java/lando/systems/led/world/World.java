package lando.systems.led.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.github.xpenatan.imgui.ImGuiInt;
import com.github.xpenatan.imgui.ImGuiString;
import lando.systems.led.input.Inputs;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class World {

    public final ImGuiInt grid;

    public String name;

    Level active_level;
    final Array<Level> levels;

    public World(String name) {
        this.name = name;
        this.levels = new Array<>();
        this.grid = new ImGuiInt(Level.default_grid_size);
        this.active_level = null;
    }

    public void update(float dt) {

    }

    public void render(ShapeDrawer drawer, SpriteBatch batch) {
        for (var level : levels) {
            var is_active = (level == active_level);
            level.render(drawer, batch, is_active);
        }
    }

    public void add_level(Level new_level) {
        levels.add(new_level);
        make_active(new_level);
    }

    public boolean is_active(Level level) {
        return (level == active_level);
    }

    public Level get_active_level() {
        return active_level;
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

    public Level pick_level_at(Vector3 point) {
        return pick_level_at(point.x, point.y);
    }

    public Level pick_level_at(Vector2 point) {
        return pick_level_at(point.x, point.y);
    }

    public Level pick_level_at(float x, float y) {
        return pick_level_at((int) x, (int) y);
    }

    public Level pick_level_at(int x, int y) {
        for (var level : levels) {
            if (level.pixel_bounds.contains(x, y)) {
                return level;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // Serialization / deserialization

    private final Json json_wrangler = new Json(JsonWriter.OutputType.javascript);

    public void save() {
        var info = new WorldInfo();
        info.setName(name);

        for (var level : levels) {
            var level_json = level.get_info_json(json_wrangler);
            var level_info = json_wrangler.fromJson(LevelInfo.class, level_json);
            info.getLevels().add(level_info);
        }

        var file = Gdx.files.local("levels/world-test.json");
        var info_json = json_wrangler.prettyPrint(info);
        file.writeString(info_json, false);
    }

    public void load() {
        // TODO: add ui in the caller, select list of files in levels/
        //  then change signature to load(filename)
        var filename = "levels/world-test.json";

        var file = Gdx.files.local(filename);
        if (file.exists()) {
            var json = file.readString();
            var info = json_wrangler.fromJson(WorldInfo.class, json);
            if (info != null) {
                name = info.getName();
                Inputs.world_input.imgui_world_name_string = new ImGuiString(name);

                levels.clear();
                for (var level_info : info.getLevels()) {
                    var level = new Level(level_info);
                    add_level(level);
                }
            }
        }
    }

}
