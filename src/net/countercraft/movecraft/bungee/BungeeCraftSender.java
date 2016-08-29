package net.countercraft.movecraft.bungee;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.EntityUpdateCommand;
import net.countercraft.movecraft.utils.MapUpdateCommand;
import net.countercraft.movecraft.utils.MapUpdateManager;
import net.countercraft.movecraft.utils.MovecraftLocation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
@Deprecated
public class BungeeCraftSender {

	public static void sendCraft(Player p, String targetserver, String world, int X, int Y, int Z, Craft c) throws IOException {
		if (c.getType().getCanServerJump()) {
			/*try {
				c.playersRidingLock.acquire();*/
				for (int i = 0; i < c.playersRidingShip.size(); i++) {
					Player plr = Movecraft.getPlayer(c.playersRidingShip.get(i));
					if (plr == null || !plr.isOnline()) {
						if (p != null)
							p.sendMessage("One of your passengers is not currently online, repilot to proceed without him!");
						//c.playersRidingLock.release();
						return;
					}
				}
				//c.playersRidingLock.release();
				byte[] craftData = serialize(p, targetserver, world, X, Y, Z, c);
				CraftManager.getInstance().removeCraft(c, false);
				BungeeFileHandler.saveCraftBytes(craftData, p.getName());
				System.out.println("Saved craft.");
				sendCraftSpawnPacket(p, targetserver);
				//c.playersRidingLock.acquire();
				for (int i = 0; i < c.playersRidingShip.size(); i++) {
					UUID s = c.playersRidingShip.get(i);
					Player player = Movecraft.getPlayer(s);
					if (player != null)
						BungeePlayerHandler.connectPlayer(player, targetserver);
				}
				//c.playersRidingLock.release();
			/*} catch (Exception e) {
				e.printStackTrace();
			}*/
			removeCraftBlocks(c);
		}
	}

	private static void sendCraftSpawnPacket(Player p, String targetserver) {
		try {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(b);
			out.writeUTF("Forward"); // So bungeecord knows to forward it
			out.writeUTF(targetserver);
			out.writeUTF("movecraftWarp"); // The channel name to check if this
											// your data

			ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
			DataOutputStream msgout = new DataOutputStream(msgbytes);
			msgout.writeUTF(p.getName()); // You can do anything you want with
											// msgout

			byte[] outmsg = msgbytes.toByteArray();
			out.writeShort(outmsg.length);
			out.write(outmsg);

			p.sendPluginMessage(Movecraft.getInstance(), "BungeeCord", b.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static byte[] serialize(Player p, String targetserver, String world, int X, int Y, int Z, Craft c) throws IOException {
		return serialize(p, targetserver, world, X, Y, Z, c, true);
	}
	public static byte[] serialize(Player p, String targetserver, String world, int X, int Y, int Z, Craft c, boolean wipeInventories) throws IOException {
		ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
		DataOutputStream msgout = new DataOutputStream(msgbytes);

		// write out if it's slipspace
		boolean slip = c.getW().getEnvironment() == Environment.THE_END;
		msgout.writeBoolean(slip);
		// send the location
		msgout.writeUTF(world);
		msgout.writeInt(X);
		msgout.writeInt(Y);
		msgout.writeInt(Z);

		// send the existing location
		Location lctn = p.getLocation();
		msgout.writeUTF(lctn.getWorld().getName());
		msgout.writeInt(lctn.getBlockX());
		msgout.writeInt(lctn.getBlockY());
		msgout.writeInt(lctn.getBlockZ());

		// send the craft type
		msgout.writeUTF(c.getType().getCraftName());

		// send the pilot name
		msgout.writeUTF(p.getName());
		msgout.writeUTF(p.getUniqueId().toString());

		// send the length of the craft's block list array
		msgout.writeInt(c.getBlockList().length);

		// send the craft's block list array
		for (MovecraftLocation l : c.getBlockList()) {
			msgout.writeInt(l.getX());
			msgout.writeInt(l.getY());
			msgout.writeInt(l.getZ());
			Location loc = new Location(c.getW(), l.getX(), l.getY(), l.getZ());
			int id = loc.getBlock().getTypeId();
			int data = loc.getBlock().getData();
			msgout.writeInt(id);
			msgout.writeInt(data);
			// send inventory
			if (loc.getBlock().getState() instanceof InventoryHolder) {
				msgout.writeBoolean(true);
				Inventory i = ((InventoryHolder) loc.getBlock().getState()).getInventory();
				msgout.writeUTF(i.getType().name());
				/*
				 * for (int index = 0; index < i.getSize(); index++){
				 * System.out.println("Scanning inventory slot."); ItemStack s =
				 * i.getItem(index); if(s != null){
				 * System.out.println("Sending itemstack");
				 * msgout.writeUTF("continue"); System.out.println(s.getTypeId()
				 * + "," + s.getDurability() + "," + s.getAmount()); //id data
				 * amount index msgout.writeInt(s.getTypeId());
				 * msgout.writeInt(s.getDurability());
				 * msgout.writeInt(s.getAmount()); msgout.writeInt(index); } }
				 */
				InventoryUtils.writeInventory(msgout, i);
				if(wipeInventories){
					i.clear();
				}
			} else {
				msgout.writeBoolean(false);
			}
			if (id == 63 || id == 68) {
				Sign s = (Sign) loc.getBlock().getState();
				msgout.writeUTF(s.getLine(0) + "");
				msgout.writeUTF(s.getLine(1) + "");
				msgout.writeUTF(s.getLine(2) + "");
				msgout.writeUTF(s.getLine(3) + "");
			}
		}
		msgout.writeInt(c.playersWithBedspawnsOnShip.size());
		for (String s : c.playersWithBedspawnsOnShip) {
			msgout.writeUTF(s);
		}
		/*try {
			c.playersRidingLock.acquire();*/
			msgout.writeInt(c.playersRidingShip.size());
			for (int i = 0; i < c.playersRidingShip.size(); i++) {
				Player plr = Movecraft.getPlayer(c.playersRidingShip.get(i));
				if (plr != null) {
					Location l = plr.getLocation();
					BungeePlayerHandler.writePlayerData(msgout, plr, targetserver, world, l.getBlockX(), l.getBlockY(), l.getBlockZ());
					if(wipeInventories){
						BungeePlayerHandler.wipePlayerInventory(plr);
					}
				}
			}
			/*c.playersRidingLock.release();
		} catch (Exception e) {
			e.printStackTrace();
		}*/

		return msgbytes.toByteArray();
	}

	private static void removeCraftBlocks(final Craft c) {
		//World w = c.getW();
		//byte zero = (byte) 0;
		MovecraftLocation[] blocks = c.getBlockList();
		final MapUpdateCommand[] updates = new MapUpdateCommand[blocks.length];
		for (int i = 0; i < blocks.length; i++) {
			//w.getBlockAt(l.getX(), l.getY(), l.getZ()).setTypeIdAndData(0, zero, false);
			//updateSet.add(new MapUpdateCommand(l, 0, c, false));
			updates[i] = new MapUpdateCommand(blocks[i], 0, c, false);
		}
		Bukkit.getScheduler().scheduleSyncDelayedTask(Movecraft.getInstance(), new Runnable(){
			@Override
			public void run(){
				MapUpdateManager.getInstance().addWorldUpdate(c.getW(), updates, new EntityUpdateCommand[0]);
			}
		});
	}

	public static void debug(String s) {
		Movecraft.getInstance().getServer().broadcastMessage(s);
	}
}
