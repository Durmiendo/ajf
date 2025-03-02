package org.durmiendo.ajf.mods.jcfg;

import arc.files.Fi;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.durmiendo.mods.parser.jcfg.JCFG;
import org.durmiendo.mods.parser.jcfg.JCFGLexer;

public class JCFGModParser {
    public void parse(Fi fi) {
        try {
            var handler = new JCFGHandler(new GenericJCFGUtils());
            handler.handleFile(
                    new JCFG(
                            new CommonTokenStream(
                                    new JCFGLexer(
                                            new ANTLRInputStream(
                                                    fi.read()
                                            )
                                    )
                            )
                    ).file(), fi
            );
            handler.dispose();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
