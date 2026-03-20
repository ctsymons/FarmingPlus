package com.example.croppy;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CroppyMod implements ModInitializer {
	public static final String MOD_ID = "fplus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CroppyCommand.register();
		ComposterBufferFeature.register();
		FarmersBagFeature.register();
		CroppyArmorEffects.register();
		CroppyCropDrops.register();
		LOGGER.info("Farming+ initialized");
	}
}
