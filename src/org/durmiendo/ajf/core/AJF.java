package org.durmiendo.ajf.core;


import arc.struct.ObjectMap;
import arc.util.Log;
import mindustry.Vars;
import mindustry.mod.ClassMap;
import mindustry.mod.Mod;
import mindustry.mod.Scripts;
import org.durmiendo.ajf.grafics.AnimateEffect;
import org.durmiendo.ajf.grafics.JSEffect;
import org.durmiendo.ajf.world.ExtendUnitType;

import java.awt.*;
import java.lang.reflect.Field;

public class AJF extends Mod {
    public static ObjectMap<Class<?>, String> m = new ObjectMap<>();
    public static ObjectMap<String, Class<?>> tmp = new ObjectMap<>();

    public AJF() {
        tmp.put("ExtendUnitType", ExtendUnitType.class);

        tmp.put("JSEffect", JSEffect.class);
        tmp.put("AnimateEffect", AnimateEffect.class);

        StringBuilder imp = new StringBuilder();
        tmp.each((s, c) -> {
            ClassMap.classes.put(s, c);
            imp.append("const ").append(s).append("=Packages.").append(c.getName()).append(";");
        });

        Vars.mods.getScripts().context.evaluateString(Vars.mods.getScripts().scope, "(function(){" + imp + "})();", "", 0);


        ClassMap.classes.each((s, c) -> m.put(c, s));

        try {
            Field p = Vars.mods.getClass().getDeclaredField("parser");
            p.setAccessible(true);
            p.set(Vars.mods, new ContentParser());
        } catch (Exception e) {
            Log.err("Failed to set parser :(");
            throw new RuntimeException(e);
        }

    }

    @Override
    public void init() {
        super.init();
    }
}
