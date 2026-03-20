package com.ctsymons.fplus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class ComposterBufferData extends SavedData {
	public static final int MAX_STORED_BONE_MEAL = 512;

	private static final String DATA_ID = "fplus_composter_buffer_v2";
	private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.LONG.fieldOf("pos").forGetter(Entry::pos),
		Codec.INT.fieldOf("count").forGetter(Entry::count))
		.apply(instance, Entry::new));
	private static final Codec<ComposterBufferData> CODEC = ENTRY_CODEC.listOf()
		.xmap(ComposterBufferData::new, ComposterBufferData::entries);
	private static final SavedDataType<ComposterBufferData> TYPE =
		new SavedDataType<>(DATA_ID, ComposterBufferData::new, CODEC, DataFixTypes.LEVEL);

	private final Map<Long, Integer> storedBoneMeal;

	private ComposterBufferData() {
		this.storedBoneMeal = new HashMap<>();
	}

	private ComposterBufferData(List<Entry> entries) {
		this.storedBoneMeal = new HashMap<>();
		for (Entry entry : entries) {
			if (entry.count() <= 0) {
				continue;
			}

			this.storedBoneMeal.put(entry.pos(), Math.min(MAX_STORED_BONE_MEAL, entry.count()));
		}
	}

	public static int getStored(ServerLevel level, BlockPos pos) {
		if (!isValidComposter(level, pos)) {
			clear(level, pos);
			return 0;
		}

		return get(level).storedBoneMeal.getOrDefault(pos.asLong(), 0);
	}

	public static boolean addBoneMeal(ServerLevel level, BlockPos pos, int amount) {
		if (amount <= 0 || !isValidComposter(level, pos)) {
			return false;
		}

		ComposterBufferData data = get(level);
		long key = pos.asLong();
		int current = data.storedBoneMeal.getOrDefault(key, 0);
		int next = Math.min(MAX_STORED_BONE_MEAL, current + amount);
		if (next == current) {
			return false;
		}

		data.storedBoneMeal.put(key, next);
		data.setDirty();
		return true;
	}

	public static int removeBoneMeal(ServerLevel level, BlockPos pos, int amount) {
		if (amount <= 0 || !isValidComposter(level, pos)) {
			clear(level, pos);
			return 0;
		}

		ComposterBufferData data = get(level);
		long key = pos.asLong();
		int current = data.storedBoneMeal.getOrDefault(key, 0);
		if (current <= 0) {
			return 0;
		}

		int removed = Math.min(amount, current);
		int next = current - removed;
		if (next <= 0) {
			data.storedBoneMeal.remove(key);
		} else {
			data.storedBoneMeal.put(key, next);
		}

		data.setDirty();
		return removed;
	}

	public static void clear(ServerLevel level, BlockPos pos) {
		ComposterBufferData data = get(level);
		if (data.storedBoneMeal.remove(pos.asLong()) != null) {
			data.setDirty();
		}
	}

	private static boolean isValidComposter(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof ComposterBlock;
	}

	private static ComposterBufferData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	private List<Entry> entries() {
		List<Entry> entries = new ArrayList<>(this.storedBoneMeal.size());
		for (Map.Entry<Long, Integer> entry : this.storedBoneMeal.entrySet()) {
			if (entry.getValue() != null && entry.getValue() > 0) {
				entries.add(new Entry(entry.getKey(), entry.getValue()));
			}
		}

		return entries;
	}

	private record Entry(long pos, int count) {
	}
}
