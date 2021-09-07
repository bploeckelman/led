package lando.systems.led;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class Assets {

    public static BitmapFont font;
    public static GlyphLayout layout;
    public static Texture outline;

    public static void init() {
        var fontFile = Gdx.files.internal("dogicapixel.ttf");
        var generator = new FreeTypeFontGenerator(fontFile);
        var parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 16;
        parameter.mono = true;
        parameter.color = Color.LIGHT_GRAY;
        parameter.borderColor = new Color(0.1f, 0.1f, 0.1f, 1f);
        parameter.shadowColor = Color.BLACK;
        parameter.borderWidth = 1;
        parameter.shadowOffsetX = 1;
        parameter.shadowOffsetY = 1;
        font = generator.generateFont(parameter);
        generator.dispose();

        layout = new GlyphLayout();

        // make a little outline image
        var pixmap = new Pixmap(8, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.drawRectangle(0, 0, 8, 8);
        pixmap.setColor(Color.CLEAR);
        pixmap.drawRectangle(1, 1, 7, 7);
        outline = new Texture(pixmap);
        pixmap.dispose();
    }

    public static void dispose() {
        font.dispose();
        outline.dispose();
    }

}
