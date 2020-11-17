/*
 * Copyright (C) 2020 Tecnio
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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

package me.tecnio.antihaxerman.check.impl.autoblock;

import io.github.retrooper.packetevents.packetwrappers.in.useentity.WrappedPacketInUseEntity;
import me.tecnio.antihaxerman.check.Check;
import me.tecnio.antihaxerman.check.CheckInfo;
import me.tecnio.antihaxerman.data.PlayerData;

@CheckInfo(name = "AutoBlock", type = "B")
public final class AutoBlockB extends Check {
    public AutoBlockB(PlayerData data) {
        super(data);
    }

    @Override
    public void onAttack(WrappedPacketInUseEntity wrapper) {
        if (wrapper.getAction() == WrappedPacketInUseEntity.EntityUseAction.ATTACK) {
            final boolean invalid = data.isBlocking() && data.getPlayer().isBlocking();

            if (invalid) {
                flag();
            }
        }
    }
}
