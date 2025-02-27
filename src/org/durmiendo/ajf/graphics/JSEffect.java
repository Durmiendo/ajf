package org.durmiendo.ajf.graphics;

import arc.graphics.g2d.Draw;
import arc.util.Log;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.mod.Scripts;
import rhino.*;

import java.util.concurrent.atomic.AtomicBoolean;

public class JSEffect extends Effect {
    public String js = "";
    public String effectContainerName = "e";

    public static EffectContainer ef;

    public JSEffect() {
        final Function[] f = new Function[1];
        Scripts c = Vars.mods.getScripts();
        AtomicBoolean errored = new AtomicBoolean(false);

        renderer = effectContainer -> {
            if (errored.get()) {
                lifetime = 240;
                layer = 120;
                Draw.rect("ohno", effectContainer.x, effectContainer.y, effectContainer.fin() * 360);
                return;
            }
            if (f[0] == null) {
                try {
                    ef = effectContainer;
                    String scriptText = "try {\n" +
                            "  (function() {\n" +
                            "    return function(" + effectContainerName + ") {\n" +
                            "      " + js + "\n" +
                            "    }\n" +
                            "  })()\n" +
                            "} catch (e) {\n" +
                            "  throw new Error(e.toString());\n" +
                            "}";
                    Object compiledScript = c.context.evaluateString(c.scope, scriptText, "", 0);

                    if (compiledScript instanceof Function) {
                        f[0] = (Function) compiledScript;
                    } else {
                        throw new RuntimeException("Failed to compile script: " + scriptText);
                    }
                } catch (Exception e) {
                    Log.info("Failed to compile script: " + js);
                    errored.set(true);
                }
            }

            try {
                f[0].call(c.context, c.scope, c.scope, new Object[]{ef});
            } catch (Exception e) {
                Log.err("Failed to execute script: " + js, e);
                errored.set(true);
            }
        };
    }
}
