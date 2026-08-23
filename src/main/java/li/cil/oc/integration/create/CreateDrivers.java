package li.cil.oc.integration.create;

import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.chassis.StickerBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyBlockEntity;
import com.simibubi.create.content.kinetics.gauge.SpeedGaugeBlockEntity;
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packagePort.postbox.PostboxBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import com.simibubi.create.content.trains.observer.TrackObserverBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import li.cil.oc.api.Driver;

public final class CreateDrivers {
    private CreateDrivers() {
    }

    public static void register() {
        Driver.add(new CreatePackageConverter());

        Driver.add(new CreateBlockDriver<>(CreativeMotorBlockEntity.class, CreateKineticEnvironments.CreativeMotor::new));
        Driver.add(new CreateBlockDriver<>(SpeedControllerBlockEntity.class, CreateKineticEnvironments.SpeedController::new));
        Driver.add(new CreateBlockDriver<>(SpeedGaugeBlockEntity.class, CreateKineticEnvironments.SpeedGauge::new));
        Driver.add(new CreateBlockDriver<>(StressGaugeBlockEntity.class, CreateKineticEnvironments.StressGauge::new));
        Driver.add(new CreateBlockDriver<>(SequencedGearshiftBlockEntity.class, CreateKineticEnvironments.SequencedGearshift::new));

        Driver.add(new CreateBlockDriver<>(ElevatorPulleyBlockEntity.class, CreateContraptionEnvironments.ElevatorPulley::new));
        Driver.add(new CreateBlockDriver<>(MechanicalBearingBlockEntity.class, CreateContraptionEnvironments.MechanicalBearing::new));
        Driver.add(new CreateBlockDriver<>(PulleyBlockEntity.class,
                blockEntity -> !(blockEntity instanceof ElevatorPulleyBlockEntity), CreateContraptionEnvironments.RopePulley::new));
        Driver.add(new CreateBlockDriver<>(HosePulleyBlockEntity.class, CreateContraptionEnvironments.HosePulley::new));
        Driver.add(new CreateBlockDriver<>(MechanicalPistonBlockEntity.class, CreateContraptionEnvironments.MechanicalPiston::new));

        Driver.add(new CreateBlockDriver<>(StickerBlockEntity.class, CreateControlEnvironments.Sticker::new));
        Driver.add(new CreateBlockDriver<>(SignalBlockEntity.class, CreateControlEnvironments.Signal::new));
        Driver.add(new CreateBlockDriver<>(TrackObserverBlockEntity.class, CreateControlEnvironments.TrackObserver::new));

        Driver.add(new CreateBlockDriver<>(DisplayLinkBlockEntity.class, CreateDisplayEnvironments.DisplayLink::new));
        Driver.add(new CreateBlockDriver<>(NixieTubeBlockEntity.class, CreateDisplayEnvironments.NixieTube::new));

        Driver.add(new CreateBlockDriver<>(FrogportBlockEntity.class, CreatePackageEnvironments.Frogport::new));
        Driver.add(new CreateBlockDriver<>(PostboxBlockEntity.class, CreatePackageEnvironments.Postbox::new));
        Driver.add(new CreateBlockDriver<>(RepackagerBlockEntity.class, CreatePackageEnvironments.Repackager::new));
        Driver.add(new CreateBlockDriver<>(PackagerBlockEntity.class,
                blockEntity -> !(blockEntity instanceof RepackagerBlockEntity), CreatePackageEnvironments.Packager::new));

        Driver.add(new CreateBlockDriver<>(RedstoneRequesterBlockEntity.class,
                CreateLogisticsEnvironments.RedstoneRequester::new));
        Driver.add(new CreateBlockDriver<>(StockTickerBlockEntity.class, CreateLogisticsEnvironments.StockTicker::new));
        Driver.add(new CreateBlockDriver<>(TableClothBlockEntity.class, CreateLogisticsEnvironments.TableClothShop::new));
        Driver.add(new CreateBlockDriver<>(StationBlockEntity.class, CreateStationEnvironment::new));
    }
}
