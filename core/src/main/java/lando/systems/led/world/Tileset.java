package lando.systems.led.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

public class Tileset implements Disposable {

    public String filename;
    public Texture texture;
    public int grid_size;
    public int cols;
    public int rows;

    private TextureRegion[][] textures;

    public void load(String filename, int grid_size) {
        if (texture != null) {
            dispose();
        }

        this.filename = filename;
        this.texture = new Texture(filename);
        this.textures = TextureRegion.split(texture, grid_size, grid_size);
        this.grid_size = grid_size;
        this.cols = textures[0].length;
        this.rows = textures.length;
    }

    public TextureRegion get(int index) {
        int x = index % cols;
        int y = index / cols;

        if (x < 0 || y < 0 || x >= cols || y >= rows) {
            return null;
        }
        return textures[y][x];
    }

    public TextureRegion get(int x, int y) {
        if (x < 0 || y < 0 || x >= cols || y >= rows) {
            return null;
        }
        return textures[y][x];
    }

    @Override
    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    }

}
