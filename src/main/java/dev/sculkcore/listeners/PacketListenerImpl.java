package dev.sculkcore.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import dev.sculkcore.SculkCorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class PacketListenerImpl extends PacketListenerAbstract implements Listener {
    private final SculkCorePlugin plugin;
    private final Set<Integer> playerEntityIds = ConcurrentHashMap.newKeySet();
    private static final String MAX_HEALTH_ATTR = "minecraft:generic.max_health";

    public PacketListenerImpl(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerEntityIds.add(event.getPlayer().getEntityId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerEntityIds.remove(event.getPlayer().getEntityId());
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        Player player = (Player) event.getPlayer();
        if (player == null) return;

        com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon type = event.getPacketType();

        // 1. Anti-Seed Cracker
        if (plugin.getConfig().getBoolean("rules.ban_seed_cracking", true)) {
            if (type == PacketType.Play.Server.JOIN_GAME) {
                WrapperPlayServerJoinGame wrapper = new WrapperPlayServerJoinGame(event);
                long seed = wrapper.getHashedSeed();
                wrapper.setHashedSeed(randomizeHashedSeed(seed));
            } else if (type == PacketType.Play.Server.RESPAWN) {
                WrapperPlayServerRespawn wrapper = new WrapperPlayServerRespawn(event);
                long seed = wrapper.getHashedSeed();
                wrapper.setHashedSeed(randomizeHashedSeed(seed));
            }
        }

        // 2. Anti-Health Indicators
        if (plugin.getConfig().getBoolean("rules.anti_health_indicators", false)) {
            if (type == PacketType.Play.Server.ENTITY_METADATA) {
                WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
                int entityId = wrapper.getEntityId();
                if (entityId != player.getEntityId() && playerEntityIds.contains(entityId)) {
                    List<EntityData> metadata = new ArrayList<>(wrapper.getEntityMetadata());
                    boolean modified = false;
                    for (int i = 0; i < metadata.size(); i++) {
                        EntityData data = metadata.get(i);
                        if (data.getIndex() == 9) { // Index 9 is health in player metadata
                            float val = ((Number) data.getValue()).floatValue();
                            if (val >= 1.0f) {
                                metadata.set(i, new EntityData(9, EntityDataTypes.FLOAT, 1.0f));
                                modified = true;
                            }
                        }
                    }
                    if (modified) {
                        wrapper.setEntityMetadata(metadata);
                        event.markForReEncode(true);
                    }
                }
            } else if (type == PacketType.Play.Server.UPDATE_ATTRIBUTES) {
                WrapperPlayServerUpdateAttributes wrapper = new WrapperPlayServerUpdateAttributes(event);
                int entityId = wrapper.getEntityId();
                if (entityId != player.getEntityId() && playerEntityIds.contains(entityId)) {
                    List<WrapperPlayServerUpdateAttributes.Property> properties = wrapper.getProperties();
                    boolean modified = false;
                    for (WrapperPlayServerUpdateAttributes.Property prop : properties) {
                        if (prop.getKey().equalsIgnoreCase(MAX_HEALTH_ATTR)) {
                            prop.setValue(2.0); // Spoof max health to 1 heart (2.0)
                            modified = true;
                        }
                    }
                    if (modified) {
                        event.markForReEncode(true);
                    }
                }
            }
        }
    }

    private long randomizeHashedSeed(long seed) {
        int length = Long.toString(seed).length();
        if (length > 18) {
            length = 18;
        }
        long min = (long) Math.pow(10.0, length - 1);
        long max = (long) (Math.pow(10.0, length) - 1.0);
        return ThreadLocalRandom.current().nextLong(min, max + 1L);
    }
}
