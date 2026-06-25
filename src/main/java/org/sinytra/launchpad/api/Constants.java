package org.sinytra.launchpad.api;

import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.sinytra.launchpad.ImplConstants;

public final class Constants {
    // Public mod metadata properties
    public static final String ENABLE_LAUNCHPAD = "launchpad:compatible";
    public static final String OVERRIDES = "launchpad:overrides";
    
    // Services
    public static final ProcessorName AT_PROCESSOR = new ProcessorName(ImplConstants.NAMESPACE, "access_transformer");

    private Constants() {}
}
