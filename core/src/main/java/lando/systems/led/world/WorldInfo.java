package lando.systems.led.world;

import com.badlogic.gdx.utils.Array;
import lombok.Data;

@Data
public class WorldInfo {

    private Array<LevelInfo> levels;

    public WorldInfo() {
        this.levels = new Array<>();
    }

}
