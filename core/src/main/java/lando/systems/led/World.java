package lando.systems.led;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.github.xpenatan.imgui.ImGuiInt;
import lando.systems.led.utils.RectI;
import space.earlygrey.shapedrawer.JoinType;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class World {

    private final Color corner = new Color(0x32cd32ff);
    private final Color corner_dim = new Color(0x129d1233);
    private final Color outline = new Color(0xffd700ff);
    private final Color outline_dim = new Color(0xaf770033);
    private final Color highlight = new Color(0xdaa5203f);
    private final Color highlight_dim = new Color(0xca951033);

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
            drawer.filledRectangle(level.x, level.y, level.w, level.h, highlight_dim);
            drawer.setColor(outline_dim);
            drawer.rectangle(level.x, level.y, level.w, level.h, 2f, JoinType.SMOOTH);
            drawer.setColor(Color.WHITE);
            drawer.filledCircle(level.x ,level.y, 3f, corner_dim);
        }
    }

}
