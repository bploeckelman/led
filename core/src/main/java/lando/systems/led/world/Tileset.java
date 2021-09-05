package lando.systems.led.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.IntMap;

public class Tileset {

    public final IntMap<TextureRegion> textures = new IntMap<>();

    public void load(String filename) {
        // TODO
    }

    public TextureRegion get(int index) {
        return textures.get(index);
    }

}
