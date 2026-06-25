package org.sinytra.launchpad.api;

import org.sinytra.launchpad.impl.ClassTweakerConverterImpl;

import java.io.BufferedReader;
import java.io.IOException;

public interface ClassTweakerConverter {
    static String createAccessTransformer(BufferedReader reader, String fileName) throws IOException {
        return ClassTweakerConverterImpl.createAccessTransformer(reader, fileName);
    }
}
