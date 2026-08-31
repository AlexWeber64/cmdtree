package org.alexweber.cmdtree;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmdTreeMod implements ClientModInitializer {
    public static final String MOD_ID = "cmdtree";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        GetCommandsCommand.register();
    }
}
