package com.qwarwin.militarytech.registry;

import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.entity.AirDefenseTurretEntity;
import com.qwarwin.militarytech.entity.AircraftEntity;
import com.qwarwin.militarytech.entity.CameraEntity;
import com.qwarwin.militarytech.entity.GradEntity;
import com.qwarwin.militarytech.entity.OreshnikEntity;
import com.qwarwin.militarytech.entity.ProjectileEntity;
import com.qwarwin.militarytech.entity.Specs;
import com.qwarwin.militarytech.entity.TankEntity;
import com.qwarwin.militarytech.entity.TosEntity;
import com.qwarwin.militarytech.entity.VehicleEntity;
import com.qwarwin.militarytech.entity.VehicleSpec;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, MilitaryTech.MODID);

    private static final Map<String, RegistryObject<? extends EntityType<? extends VehicleEntity>>> BY_SPEC = new HashMap<>();

    public static final RegistryObject<EntityType<TankEntity>> T72B3 = tank(Specs.T72B3);
    public static final RegistryObject<EntityType<TankEntity>> T80U = tank(Specs.T80U);
    public static final RegistryObject<EntityType<TankEntity>> T90M = tank(Specs.T90M);
    public static final RegistryObject<EntityType<TankEntity>> BMPT = tank(Specs.BMPT);
    public static final RegistryObject<EntityType<TankEntity>> BMP1 = tank(Specs.BMP1);

    public static final RegistryObject<EntityType<TosEntity>> TOS1A = reg(Specs.TOS1A, ENTITIES.register("tos1a",
            () -> EntityType.Builder.<TosEntity>of(TosEntity::new, EntityClassification.MISC)
                    .sized(Specs.TOS1A.width, Specs.TOS1A.height).clientTrackingRange(16).updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false).build("tos1a")));

    public static final RegistryObject<EntityType<AirDefenseTurretEntity>> ZAK = turret(Specs.ZAK);
    public static final RegistryObject<EntityType<AirDefenseTurretEntity>> ZRK = turret(Specs.ZRK);

    public static final RegistryObject<EntityType<GradEntity>> GRAD = reg(Specs.GRAD, ENTITIES.register("grad",
            () -> EntityType.Builder.<GradEntity>of(GradEntity::new, EntityClassification.MISC)
                    .sized(Specs.GRAD.width, Specs.GRAD.height).clientTrackingRange(16).updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false).build("grad")));

    public static final RegistryObject<EntityType<OreshnikEntity>> ORESHNIK = reg(Specs.ORESHNIK, ENTITIES.register("oreshnik",
            () -> EntityType.Builder.<OreshnikEntity>of(OreshnikEntity::new, EntityClassification.MISC)
                    .sized(Specs.ORESHNIK.width, Specs.ORESHNIK.height).clientTrackingRange(16).updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false).build("oreshnik")));

    public static final RegistryObject<EntityType<AircraftEntity>> SU25 = aircraft(Specs.SU25);
    public static final RegistryObject<EntityType<AircraftEntity>> SU34 = aircraft(Specs.SU34);
    public static final RegistryObject<EntityType<AircraftEntity>> MIG23 = aircraft(Specs.MIG23);
    public static final RegistryObject<EntityType<AircraftEntity>> TU160 = aircraft(Specs.TU160);

    public static final RegistryObject<EntityType<ProjectileEntity>> PROJECTILE = ENTITIES.register("projectile",
            () -> EntityType.Builder.<ProjectileEntity>of(ProjectileEntity::new, EntityClassification.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(16).updateInterval(1).setShouldReceiveVelocityUpdates(true)
                    .build("projectile"));

    /** Client-only stand-in for the camera when looking at a vehicle from outside. Never spawned on a server. */
    public static final RegistryObject<EntityType<CameraEntity>> CAMERA = ENTITIES.register("camera",
            () -> EntityType.Builder.<CameraEntity>of(CameraEntity::new, EntityClassification.MISC)
                    .sized(0.1F, 0.1F).noSave().noSummon().clientTrackingRange(0).build("camera"));

    private static RegistryObject<EntityType<TankEntity>> tank(VehicleSpec s) {
        return reg(s, ENTITIES.register(s.id, () -> EntityType.Builder.<TankEntity>of((t, w) -> new TankEntity(t, w, s),
                EntityClassification.MISC).sized(s.width, s.height).clientTrackingRange(16).updateInterval(1)
                .setShouldReceiveVelocityUpdates(false).build(s.id)));
    }

    private static RegistryObject<EntityType<AirDefenseTurretEntity>> turret(VehicleSpec s) {
        return reg(s, ENTITIES.register(s.id, () -> EntityType.Builder.<AirDefenseTurretEntity>of(
                (t, w) -> new AirDefenseTurretEntity(t, w, s), EntityClassification.MISC).sized(s.width, s.height)
                .clientTrackingRange(16).updateInterval(1).setShouldReceiveVelocityUpdates(false).build(s.id)));
    }

    private static RegistryObject<EntityType<AircraftEntity>> aircraft(VehicleSpec s) {
        return reg(s, ENTITIES.register(s.id, () -> EntityType.Builder.<AircraftEntity>of((t, w) -> new AircraftEntity(t, w, s),
                EntityClassification.MISC).sized(s.width, s.height).clientTrackingRange(24).updateInterval(1)
                .setShouldReceiveVelocityUpdates(false).build(s.id)));
    }

    private static <T extends VehicleEntity> RegistryObject<EntityType<T>> reg(VehicleSpec s, RegistryObject<EntityType<T>> r) {
        BY_SPEC.put(s.id, r);
        return r;
    }

    public static EntityType<? extends VehicleEntity> forSpec(VehicleSpec s) {
        return BY_SPEC.get(s.id).get();
    }

    private ModEntities() {
    }
}
