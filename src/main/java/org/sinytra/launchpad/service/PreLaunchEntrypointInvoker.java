package org.sinytra.launchpad.service;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforgespi.transformation.ClassProcessorProvider;
import org.sinytra.launchpad.impl.LaunchpadImpl;
import org.slf4j.Logger;

public class PreLaunchEntrypointInvoker implements ClassProcessorProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void createProcessors(Context context, Collector collector) {
        if (LaunchpadImpl.hasLoadingError()) {
            LOGGER.error("Skipping fabric prelaunch entrypoint invocation due to previous error");
            return;
        }

        // Ensure loader has initialized its entrypoint storage
        FabricLoaderImpl.INSTANCE.setup();

        invokePreLaunchEntrypoints();
    }

    private static void invokePreLaunchEntrypoints() {
        LOGGER.debug("Invoking Fabric preLaunch entrypoint");

        ProgressMeter progress = StartupNotificationManager.prependProgressBar("[Launchpad] PreLaunch", 0);
        
        ScopedValue.where(LaunchpadImpl.LOADING, true).run(() -> {
            try {
                FabricLoader.getInstance().invokeEntrypoints("preLaunch", PreLaunchEntrypoint.class, PreLaunchEntrypoint::onPreLaunch);
            } catch (Throwable t) {
                LOGGER.error("Failed to invoke prelaunch entrypoint", t);
                LaunchpadImpl.addLoadingException(t, "Failed to invoke prelaunch entrypoint");
            }
        });
        
        progress.complete();
    }
}
