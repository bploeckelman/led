package lando.systems.led.world;

import com.badlogic.gdx.utils.Array;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WorldInfo {

    private String name;
    private Array<LevelInfo> levels = new Array<>();

}
