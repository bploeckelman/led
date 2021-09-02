package lando.systems.led;

import com.badlogic.gdx.utils.Array;
import com.github.xpenatan.imgui.ImGuiInt;
import lando.systems.led.utils.RectI;

public class World {

    public static final int default_grid = 16;
    public final ImGuiInt grid;

    final Array<RectI> levels;

    public World() {
        this.levels = new Array<>();
        this.grid = new ImGuiInt(default_grid);
    }

    public void update(float dt) {

    }

    public void add_level(RectI level) {
        // todo
    }

}
