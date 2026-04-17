package ashjack.simukraftreloaded.core;

import ashjack.simukraftreloaded.client.Gui.GuiHandler;
import ashjack.simukraftreloaded.commands.CommandChangeCredits;
import ashjack.simukraftreloaded.commands.CommandGenerateFolk;
import ashjack.simukraftreloaded.core.registry.SimukraftReloadedBlocks;
import ashjack.simukraftreloaded.core.registry.SimukraftReloadedConfig;
import ashjack.simukraftreloaded.core.registry.SimukraftReloadedEntities;
import ashjack.simukraftreloaded.core.registry.SimukraftReloadedEvents;
import ashjack.simukraftreloaded.core.registry.SimukraftReloadedGases;
import ashjack.simukraftreloaded.core.registry.SimukraftReloadedItems;
import ashjack.simukraftreloaded.core.registry.SimukraftReloadedRecipes;
import ashjack.simukraftreloaded.core.registry.SimukraftReloadedTabs;
import ashjack.simukraftreloaded.entity.EntityAlignBeam;
import ashjack.simukraftreloaded.entity.EntityConBox;
import ashjack.simukraftreloaded.entity.EntityFolk;
import ashjack.simukraftreloaded.entity.EntityWindmill;
import ashjack.simukraftreloaded.folk.traits.Traits;
import ashjack.simukraftreloaded.packetsNEW.PacketHandler;
import ashjack.simukraftreloaded.proxies.ClientProxy;
import ashjack.simukraftreloaded.proxies.CommonProxy;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;

@Mod(modid = "ashjacksimukraftreloaded", name = "Sim-U-Kraft Reloaded", version = "1.0.2b", dependencies = "required-after:Forge@[9.10,)")


public class ModSimukraft
{
	
    public static final String version = "1.0.3";
    public static final String modid = "ashjacksimukraftreloaded";
    
    @Mod.Instance(modid)
	public static ModSimukraft instance;
    
    @SidedProxy(clientSide = "ashjack.simukraftreloaded.proxies.ClientProxy",
                serverSide = "ashjack.simukraftreloaded.proxies.CommonProxy")
    
    //Proxies
    public static CommonProxy proxy;
    public static ClientProxy clientProxy; 
    
    @EventHandler
    public void preinit(FMLPreInitializationEvent event)
    {
    	
    	PacketHandler.initPackets();
    	
    	NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
    	
        //File check = new File(SimukraftReloaded.getSimukraftFolder());

       /* if (!check.exists())
        {
            System.out.println("Sim-U-Kraft error - Mod not correctly installed, ./minecraft/mods/Simukraft/ folder is missing - copy this file from the zip provided");
        }*/
        
        //Config
        SimukraftReloadedConfig.loadConfigFile(event);
        
        //Creative Tab
        SimukraftReloadedTabs.loadCreativeTabs();
        
        //Blocks
        SimukraftReloadedBlocks.loadBlocks();
        
        //Items
        SimukraftReloadedItems.loadItems();
        
        //Gases
        SimukraftReloadedGases.loadGases();
        
        //Packets
        //packetPipeline.initialize();

        //Entities
        SimukraftReloadedEntities.loadEntities();
        EntityRegistry.registerGlobalEntityID(EntityAlignBeam.class,
                "AlignBeam", EntityRegistry.findGlobalUniqueEntityId());
        EntityRegistry.registerModEntity(EntityAlignBeam.class,"AlignBeam", 0, this, 250, 10, false);
        
        EntityRegistry.registerGlobalEntityID(EntityFolk.class,
                "Folk", EntityRegistry.findGlobalUniqueEntityId());
        EntityRegistry.registerModEntity(EntityFolk.class, "Folk", 1, this, 250, 2, true);
        
        EntityRegistry.registerGlobalEntityID(EntityConBox.class,
                "ConBox", EntityRegistry.findGlobalUniqueEntityId());
        EntityRegistry.registerModEntity(EntityConBox.class, "ConBox", 2, this, 250, 2, true);
        
        EntityRegistry.registerGlobalEntityID(EntityWindmill.class,
                "SUKWindmill", EntityRegistry.findGlobalUniqueEntityId());
        EntityRegistry.registerModEntity(EntityWindmill.class,"SUKWindmill", 3, this, 250, 1, false);
        
        //Traits
        Traits.loadTraits();
        
        //Events
        SimukraftReloadedEvents.loadEvents();
        
        //Recipes
        SimukraftReloadedRecipes.registerRecipes();
        
        proxy.registerRenderInfo();
        proxy.registerMisc();
        
    }

  //Packets
    @EventHandler
    public void postLoad(FMLPostInitializationEvent event)
    {
    //	packetPipeline.postInitialize();
    }
    
    @EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
    	event.registerServerCommand(new CommandChangeCredits());
    	event.registerServerCommand(new CommandGenerateFolk());
    }

}
