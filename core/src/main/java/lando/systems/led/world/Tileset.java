package lando.systems.led.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

public class Tileset implements Disposable {

    public String filename;
    public Texture texture;
    public int grid_size;

    private TextureRegion[][] textures;

    public void load(String filename, int grid_size) {
        if (texture != null) {
            dispose();
        }

        this.filename = filename;
        this.texture = new Texture(filename);
        this.textures = TextureRegion.split(texture, grid_size, grid_size);
        this.grid_size = grid_size;
    }

    public TextureRegion get(int index) {
        int x = index / grid_size;
        int y = index % grid_size;

        if (x < 0 || y < 0 || x >= grid_size || y >= grid_size) {
            return null;
        }

        return textures[x][y];
    }

    @Override
    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    }

}
