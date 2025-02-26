package org.durmiendo.ajf.world;

import arc.Events;
import arc.func.Prov;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.*;
import mindustry.mod.Scripts;
import mindustry.type.UnitType;
import rhino.Context;
import rhino.Function;
import rhino.Scriptable;
import rhino.ScriptableObject;

import java.lang.reflect.Proxy;

public class ExtendUnitType extends UnitType {
    public float mass = -1f;
    public String constructorName = "";
    public static Class<? extends Unit> unitClass; // <Unit>

    public ExtendUnitType(String name) {
        super(name);
        Events.on(EventType.ClientLoadEvent.class, e -> {
            unitClass = switch(constructorName) {
                case "mech" -> MechUnit.class;
                case "legs" -> LegsUnit.class;
                case "naval" -> UnitWaterMove.class;
                case "payload" -> PayloadUnit.class;
                case "missile" -> TimedKillUnit.class;
                case "tank" -> TankUnit.class;
                case "hover" -> ElevationMoveUnit.class;
                case "tether" -> BuildingTetherPayloadUnit.class;
                case "crawl" -> CrawlUnit.class;
                default -> UnitEntity.class;
            };

            try {
                String script = "(function() {\n" +
                                "    return function(b) {\n" +
                                "        b.constructor = () => extend(" + unitClass.getSimpleName() + ", {mass() {return " + mass + "}});\n" +
                                "    };\n" +
                                "})();\n";
                
                Scripts s = Vars.mods.getScripts();
                Function f = (Function) s.context.evaluateString(s.scope, script, "", 1);
                f.call(s.context, s.scope, s.scope, new Object[]{this});
            } catch (Exception ex) {
                Log.err("Failed to extend unit constructor: " + name, ex);
            }
        });

    }
}
