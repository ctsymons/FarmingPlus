package com.ctsymons.fplus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class PlayerPlacedPumpkinsData extends SavedData {
	private static final String DATA_ID = "fplus_player_placed_pumpkins";
	private static final Codec<PlayerPlacedPumpkinsData> CODEC = Codec.LONG.listOf()
		.xmap(PlayerPlacedPumpkinsData::new, PlayerPlacedPumpkinsData::positionsAsList);
	private static final SavedDataType<PlayerPlacedPumpkinsData> TYPE =
		new SavedDataType<>(DATA_ID, PlayerPlacedPumpkinsData::new, CODEC, DataFixTypes.LEVEL);

	private final Set<Long> positions;

	private PlayerPlacedPumpkinsData() {
		this.positions = new HashSet<>();
	}

	private PlayerPlacedPumpkinsData(List<Long> positions) {
		this.positions = new HashSet<>(positions);
	}

	public static void markPlaced(ServerLevel level, BlockPos pos) {
		PlayerPlacedPumpkinsData data = get(level);
		if (data.positions.add(pos.asLong())) {
			data.setDirty();
		}
	}

	public static void clear(ServerLevel level, BlockPos pos) {
		PlayerPlacedPumpkinsData data = get(level);
		if (data.positions.remove(pos.asLong())) {
			data.setDirty();
		}
	}

	public static boolean clearIfPresent(ServerLevel level, BlockPos pos) {
		PlayerPlacedPumpkinsData data = get(level);
		if (!data.positions.remove(pos.asLong())) {
			return false;
		}

		data.setDirty();
		return true;
	}

	public static boolean isPlayerPlaced(ServerLevel level, BlockPos pos) {
		return get(level).positions.contains(pos.asLong());
	}

	private static PlayerPlacedPumpkinsData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	private List<Long> positionsAsList() {
		return new ArrayList<>(this.positions);
	}
}
