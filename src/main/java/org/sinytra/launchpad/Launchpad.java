package org.sinytra.launchpad;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Launchpad.MODID)
public class Launchpad {
    public static final String MODID = "launchpad";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Launchpad(IEventBus modEventBus, ModContainer modContainer) {
        
    }
}
