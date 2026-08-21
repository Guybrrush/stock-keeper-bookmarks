package com.stockkeeperbookmarks;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Bookmarks, stored per Stock Keeper.
 *
 * Create's menu hands the client the real block entity, so a Stock Keeper can be identified
 * without any server-side help. The key is scoped by world as well as position, because two
 * different saves can easily both have a keeper at the same coordinates.
 *
 * A keeper with no entry of its own shows {@link AddressBookConfig#addresses()} — editing it
 * is what promotes it to an entry, so untouched keepers cost nothing and keep working after
 * an upgrade from the days when the list was global.
 */
public final class BookmarkStore {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int VERSION = 1;

	private static Map<String, List<String>> entries;
	/** Set when the file on disk is newer than this build understands; blocks all writes. */
	private static boolean readOnly;

	private BookmarkStore() {
	}

	private static Path file() {
		return FMLPaths.CONFIGDIR.get().resolve("stockkeeperbookmarks-bookmarks.json");
	}

	/**
	 * Identity of one Stock Keeper: which world, which dimension, which block.
	 * Null when the block entity could not be resolved — callers fall back to the defaults.
	 */
	public static String keyFor(BlockEntity blockEntity) {
		if (blockEntity == null)
			return null;
		Level level = blockEntity.getLevel();
		if (level == null)
			return null;
		BlockPos pos = blockEntity.getBlockPos();
		return worldScope() + "|" + level.dimension().location() + "|"
			+ pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}

	/** Distinguishes saves and servers, so identical coordinates in two worlds stay separate. */
	private static String worldScope() {
		Minecraft minecraft = Minecraft.getInstance();

		ServerData server = minecraft.getCurrentServer();
		if (server != null && server.ip != null && !server.ip.isBlank())
			return "server/" + server.ip;

		MinecraftServer singleplayer = minecraft.getSingleplayerServer();
		if (singleplayer != null)
			return "local/" + singleplayer.getWorldData().getLevelName();

		return "unknown";
	}

	public static List<String> get(String key) {
		if (key == null)
			return AddressBookConfig.addresses();
		List<String> stored = entries().get(key);
		return new ArrayList<>(stored != null ? stored : AddressBookConfig.addresses());
	}

	public static void set(String key, List<String> addresses) {
		if (key == null)
			return;
		entries().put(key, new ArrayList<>(addresses));
		save();
	}

	/** @return false if nothing was pinned — the address was blank or already present. */
	public static boolean add(String key, String address) {
		String trimmed = address == null ? "" : address.trim();
		if (key == null || trimmed.isEmpty())
			return false;
		List<String> current = get(key);
		if (current.contains(trimmed))
			return false;
		current.add(trimmed);
		set(key, current);
		return true;
	}

	public static void remove(String key, String address) {
		if (key == null)
			return;
		List<String> current = get(key);
		if (current.remove(address))
			set(key, current);
	}

	private static Map<String, List<String>> entries() {
		if (entries == null)
			load();
		return entries;
	}

	private static void load() {
		entries = new LinkedHashMap<>();
		Path path = file();
		if (!Files.exists(path))
			return;

		try (Reader reader = Files.newBufferedReader(path)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			// Refuse to read a file a newer build wrote. Loading it would silently drop
			// whatever fields this version does not know about, and the first edit would
			// write that loss back over the original.
			int version = root.has("version") ? root.get("version").getAsInt() : VERSION;
			if (version > VERSION) {
				LOGGER.error("{} was written by a newer version ({} > {}); not loading it, and "
					+ "no bookmarks will be saved this session", path, version, VERSION);
				readOnly = true;
				return;
			}

			JsonObject keepers = root.getAsJsonObject("keepers");
			if (keepers == null)
				return;
			for (Map.Entry<String, JsonElement> keeper : keepers.entrySet()) {
				List<String> addresses = new ArrayList<>();
				for (JsonElement address : keeper.getValue().getAsJsonArray())
					addresses.add(address.getAsString());
				entries.put(keeper.getKey(), addresses);
			}
		} catch (Exception e) {
			// A malformed file must not take the GUI down with it; start empty instead.
			LOGGER.error("Could not read {}, starting with no saved bookmarks", path, e);
		}
	}

	private static void save() {
		if (readOnly)
			return;

		JsonObject keepers = new JsonObject();
		entries().forEach((key, addresses) -> {
			JsonArray array = new JsonArray();
			addresses.forEach(array::add);
			keepers.add(key, array);
		});

		JsonObject root = new JsonObject();
		root.addProperty("version", VERSION);
		root.add("keepers", keepers);

		Path path = file();
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(root, writer);
		} catch (Exception e) {
			LOGGER.error("Could not write {}", path, e);
		}
	}
}
