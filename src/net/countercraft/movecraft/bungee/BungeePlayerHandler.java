
package net.countercraft.movecraft.bungee;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.cryo.ActiveCryoHandler;
import net.countercraft.movecraft.utils.MathUtils;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import com.dibujaron.cardboardbox.Knapsack;

/**
 * @author Dibujaron
 * BUNGEECORD: TURN BACK NOW FAINT OF HEART
 * This class is for handing players in a Bungee sense
 */
public class BungeePlayerHandler {

	public static ArrayList<PlayerTeleport> teleportQueue = new ArrayList<PlayerTeleport>();
	static HashMap<UUID, Craft> pilotQueue = new HashMap<UUID, Craft>();
	static Location spawnLocation;
	
	/**
	 * @param p the Player involved
	 * @return if it was succesful
	 */
	public static boolean onLogin(final Player p){
		if(!p.hasPlayedBefore() && Bukkit.getServer().getServerName().equals("Trinitos_Alpha")){
			System.out.println("New player on Regalis!");
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Movecraft.getInstance(), new Runnable(){
				@Override
				public void run(){
					if(spawnLocation == null){
						spawnLocation = Movecraft.getInstance().getDefaultSpawnLocation();
					}
					p.teleport(spawnLocation);
				}
			}, 1L);
			return true;
		}
		final UUID uid = p.getUniqueId();
		for(int i = 0; i < teleportQueue.size(); i++){
			final PlayerTeleport t = teleportQueue.get(i);
			if(uid.equals(t.getUUID())){
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Movecraft.getInstance(), new Runnable(){
					@Override
					public void run(){
						t.execute();
					}
				}, 2L);
				final Craft c = pilotQueue.get(uid);
				if(c != null){
					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Movecraft.getInstance(), new Runnable(){
						@Override
						public void run(){
							if(c.originalPilotLoc == null){
								c.detect(p, MathUtils.bukkit2MovecraftLoc(p.getLocation()));
							} else {
								c.detect(p, MathUtils.bukkit2MovecraftLoc(c.originalPilotLoc));
							}
							pilotQueue.remove(uid);
						}
					}, 3L);
				}
				return true;
			}
		}
		return ActiveCryoHandler.onPlayerLogin(p);
	}
	public static void sendPlayer(Player p, String targetserver, String world, int X, int Y, int Z){
		sendPlayer(p, targetserver, world, X, Y, Z, false);
	}

	public static void sendPlayer(Player p, String targetserver, String world, int X, int Y, int Z, boolean isBedspawn) {

		sendPlayerCoordinateData(p, targetserver, world, X, Y, Z, isBedspawn);
		connectPlayer(p, targetserver);
	}

	public static void connectPlayer(Player p, String targetserver) {

		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);
		try {
			out.writeUTF("Connect");
			out.writeUTF(targetserver);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		p.sendPluginMessage(Movecraft.getInstance(), "BungeeCord", b.toByteArray());
	}
	
	/*public static void sendDeath(Player p, String targetserver) {
		System.out.println("Sending death.");
		sendPlayerDeathData(p, targetserver);
		System.out.println("Sending player.");
		connectPlayer(p, targetserver);
	}*/
	
	private static void sendPlayerDeathData(Player p, String targetserver){
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);
		// send player data of where they should be teleported to
		try {
			out.writeUTF("Forward");
			out.writeUTF(targetserver);
			out.writeUTF("movecraftDeath");

			ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
			DataOutputStream msgout = new DataOutputStream(msgbytes);

			msgout.writeUTF(p.getUniqueId().toString());

			byte[] outbytes = msgbytes.toByteArray();
			out.writeShort(outbytes.length);
			out.write(outbytes);
			p.sendPluginMessage(Movecraft.getInstance(), "BungeeCord", b.toByteArray());
			System.out.println("Death data sent.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void sendPlayerCoordinateData(Player p, String targetserver, String world, int X, int Y, int Z, boolean isBedspawn) {

		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);
		// send player data of where they should be teleported to
		try {
			out.writeUTF("Forward");
			out.writeUTF(targetserver);
			out.writeUTF("movecraftPlayer");

			ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
			DataOutputStream msgout = new DataOutputStream(msgbytes);

			writePlayerData(msgout, p, targetserver, world, X, Y, Z, isBedspawn);

			byte[] outbytes = msgbytes.toByteArray();
			out.writeShort(outbytes.length);
			out.write(outbytes);
			p.sendPluginMessage(Movecraft.getInstance(), "BungeeCord", b.toByteArray());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void writePlayerData(DataOutputStream msgout, Player p, String targetserver, String world, int X, int Y, int Z, boolean isBedspawn) throws IOException {

		Location l = p.getLocation();
		msgout.writeUTF(world);
		msgout.writeUTF(p.getUniqueId().toString());
		msgout.writeInt(X);
		msgout.writeInt(Y);
		msgout.writeInt(Z);
		msgout.writeDouble(l.getYaw());
		msgout.writeDouble(l.getPitch());
		InventoryUtils.writePlayer(msgout, p);
		msgout.writeInt(gameModeToInt(p.getGameMode()));
		msgout.writeBoolean(isBedspawn);
	}
	
	public static void writePlayerData(DataOutputStream msgout, Player p, String targetserver, String world, int X, int Y, int Z) throws IOException{
		writePlayerData(msgout, p, targetserver, world, X, Y, Z, false);
	}

	public static void wipePlayerInventory(Player p) {

		p.getInventory().clear();
		p.getInventory().setArmorContents(null);
		for(PotionEffect effect : p.getActivePotionEffects()) {
			p.removePotionEffect(effect.getType());
		}
	}

	public static void recievePlayer(DataInputStream in) {

		try {
			short len = in.readShort();
			byte[] msgbytes = new byte[len];
			in.readFully(msgbytes);

			DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));

			ServerjumpTeleport t = recievePlayerTeleport(msgin);
			
			if(t != null){
				if (Movecraft.getPlayer(t.uuid) != null) {
					t.execute();
				} else {
					teleportQueue.add(t);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*public static void recievePlayerDeath(DataInputStream in) {
		System.out.println("Recieving player death!");
		try {
			short len = in.readShort();
			byte[] msgbytes = new byte[len];
			in.readFully(msgbytes);

			DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));

			DeathTeleport t = new DeathTeleport(UUID.fromString(msgin.readUTF()));
			System.out.println("Death Teleport constructed.");
			if(t != null){
				if (Movecraft.getPlayer(t.getUUID()) != null) {
					t.execute();
				} else {
					teleportQueue.add(t);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

	public static ServerjumpTeleport recievePlayerTeleport(DataInputStream msgin) throws IOException {
		try{
			String worldname = msgin.readUTF();
			UUID uuid = UUID.fromString(msgin.readUTF());
			int coordX = msgin.readInt();
			int coordY = msgin.readInt();
			int coordZ = msgin.readInt();
			double yaw = msgin.readDouble();
			double pitch = msgin.readDouble();
			Knapsack playerKnap = InventoryUtils.readPlayer(msgin);
			GameMode gamemode = intToGameMode(msgin.readInt());
			boolean isBedspawn = msgin.readBoolean();
			//I should remove this sometime.
	
			if (coordY > 256) {
				Location loc = Bukkit.getServer().getWorld(worldname).getSpawnLocation();
				coordX = loc.getBlockX();
				coordY = loc.getBlockY();
				coordZ = loc.getBlockZ();
			}
			ServerjumpTeleport t = new ServerjumpTeleport(uuid, worldname, coordX, coordY, coordZ, yaw, pitch, playerKnap, gamemode);
			return t;
		} catch (EOFException e){
			System.out.println("EOFException in movecraft: told to recieve player teleport but none found!");
			return null;
		}
	}

	private static int gameModeToInt(GameMode g) {

		switch (g) {
			case SURVIVAL:
				return 0;
			case CREATIVE:
				return 1;
			case ADVENTURE:
				return 2;
			default:
				return 0;
		}
	}

	private static GameMode intToGameMode(int g) {

		switch (g) {
			case 0:
				return GameMode.SURVIVAL;
			case 1:
				return GameMode.CREATIVE;
			case 2:
				return GameMode.ADVENTURE;
			default:
				return GameMode.SURVIVAL;
		}
	}

	public static PlayerTeleport teleportFromUUID(UUID u) {

		for (PlayerTeleport t : teleportQueue) {
			if (t.getUUID().equals(u)) {
				return t;
			}
		}
		return null;
	}

	private static Inventory armorInv(Player p) {

		Inventory armorInv = Bukkit.createInventory(p, 9);
		ItemStack[] armor = p.getInventory().getArmorContents();
		for (int i = 0; i < armor.length; i++) {
			armorInv.setItem(i, armor[i]);
		}
		return armorInv;
	}
}
