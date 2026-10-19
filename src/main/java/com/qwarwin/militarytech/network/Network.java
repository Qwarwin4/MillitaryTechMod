package com.qwarwin.militarytech.network;

import com.qwarwin.militarytech.MilitaryTech;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

public final class Network {
    private static final String PROTOCOL = "4";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            MilitaryTech.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static boolean registered;

    public static void init() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;
        CHANNEL.registerMessage(id++, VehicleInputPacket.class, VehicleInputPacket::encode, VehicleInputPacket::decode,
                VehicleInputPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, TargetPacket.class, TargetPacket::encode, TargetPacket::decode,
                TargetPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, SeatPacket.class, SeatPacket::encode, SeatPacket::decode,
                SeatPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, ClickVehiclePacket.class, ClickVehiclePacket::encode, ClickVehiclePacket::decode,
                ClickVehiclePacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, LoadoutPacket.class, LoadoutPacket::encode, LoadoutPacket::decode,
                LoadoutPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, NukeFxPacket.class, NukeFxPacket::encode, NukeFxPacket::decode,
                NukeFxPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private Network() {
    }
}
