/*
 *  Copyright (C) 2020 - 2021 Tecnio
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package me.tecnio.antihaxerman.data.processor;

import io.github.retrooper.packetevents.packetwrappers.play.in.clientcommand.WrappedPacketInClientCommand;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import io.github.retrooper.packetevents.packetwrappers.play.out.position.WrappedPacketOutPosition;
import lombok.Getter;
import me.tecnio.antihaxerman.data.PlayerData;
import me.tecnio.antihaxerman.util.PlayerUtil;
import me.tecnio.antihaxerman.util.type.BoundingBox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Vehicle;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public final class PositionProcessor {

    private final PlayerData data;

    private double x, y, z,
            lastX, lastY, lastZ,
            deltaX, deltaY, deltaZ, deltaXZ,
            lastDeltaX, lastDeltaZ, lastDeltaY, lastDeltaXZ,
            lastLastDeltaY;

    private boolean flying, inVehicle, inWater, inLava, inLiquid, fullySubmergedInLiquidStat, inAir, inWeb,
            blockNearHead, nearWall, onClimbable, onSolidGround, nearVehicle, onSlime,
            onIce, nearPiston, nearStair, pos, lastPos;

    private int airTicks, clientAirTicks, sinceVehicleTicks, sinceFlyingTicks,
            liquidTicks, sinceLiquidTicks, climbableTicks, sinceClimbableTicks,
            webTicks, sinceWebTicks, ticks,
            groundTicks, sinceTeleportTicks, sinceSlimeTicks, solidGroundTicks,
            iceTicks, sinceIceTicks, sinceBlockNearHeadTicks;

    private boolean onGround, lastOnGround, mathematicallyOnGround, lastMathGround;

    private Location location;
    private Location lastLocation;

    private final Deque<Vector> teleportList = new ArrayDeque<>();
    private boolean teleported;

    private final List<Block> blocks = new ArrayList<>();
    private final List<Block> blocksNear = new ArrayList<>();
    private List<Block> blocksBelow = new ArrayList<>();
    private List<Block> blocksAbove = new ArrayList<>();

    private List<Entity> nearbyEntities = new ArrayList<>();

    public PositionProcessor(final PlayerData data) {
        this.data = data;
    }

    public void handle(final WrappedPacketInFlying wrapper) {
        teleported = false;

        this.lastOnGround = this.onGround;
        this.onGround = wrapper.isOnGround();

        this.lastPos = pos;
        pos = wrapper.isPosition();

        if (wrapper.isPosition()) {
            lastX = this.x;
            lastY = this.y;
            lastZ = this.z;

            this.x = wrapper.getX();
            this.y = wrapper.getY();
            this.z = wrapper.getZ();

            lastLocation = location != null ? location : null;
            location = new Location(data.getPlayer().getWorld(), x, y, z);

            // lmao
            this.lastLastDeltaY = lastDeltaY;

            lastDeltaX = deltaX;
            lastDeltaY = deltaY;
            lastDeltaZ = deltaZ;

            lastDeltaXZ = deltaXZ;

            deltaX = this.x - lastX;
            deltaY = this.y - lastY;
            deltaZ = this.z - lastZ;
            deltaXZ = Math.hypot(deltaX, deltaZ);

            lastMathGround = mathematicallyOnGround;
            mathematicallyOnGround = y % 0.015625 < 0.005;

            handleCollisions(0);
            handleCollisions(1);

            if (wrapper.isLook()) {
                // Iterator used in order to prevent CME.
                final Iterator<Vector> iterator = teleportList.iterator();

                while (iterator.hasNext()) {
                    final Vector wantedLocation = iterator.next();

                    if ((wantedLocation.getX() == x
                            && wantedLocation.getY() == y
                            && wantedLocation.getZ() == z)
                            && !onGround) {
                        teleported = true;
                        sinceTeleportTicks = 0;

                        teleportList.remove(wantedLocation);
                        break;
                    }
                }
            }
        }

        handleTicks();
    }

    public void handleTicks() {
        ++ticks;

        if (onGround) ++groundTicks;
        else groundTicks = 0;

        if (inAir) {
            ++airTicks;
        } else {
            airTicks = 0;
        }

        if (!onGround) {
            ++clientAirTicks;
        } else {
            clientAirTicks = 0;
        }

        ++sinceTeleportTicks;

        if (data.getPlayer().getVehicle() != null) {
            sinceVehicleTicks = 0;
            inVehicle = true;
        } else {
            ++sinceVehicleTicks;
            inVehicle = false;
        }

        if (onIce) {
            ++iceTicks;
            sinceIceTicks = 0;
        } else {
            iceTicks = 0;
            ++sinceIceTicks;
        }

        if (onSolidGround) {
            ++solidGroundTicks;
        } else {
            solidGroundTicks = 0;
        }

        if (data.getPlayer().isFlying()) {
            flying = true;
            sinceFlyingTicks = 0;
        } else {
            ++sinceFlyingTicks;
            flying = false;
        }

        if (onSlime) {
            sinceSlimeTicks = 0;
        } else {
            ++sinceSlimeTicks;
        }

        if (blockNearHead) {
            sinceBlockNearHeadTicks = 0;
        } else {
            ++sinceBlockNearHeadTicks;
        }

        if (inLiquid) {
            ++liquidTicks;
            sinceLiquidTicks = 0;
        } else {
            liquidTicks = 0;
            ++sinceLiquidTicks;
        }

        if (onClimbable) {
            ++climbableTicks;
            sinceClimbableTicks = 0;
        } else {
            climbableTicks = 0;
            ++sinceClimbableTicks;
        }

        if (inWeb) {
            ++webTicks;
            sinceWebTicks = 0;
        } else {
            webTicks = 0;
            ++sinceWebTicks;
        }
    }

    public void handleCollisions(int type) {
        blocks.clear();
        blocksNear.clear();

        BoundingBox boundingBox = new BoundingBox(data);

        switch (type) {
            case 0:
                boundingBox.expandSpecific(0, 0, 0.55, 0.6, 0, 0);
                break;
            case 1:
                boundingBox.expandSpecific(0.1, 0.1, 0.55, 0.6, 0.1, 0.1);
                break;
        }

        final double minX = boundingBox.getMinX();
        final double minY = boundingBox.getMinY();
        final double minZ = boundingBox.getMinZ();
        final double maxX = boundingBox.getMaxX();
        final double maxY = boundingBox.getMaxY();
        final double maxZ = boundingBox.getMaxZ();

        for (double x = minX; x <= maxX; x += (maxX - minX)) {
            for (double y = minY; y <= maxY + 0.01; y += (maxY - minY) / 4) { //Expand max by 0.01 to compensate shortly for precision issues due to FP.
                for (double z = minZ; z <= maxZ; z += (maxZ - minZ)) {
                    final Location location = new Location(data.getPlayer().getWorld(), x, y, z);
                    final Block block = this.getBlock(location);

                    if (block != null) {
                        switch (type) {
                            case 0:
                                blocks.add(block);
                                break;
                            case 1:
                                blocksNear.add(block);
                                break;
                        }
                    }
                }
            }
        }

        switch (type) {
            case 0:
                handleClimbableCollision();
                handleNearbyEntities();

                inLiquid = blocks.stream().anyMatch(Block::isLiquid);
                fullySubmergedInLiquidStat = blocks.stream().allMatch(block -> block.getType() == Material.STATIONARY_WATER || block.getType() == Material.STATIONARY_LAVA);
                inWater = blocks.stream().anyMatch(block -> block.getType().toString().contains("WATER"));
                inLava = blocks.stream().anyMatch(block -> block.getType().toString().contains("LAVA"));
                inWeb = blocks.stream().anyMatch(block -> block.getType().toString().contains("WEB"));
                inAir = blocks.stream().allMatch(block -> block.getType() == Material.AIR);
                onIce = blocks.stream().anyMatch(block -> block.getType().toString().contains("ICE"));
                onSolidGround = blocks.stream().anyMatch(block -> block.getType().isSolid());
                nearStair = blocks.stream().anyMatch(block -> block.getType().toString().contains("STAIR"));
                blockNearHead = blocks.stream().filter(block -> block.getLocation().getY() - data.getPositionProcessor().getY() >= 1.0).anyMatch(block -> block.getType() != Material.AIR);
                nearWall = blocks.stream().filter(block -> block.getLocation().getY() - data.getPositionProcessor().getY() >= 1.0).anyMatch(block -> block.getType() != Material.AIR);
                blocksAbove = blocks.stream().filter(block -> block.getLocation().getY() - data.getPositionProcessor().getY() >= 1.0).collect(Collectors.toList());
                blocksBelow = blocks.stream().filter(block -> block.getLocation().getY() - data.getPositionProcessor().getY() < 0.0).collect(Collectors.toList());
                onSlime = blocks.stream().anyMatch(block -> block.getType().toString().equalsIgnoreCase("SLIME_BLOCK"));
                nearPiston = blocks.stream().anyMatch(block -> block.getType().toString().contains("PISTON"));

                break;
            case 1:
                nearWall = blocksNear.stream().anyMatch(block -> block.getType().isSolid());
                break;
        }

    }

    public void handleClimbableCollision() {
        final int var1 = NumberConversions.floor(this.x);
        final int var2 = NumberConversions.floor(this.y);
        final int var3 = NumberConversions.floor(this.z);

        final Block var4 = this.getBlock(new Location(data.getPlayer().getWorld(), var1, var2, var3));

        if (var4 != null) {
            this.onClimbable = var4.getType() == Material.LADDER || var4.getType() == Material.VINE;
        }
    }

    public void handleNearbyEntities() {
        try {
            nearbyEntities = PlayerUtil.getEntitiesWithinRadius(data.getPlayer().getLocation(), 2);

            if (nearbyEntities == null) {
                nearVehicle = false;
                return;
            }

            nearVehicle = nearbyEntities.stream().anyMatch(entity -> entity instanceof Vehicle);
        } catch (final Throwable t) {
            // I know stfu
        }
    }

    public void handleTeleport(final WrappedPacketOutPosition wrapper) {
        final Vector requestedLocation = new Vector(
                wrapper.getPosition().getX(),
                wrapper.getPosition().getY(),
                wrapper.getPosition().getZ()
        );

        teleportList.add(requestedLocation);
    }

    public void handleClientCommand(final WrappedPacketInClientCommand wrapper) {
        if (wrapper.getClientCommand() == WrappedPacketInClientCommand.ClientCommand.PERFORM_RESPAWN) {
            //handleTeleport();
        }
    }

    public boolean isColliding(final CollisionType collisionType, final Material blockType) {
        if (collisionType == CollisionType.ALL) {
            return blocks.stream().allMatch(block -> block.getType() == blockType);
        }
        return blocks.stream().anyMatch(block -> block.getType() == blockType);
    }

    public Block getBlock(final Location location) {
        if (location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return location.getWorld().getBlockAt(location);
        } else {
            return null;
        }
    }

    public enum CollisionType {
        ANY, ALL
    }
}
