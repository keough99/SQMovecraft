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

package net.countercraft.movecraft.listener;

//import net.countercraft.movecraft.Movecraft;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.cryo.CryoSpawn;
import net.countercraft.movecraft.shield.ShieldUtils;
import net.countercraft.movecraft.utils.BlockUtils;
import net.countercraft.movecraft.utils.BoardingRampUtils;
import net.countercraft.movecraft.utils.KillUtils;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.vapor.VaporRunnable;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
//import net.countercraft.movecraft.items.StorageChestItem;
//import net.countercraft.movecraft.localisation.I18nSupport;
//import net.countercraft.movecraft.utils.MovecraftLocation;
//import org.bukkit.Location;
//import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
//import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
//import org.bukkit.event.block.Action;
//import org.bukkit.event.block.BlockBreakEvent;
//import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
//import org.bukkit.event.player.PlayerInteractEvent;
//import org.bukkit.inventory.Inventory;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.scheduler.BukkitRunnable;

public class BlockListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onSignChangeEarly(SignChangeEvent event) {
		if(event.getPlayer().hasPermission("movecraft.colorsigns")) return;
		for (String s : event.getLines()) {
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (c == '�') {
					event.getPlayer().sendMessage("Colored chat signs are disabled.");
					event.setCancelled(true);
					return;
				}
				if (c == '&') {
					if (i + 1 >= s.length())
						continue;
					char next = s.charAt(i + 1);
					if (next != ' ') {
						event.getPlayer().sendMessage("Colored chat signs are disabled.");
						event.setCancelled(true);
						return;
					}
				}
			}
			if (s.contains("�")) {
				event.getPlayer().sendMessage("Colored chat signs are disabled.");
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		for (CraftType t : CraftManager.getInstance().getCraftTypes()) {
			if (event.getLine(0).equalsIgnoreCase(t.getCraftName())) {
				if (event.getLine(1) == null || event.getLine(1).equals("")) {
					String str = event.getPlayer().getName();
					if (str.length() > 15) {
						event.setLine(1, str.substring(0, 15));
					} else {
						event.setLine(1, str);
					}
				}
				return;
			} else if (event.getLine(0).equalsIgnoreCase(t.getAltName())) {
				event.setLine(0, t.getCraftName());
				if (event.getLine(1) == null || event.getLine(1).equals("")) {
					String str = event.getPlayer().getName();
					if (str.length() > 15) {
						event.setLine(1, str.substring(0, 15));
					} else {
						event.setLine(1, str);
					}
				}
				return;
			}
		}
		if (event.getLine(0).equalsIgnoreCase("[shield]")) {
			ShieldUtils.setupShieldSign(event);
		} else if (event.getLine(0).equalsIgnoreCase("[shieldmore]")) {
			ShieldUtils.setupMoreShieldSign(event);
		}
	}

	/*
	 * @EventHandler public void onBlockPlace( final BlockPlaceEvent e ) { if (
	 * e.getBlockAgainst().getTypeId() == 33 && e.getBlockAgainst().getData() ==
	 * ( ( byte ) 6 ) ) { e.setCancelled( true ); } else if (
	 * e.getItemInHand().getItemMeta() != null &&
	 * e.getItemInHand().getItemMeta().getDisplayName() != null &&
	 * e.getItemInHand().getItemMeta().getDisplayName().equalsIgnoreCase(
	 * String.format( I18nSupport.getInternationalisedString(
	 * "Item - Storage Crate name" ) ) ) ) { e.getBlockPlaced().setTypeId( 33 );
	 * Location l = e.getBlockPlaced().getLocation(); MovecraftLocation l1 = new
	 * MovecraftLocation( l.getBlockX(), l.getBlockY(), l.getBlockZ() );
	 * StorageChestItem.createNewInventory( l1, e.getBlockPlaced().getWorld() );
	 * new BukkitRunnable() {
	 * 
	 * @Override public void run() { e.getBlockPlaced().setData( ( byte ) 6 ); }
	 * 
	 * }.runTask( Movecraft.getInstance() ); } }
	 */

	/*
	 * @EventHandler public void onPlayerInteract( PlayerInteractEvent event ) {
	 * 
	 * if ( event.getAction() == Action.RIGHT_CLICK_BLOCK ) { if (
	 * event.getClickedBlock().getTypeId() == 33 &&
	 * event.getClickedBlock().getData() == ( ( byte ) 6 ) ) { Location l =
	 * event.getClickedBlock().getLocation(); MovecraftLocation l1 = new
	 * MovecraftLocation( l.getBlockX(), l.getBlockY(), l.getBlockZ() );
	 * Inventory i = StorageChestItem.getInventoryOfCrateAtLocation( l1,
	 * event.getPlayer().getWorld() );
	 * 
	 * if ( i != null ) { event.getPlayer().openInventory( i ); } } } }
	 */
	// #CRYO
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockBreak(final BlockBreakEvent e) {
		if (e.getBlock().getType() == Material.WALL_SIGN || e.getBlock().getType() == Material.SIGN_POST) {
			Sign s = ((Sign) e.getBlock().getState());
			if (s.getLine(0).equals(CryoSpawn.KEY_LINE)) {
				if (!e.isCancelled() || CryoSpawn.signTrim(e.getPlayer().getName()).equals(s.getLine(1))) {
					e.setCancelled(false);
					CryoSpawn.removePodSpawn(s);
					e.getPlayer().sendMessage("CryoPod spawn removed.");
				}
			}

			else if (!e.isCancelled() && (InteractListener.getCraftTypeFromString(s.getLine(0)) != null || s.getLine(0).equals(ChatColor.RED + "EMP shorted"))) {
				onShipSignBreak(s, e);
			}
		} else if (VaporRunnable.isVaporBlock(e.getBlock())) {
			e.setCancelled(true);
		} else {
			if (!e.isCancelled()) {
				for (Block b : BlockUtils.getEdges(e.getBlock(), false, false)) {
					if (b.getType() == Material.WALL_SIGN) {
						Sign s = ((Sign) b.getState());
						BlockFace face = e.getBlock().getFace(s.getBlock());
						if (face == BoardingRampUtils.getFacingBlockFace(s)) {
							if (s.getLine(0).equals(CryoSpawn.KEY_LINE)) {
								CryoSpawn.removePodSpawn(s);
								e.getPlayer().sendMessage("CryoPod spawn removed.");
							} else if ((InteractListener.getCraftTypeFromString(s.getLine(0)) != null || s.getLine(0).equals(ChatColor.RED + "EMP shorted"))) {
								onShipSignBreak(s, e);
							}
						}
					}
				}
			}
		}
	}

	private void onShipSignBreak(Sign s, BlockBreakEvent e) {
		boolean cancel = KillUtils.onBreakShipSign(s, e.getPlayer());
		System.out.println("Ship sign break cancel = " + cancel);
		if (cancel) {
			if (!e.getPlayer().hasPermission("movecraft.override")) {
				e.getPlayer().sendMessage("You cannot break your own ship sign within 5 minutes of piloting the craft.");
				e.setCancelled(true);
				return;
			} else {
				e.getPlayer().sendMessage("You were allowed to break your ship sign within cooldown because of your override perms.");
			}
		}
		Craft[] crafts = CraftManager.getInstance().getCraftsInWorld(e.getBlock().getWorld());
		Craft cFound = null;
		if (crafts != null) {
			for (Craft c : crafts) {
				if (MathUtils.playerIsWithinBoundingPolygon(c.getHitBox(), c.getMinX(), c.getMinZ(), MathUtils.bukkit2MovecraftLoc(e.getBlock().getLocation()))) {
					cFound = c;
					break;
				}
			}
			if (cFound != null) {
				CraftManager.getInstance().removeCraft(cFound);
			}
		}
		Movecraft.getInstance().getStarshipDatabase().removeStarshipAtLocation(e.getBlock().getLocation());
	}

	@EventHandler
	public void onPistonPush(BlockPistonExtendEvent event) {
		for (Block b : event.getBlocks()) {
			Block[] edges = BlockUtils.getEdges(b, false, false);
			for (Block b2 : edges) {
				if (b2.getType() == Material.WALL_SIGN || b2.getType() == Material.SIGN_POST) {
					Sign bs = (Sign) b2.getState();
					if (InteractListener.getCraftTypeFromString(bs.getLine(0)) != null) {
						org.bukkit.material.Sign s = (org.bukkit.material.Sign) b2.getState().getData();
						Block attachedBlock = b2.getRelative(s.getAttachedFace());
						if (attachedBlock.equals(b)) {
							event.setCancelled(true);
							return;
						}
					}
				}
			}
		}
	}

}
