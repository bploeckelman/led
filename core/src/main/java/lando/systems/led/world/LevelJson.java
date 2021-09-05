package lando.systems.led.world;

import lando.systems.led.utils.RectI;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LevelJson {

    private String name;
    private RectI pixel_bounds;

}
