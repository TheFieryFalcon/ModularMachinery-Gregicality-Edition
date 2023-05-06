/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery;

import github.kasuminova.mmce.common.concurrent.TaskExecutor;
import github.kasuminova.mmce.common.network.PktPerformanceReport;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.command.CommandGetBluePrint;
import hellfirepvp.modularmachinery.common.command.CommandHand;
import hellfirepvp.modularmachinery.common.command.CommandPerformanceReport;
import hellfirepvp.modularmachinery.common.command.CommandSyntax;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.command.CommandCTReload;
import hellfirepvp.modularmachinery.common.network.*;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ModularMachinery
 * Created by HellFirePvP
 * Date: 26.06.2017 / 20:26
 */
@Mod(modid = ModularMachinery.MODID, name = ModularMachinery.NAME, version = ModularMachinery.VERSION,
        dependencies = "required-after:forge@[14.21.0.2371,);" +
                "required-after:crafttweaker@[4.0.4,);" +
                "after:zenutils@[1.12.8,);" +
//                "after:jei@[4.13.1.222,);" +
                "after:fluxnetworks@[4.1.0,);" +
                "after:tconstruct@[1.12.2-2.13.0.183,)",
        acceptedMinecraftVersions = "[1.12, 1.13)"
)
public class ModularMachinery {

    public static final String MODID = "modularmachinery";
    public static final String NAME = "Modular Machinery: Beyond Edition";
    public static final String VERSION = "1.12.2";
    public static final String CLIENT_PROXY = "hellfirepvp.modularmachinery.client.ClientProxy";
    public static final String COMMON_PROXY = "hellfirepvp.modularmachinery.common.CommonProxy";
    public static final SimpleNetworkWrapper NET_CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
    public static final TaskExecutor EXECUTE_MANAGER = new TaskExecutor();
    public static boolean pluginServerCompatibleMode = false;
    @Mod.Instance(MODID)
    public static ModularMachinery instance;
    public static Logger log;
    @SidedProxy(clientSide = CLIENT_PROXY, serverSide = COMMON_PROXY)
    public static CommonProxy proxy;
    private static boolean devEnvCache = false;

    static {
        FluidRegistry.enableUniversalBucket();
    }

    public static boolean isRunningInDevEnvironment() {
        return devEnvCache;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        event.getModMetadata().version = VERSION;
        log = event.getModLog();
        devEnvCache = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");

        NET_CHANNEL.registerMessage(PktCopyToClipboard.class, PktCopyToClipboard.class, 0, Side.CLIENT);
        NET_CHANNEL.registerMessage(PktSyncSelection.class, PktSyncSelection.class, 1, Side.CLIENT);
        NET_CHANNEL.registerMessage(PktPerformanceReport.class, PktPerformanceReport.class, 2, Side.CLIENT);

        NET_CHANNEL.registerMessage(PktInteractFluidTankGui.class, PktInteractFluidTankGui.class, 100, Side.SERVER);
        NET_CHANNEL.registerMessage(PktSmartInterfaceUpdate.class, PktSmartInterfaceUpdate.class, 101, Side.SERVER);
        NET_CHANNEL.registerMessage(PktParallelControllerUpdate.class, PktParallelControllerUpdate.class, 102, Side.SERVER);

        proxy.loadModData(event.getModConfigurationDirectory());

        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit();
    }

    @Mod.EventHandler
    public void onServerStart(FMLServerStartingEvent event) {
        //Cmd registration
        event.registerServerCommand(new CommandSyntax());
        event.registerServerCommand(new CommandHand());
        event.registerServerCommand(new CommandGetBluePrint());
        event.registerServerCommand(new CommandPerformanceReport());

        if (Mods.ZEN_UTILS.isPresent()) {
            event.registerServerCommand(new CommandCTReload());
        }
    }

}
