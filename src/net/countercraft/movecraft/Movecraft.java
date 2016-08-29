/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

import net.countercraft.movecraft.async.AsyncManager;
import net.countercraft.movecraft.bedspawns.Bedspawn;
import net.countercraft.movecraft.crafttransfer.utils.transfer.BungeeCraftReceiver;
import net.countercraft.movecraft.crafttransfer.utils.transfer.BungeeCraftSender;
import net.countercraft.movecraft.bungee.BungeeListener;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.crafttransfer.SerializableLocation;
import net.countercraft.movecraft.crafttransfer.database.SQLDatabase;
import net.countercraft.movecraft.crafttransfer.utils.bungee.BungeeHandler;
import net.countercraft.movecraft.crafttransfer.utils.bungee.PlayerHandler;
import net.countercraft.movecraft.cryo.CryoSpawn;
import net.countercraft.movecraft.database.FileDatabase;
import net.countercraft.movecraft.database.StarshipDatabase;
import net.countercraft.movecraft.listener.BlockListener;
import net.countercraft.movecraft.listener.CommandListener;
import net.countercraft.movecraft.listener.EntityListener;
import net.countercraft.movecraft.listener.InteractListener;
import net.countercraft.movecraft.listener.InventoryListener;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.shield.ShieldUtils;
import net.countercraft.movecraft.task.AutopilotRunTask;
import net.countercraft.movecraft.task.CooldownTask;
import net.countercraft.movecraft.utils.HPUtils;
import net.countercraft.movecraft.utils.HangarGateUtils;
import net.countercraft.movecraft.utils.LocationUtils;
import net.countercraft.movecraft.utils.MapUpdateManager;
import net.countercraft.movecraft.utils.ShipNuker;
import net.countercraft.movecraft.utils.ShipSizeUtils;
import net.countercraft.movecraft.vapor.VaporRunnable;

/**
 * @author AJCStriker, Dibujaron
 * Movecraft main class, contains most of the bukkit plugin "stuff" and some plugin-wide utility methods
 */
public class Movecraft extends JavaPlugin {
	
	private static Movecraft instance;
	private Logger logger;
	private boolean shuttingDown;
	private StarshipDatabase database;
	private SQLDatabase sqlDatabase;
	
	public static String[] blockMetadataTransfer = new String[] {"guiblock", "hp"};
	
	@Override
	public void onDisable() {
		// Process the storage crates to disk
		//StorageChestItem.saveToDisk();
		shuttingDown = true;
		VaporRunnable.onDisable();
		HangarGateUtils.onDisable();
		ShieldUtils.activateAllRemaining();
	}
	
	@Override
	public boolean onCommand(final CommandSender sender, Command cmd, String label, final String[] args) {
	if (cmd.getName().equalsIgnoreCase("removehome") && sender instanceof Player){
			if(args.length > 0){
				String player = args[0];
				if(sender.hasPermission("Cryospawn.remove")){
					CryoSpawn.removePodSpawn(player, null);
					sender.sendMessage("Removing pod, be sure you typed the name exactly right!");
					return true;
				}
			}
		} else if(cmd.getName().equalsIgnoreCase("cryocheck")){
			if(sender instanceof Player){
				sender.sendMessage("Fetching cryospawn...");
				Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable(){
					@Override
					public void run(){
						CryoSpawn spawn = CryoSpawn.getSpawnAsync(sender.getName());
						if(spawn == null){
							sender.sendMessage("You don't seem to have a registered spawn. If this is unexpected, please post any details you can remember about its possible removal to GitHub immediately.");
						} else {
							sender.sendMessage("Your spawn is currently set to " + spawn.server + " at " + spawn.x + ", " + spawn.y + ", " + spawn.z + " with active status: " + spawn.isActive);
						}
					}
				});
				return true;
			}
			return false;
				
		} else if(cmd.getName().equalsIgnoreCase("handlers")){

			String name = args[0];
			String base = "org.bukkit.event.";
			String qualname = base + name;
			try {
				Class<Event> cls = (Class<Event>) Class.forName(qualname);
				HandlerList list = (HandlerList) cls.getDeclaredMethod("getHandlerList", null).invoke(null, null);
				RegisteredListener[] rlist = list.getRegisteredListeners();
				for(RegisteredListener l : rlist){
					sender.sendMessage(l.getPlugin() + " listening at " + l.getPriority());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		} else if(cmd.getName().equalsIgnoreCase("serverjump") && sender.getName().equalsIgnoreCase("dibujaron")){
			String serverName = args[0];
			final SerializableLocation destinationLocation = new SerializableLocation(serverName, 0, 200, 0);
			Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
				@Override
				public void run() {
					BungeeCraftSender.sendCraft(destinationLocation, CraftManager.getInstance().getCraftByPlayer((Player) sender));
				}
			});
			return true;
		} else if(cmd.getName().equalsIgnoreCase("loadship")){
			if(sender.hasPermission("movecraft.loadship")){
				if(args.length == 1){
					Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable()  {
						@Override
						public void run() {
							BungeeCraftReceiver.receiveCraft(args[0]);
							sender.sendMessage("loaded ship.");
						}
					});
					return true;
				}
				else if(args.length == 2) {
					Bukkit.getScheduler().runTaskAsynchronously(this,  new Runnable() {
						@Override
						public void run() {
							if(args[1].equalsIgnoreCase("true")) {
								BungeeCraftReceiver.receiveCraft(Bukkit.getPlayer(args[0]));
							}
							else {
								BungeeCraftReceiver.receiveCraft(args[0]);
							}
						}
					});
				}
				else if(args.length == 4){
					final Player p = Bukkit.getPlayer(args[0]);
					if(p == null){
						sender.sendMessage("player not found.");
						return false;
					}
					int x, y, z;
					try{
						x = Integer.parseInt(args[1]);
						y = Integer.parseInt(args[2]);
						z = Integer.parseInt(args[3]);
					} catch (Exception e){
						sender.sendMessage("Incorrect coordinates.");
						return false;
					}
					try{
						BungeeCraftReceiver.receiveCraft(p.getName());
					} catch (Exception e){
						e.printStackTrace();
						p.sendMessage("Failed to load ship: error. Did you have a ship saved?");
					}
					sender.sendMessage("loaded ship.");
					return true;
				}
				sender.sendMessage("incorrect arguments.");
			}
			sender.sendMessage("you don't have permission.");
			return true;
		} else if(cmd.getName().equalsIgnoreCase("saveship")){
			if(!(sender instanceof Player)) return false;
			/*Player p = (Player) sender;
			Craft craft = CraftManager.getInstance().getCraftByPlayer(p);
			if(craft == null){
				p.sendMessage("You must be flying a ship to use this command.");
				return true;
			}
			
			boolean passed = BungeeFileHandler.transferScanForIllegal(craft);
			if(!passed){
				p.sendMessage("your ship contains an illegal block for transfer. Please remove it and try again.");
				return false;
			}
			try {
				byte[] craftData = BungeeCraftSender.serialize(p, "transfer", "transfer", 0, 0, 0, craft, false);
				BungeeFileHandler.saveCraftBytes(craftData, p.getName(), BungeeFileHandler.transferFolder);
				p.sendMessage("Starship saved. On the other side you will be able to load this save. You may overwrite this save at any time.");
				return true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				p.sendMessage("Error saving ship, please report this to the developers!");
			}*/
		} else if(cmd.getName().equalsIgnoreCase("shipclass")){
			if(args.length < 1) return false;
			String player = args[0];
			Player p = Bukkit.getPlayer(player);
			if(p == null){
				sender.sendMessage("This player is not currently online.");
				return true;
			}
			Craft c = CraftManager.getInstance().getCraftByPlayer(p);
			if(c == null){
				sender.sendMessage(p.getName() + " is not currently flying a ship.");
				return true;
			}
			sender.sendMessage(p.getName() + " is currently piloting a craft of type " + c.getType().getCraftName());
			return true;
		}
		else if(cmd.getName().equalsIgnoreCase("nukeship")){
			if(sender.hasPermission("movecraft.loadship")){
				ShipNuker.nuke((Player) sender);
				sender.sendMessage("Ship nuked!");
			}
		} else if(cmd.getName().equalsIgnoreCase("releaseall")){
			if(sender instanceof ConsoleCommandSender || sender.isOp() || sender.hasPermission("movecraft.override")){
				CraftManager.getInstance().releaseAllCrafts();
				sender.sendMessage("All crafts released!");
				return true;
			}
		} else if (cmd.getName().equalsIgnoreCase("shipsize") && sender instanceof Player){
			return ShipSizeUtils.printPlayerShipSize((Player) sender, false);
		} else if (cmd.getName().equalsIgnoreCase("shipsizecolor") && sender instanceof Player){
			return ShipSizeUtils.printPlayerShipSize((Player) sender, true);
		} else if (cmd.getName().equalsIgnoreCase("reloadships") && sender.hasPermission("movecraft.reload")){
			sender.sendMessage("Reinitializing craft types...");
			CraftManager.getInstance().initCraftTypes();
			sender.sendMessage("Craft types reloaded.");
		} else if (cmd.getName().equalsIgnoreCase("mapfailures")){
			sender.sendMessage("Found " + MapUpdateManager.failures + ".");
			return true;
		}
//		else if (cmd.getName().equalsIgnoreCase("redline") && sender instanceof Player) {
//			return RedlineUtils.onRedlineCommand((Player) sender, args);
//		}
		return false;
	}

	@Override
	public void onEnable() {
		// Read in config
		this.saveDefaultConfig();
		Settings.LOCALE = getConfig().getString( "Locale" );
		// if the PilotTool is specified in the config.yml file, use it
		if(getConfig().getInt("PilotTool")!=0) {
			logger.log( Level.INFO, "Recognized PilotTool setting of: "+getConfig().getInt("PilotTool"));
			Settings.PilotTool=getConfig().getInt("PilotTool");
		} else {
			logger.log( Level.INFO, "No PilotTool setting, using default");
		}
		HangarGateUtils.onEnable();
		// if the CompatibilityMode is specified in the config.yml file, use it. Otherwise set to false. - NOT IMPLEMENTED YET - Mark
		Settings.CompatibilityMode=getConfig().getBoolean("CompatibilityMode", false);
		LocationUtils.setUp(getConfig());
		if ( !new File( getDataFolder() + "/localisation/movecraftlang_en.properties" ).exists() ) {
			this.saveResource( "localisation/movecraftlang_en.properties", false );
		}
		I18nSupport.init();
		if ( shuttingDown && Settings.IGNORE_RESET ) {
			logger.log( Level.SEVERE, String.format( I18nSupport.getInternationalisedString( "Startup - Error - Reload error" ) ) );
			logger.log( Level.INFO, String.format( I18nSupport.getInternationalisedString( "Startup - Error - Disable warning for reload" ) ) );
			getPluginLoader().disablePlugin( this );
		} else {


			// Startup procedure
			AsyncManager.getInstance().runTaskTimer( this, 0, 1 );
			MapUpdateManager.getInstance().runTaskTimer( this, 0, 1 );

			CraftManager.getInstance();
			
			PluginManager pm = getServer().getPluginManager();
			pm.registerEvents( new InteractListener(), this );
			pm.registerEvents( new CommandListener(), this );
			pm.registerEvents( new BlockListener(), this );
			pm.registerEvents( new EntityListener(), this );
			pm.registerEvents( new InventoryListener(), this );
			pm.registerEvents( new BungeeHandler(), this );
			pm.registerEvents( new PlayerHandler(), this );
			
			Messenger m = this.getServer().getMessenger();
			m.registerOutgoingPluginChannel(this, "BungeeCord");
			BungeeListener b = new BungeeListener();
		    m.registerIncomingPluginChannel(this, "BungeeCord", b);
		    m.registerIncomingPluginChannel(this, "cryoBounce", b);
		    
			//StorageChestItem.readFromDisk();
			//StorageChestItem.addRecipie();

			//bungee server/minecraft server/plugins/movecraft
			
			new AutopilotRunTask();
			
			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new CooldownTask(), 0, 0);
			
			Bedspawn.setUp();
			CryoSpawn.setUp();
			sqlDatabase = new SQLDatabase();
			//PingUtils.setUp();
			
			database = new FileDatabase();
			
			logger.log( Level.INFO, String.format( I18nSupport.getInternationalisedString( "Startup - Enabled message" ), getDescription().getVersion() ) );
		
			if (new File(this.getDataFolder().getAbsolutePath() + "/hp.yml").exists()) {
				
				HPUtils.setup(YamlConfiguration.loadConfiguration(new File(this.getDataFolder().getAbsolutePath() + "/hp.yml")));
				
			} else {
				
				System.out.println("ERROR: hp.yml not found");
				
			}
			
		}
		if(Bukkit.getServerName().equalsIgnoreCase("CoreSystem")){
			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
				@Override
				public void run(){
				    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "reloadchat");
				}
			}, 6000L,6000L);
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		instance = this;
		logger = getLogger();
	}

	public static Movecraft getInstance() {
		return instance;
	}
	
	public StarshipDatabase getStarshipDatabase(){
		return database;
	}
	
	/*public WorldGuardPlugin getWorldGuard() {
	    Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
	 
	    // WorldGuard may not be loaded
	    if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
	        return null; // Maybe you want throw an exception instead
	    }
	 
	    return (WorldGuardPlugin) plugin;
	}*/
	
	public static String locToString(Location loc) {
		return loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ();
	}
	
	public static boolean signContainsPlayername(Sign sign, String name){
		String[] lines = sign.getLines();
		if(name.length() > 15){
			name = name.substring(0, 15);
		}
		for(int i = 1; i < 4; i++){
			String s = lines[i];
			if(s.length() > 15){
				s = s.substring(0, 15);
			}
			if(name.equals(s)){
				return true;
			}
		}
		return false;
	}
	
	public Bedspawn getDefaultBedspawn(){
		String server = /*getConfig().getString("defaultBedspawnServer")*/ "CoreSystem";
		String world = /*getConfig().getString("defaultBedspawnWorld")*/ "CoreSystem";
		int X = /*getConfig().getInt("defaultBedspawnX")*/ 0;
		int Y = /*getConfig().getInt("defaultBedspawnY")*/ 100;
		int Z = /*getConfig().getInt("defaultBedspawnZ")*/ 0;
		return new Bedspawn(null, server, world, X, Y, Z);
	}
	
	public Location getDefaultSpawnLocation(){
		int X = getConfig().getInt("defaultBedspawnX");
		int Y = getConfig().getInt("defaultBedspawnY");
		int Z = getConfig().getInt("defaultBedspawnZ");
		String world = getConfig().getString("defaultBedspawnWorld");
		return new Location(Bukkit.getServer().getWorld(world), X, Y, Z);
	}
	
	//returns the caller of the method that called this method.
	public static String getMethodCaller(){
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		StackTraceElement e = stackTraceElements[3];
		return e.getClassName() + " by method " + e.getMethodName() + " at line " + e.getLineNumber();
	}
	
	//gets the player with given UUID. Attempts to resolve from cache, if it cannot it gets from bukkit.
	public static Player getPlayer(UUID u){
		return Bukkit.getPlayer(u);
	}
	
	public SQLDatabase getSQLDatabase() {
		return sqlDatabase;
	}
}
