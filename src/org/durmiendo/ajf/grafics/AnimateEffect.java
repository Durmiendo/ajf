package org.durmiendo.ajf.grafics;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import mindustry.Vars;
import mindustry.entities.Effect;

public class AnimateEffect extends Effect {
    float[] times;
    float size = 12f;
    int cadres = 0;
    TextureRegion[] regions;
    String name;

    public AnimateEffect() {
        renderer = effectContainer -> {
            if (times == null || regions == null) {
                initt();
            }
            float f = effectContainer.fin() * lifetime;
            int cadre = Mathf.floor(f);
            Draw.rect(regions[cadre], effectContainer.x, effectContainer.y, size);
        };
    }

    void initt() {
        if (times != null && cadres <= 0) {
            cadres = times.length;
        } else if (times == null) {
            times = new float[cadres];
            for (int i = 0; i < times.length; i++) {
                times[i] = (float) i / (times.length - 1) * lifetime;
            }
        } else {
            Vars.ui.showErrorMessage("times is not null for AnimateEffect");
            return;
        }

        if (name == null) {
            Vars.ui.showErrorMessage("name is null for AnimateEffect");
            return;
        }

        if (regions == null) {
            regions = new TextureRegion[cadres];
            for (int i = 0; i < regions.length; i++) {
                regions[i] = Core.atlas.find(name + "-" + (i+1));
            }
        }

    }
}
