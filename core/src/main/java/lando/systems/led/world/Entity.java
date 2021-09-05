package lando.systems.led.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import lando.systems.led.utils.Point;
import lando.systems.led.utils.RectI;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class Entity {

    public final RectI bounds = RectI.zero();
    public final Point origin = Point.zero();

    public void render(ShapeDrawer drawer, SpriteBatch batch) {
        drawer.filledRectangle(bounds.x, bounds.y, bounds.w, bounds.h, Color.TEAL);
        drawer.rectangle(bounds.x, bounds.y, bounds.w, bounds.h, Color.SKY);
        drawer.filledCircle(bounds.x + origin.x, bounds.y + origin.y, 3, Color.ORANGE);
    }

}
