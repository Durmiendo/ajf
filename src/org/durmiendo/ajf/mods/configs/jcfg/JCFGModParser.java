package org.durmiendo.ajf.mods.configs.jcfg;

import arc.files.Fi;
import arc.util.Log;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.durmiendo.mods.parser.jcfg.JCFG;
import org.durmiendo.mods.parser.jcfg.JCFGLexer;

public class JCFGModParser {
    public void parse(Fi fi) {
        try {
            Log.info("Parsing @", fi.name());
            Log.info(new JCFGHandler().handleFile(
                            new JCFG(
                                    new CommonTokenStream(
                                            new JCFGLexer(
                                                    new ANTLRInputStream(
                                                            fi.read()
                                                    )
                                            )
                                    )
                            ).file(), fi
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
