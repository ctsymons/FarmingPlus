package com.example.croppy;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class CroppyArmorEffects {
	private static final Identifier HEARTY_MODIFIER_ID = Identifier.fromNamespaceAndPath(CroppyMod.MOD_ID, "hearty_bonus_health");
	private static final double HEALTH_PER_LEVEL = 0.5D;

	private CroppyArmorEffects() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				updateHeartyModifier(player);
			}
		});
	}

	private static void updateHeartyModifier(ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}

		int heartyLevel = CroppyEnchantmentUtil.getTotalHeartyLevel(player);
		if (heartyLevel <= 0) {
			maxHealth.removeModifier(HEARTY_MODIFIER_ID);
			if (player.getHealth() > player.getMaxHealth()) {
				player.setHealth(player.getMaxHealth());
			}
			return;
		}

		maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
			HEARTY_MODIFIER_ID,
			heartyLevel * HEALTH_PER_LEVEL,
			AttributeModifier.Operation.ADD_VALUE
		));

		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}
}
