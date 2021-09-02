package lando.systems.led;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.github.xpenatan.imgui.ImGuiInt;
import lando.systems.led.utils.RectI;
import space.earlygrey.shapedrawer.JoinType;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class World {

    private static final Color highlight = new Color(0xdaa5203f);

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
        levels.add(RectI.of(level));
    }

    public void render(ShapeDrawer drawer) {
        for (var level : levels) {
            drawer.filledRectangle(level.x, level.y, level.w, level.h, highlight);
            drawer.setColor(Color.GOLD);
            drawer.rectangle(level.x, level.y, level.w, level.h, 2f, JoinType.SMOOTH);
            drawer.setColor(Color.WHITE);
            drawer.filledCircle(level.x ,level.y, 3f, Color.LIME);
        }
    }

}
