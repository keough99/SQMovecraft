package net.countercraft.movecraft.slip;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.LocationUtils;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class WarpStartTask extends BukkitRunnable{
	
	int iteration;
	Craft c;
	Player p;
	Sign s;
	
	public WarpStartTask(Craft c, Player p, Sign s){
		if(c == null){
			p.sendMessage("You aren't flying a ship.");
			return;
		}
		if(!p.getWorld().getName().equals(LocationUtils.getSystem())){
			p.sendMessage("Activating slipdrive while not in a vacuum will blow your starship to subatomic particles.");
			return;
		}
		boolean success = WarpUtils.takeFuel(s, 1);
		if(!success){ p.sendMessage("No catalyst found."); return;}
		this.c = c;
		this.p = p;
		this.s = s;
		iteration = 6;
		this.runTaskTimer(Movecraft.getInstance(), 0, 60);
	}
	
	@Override
	public void run(){
		//s.getWorld().playSound(s.getLocation(), Sound.FIZZ, 1.0F, 1.0F);
		switch(iteration){
		case 0:
			if(p != null){
				if(CraftManager.getInstance().getCraftByPlayer(p) != null){
					
					s.setLine(2, ChatColor.BLUE + "Stable Slip.");
					s.update();
					
					WarpUtils.enterWarp(p, CraftManager.getInstance().getCraftByPlayer(p));
					message(ChatColor.AQUA +"succesfully entered the slip and in stable travel. To navigate, fly just like your normally do.");
				}
			}
			this.cancel();
			return;
		case 1:
			if(p != null) message(ChatColor.AQUA + "Energizing..."); iteration--; return;
		case 2:
			if(p != null) message(ChatColor.AQUA +"Capacitors charged, calibrating energization ray..."); iteration--; return;
		case 3:
			if(p != null) message(ChatColor.AQUA +"Flux field generated. Charging capacitors for energization..."); iteration--; return;
		case 4:
			if(p != null) message(ChatColor.AQUA +"Catalyst liquified and stable, Spinning up flux generator..."); iteration--; return;
		case 5:
			if(p != null) message(ChatColor.AQUA +"Beginning catalyist liquification process..."); iteration--; return;
		case 6:
			if(p != null) message(ChatColor.AQUA +"Initializing slipdrive..."); iteration--;
			s.setLine(2, ChatColor.AQUA + "Starting up...");
			s.update();
			return;
		}
	}
	
	private void message(String msg){
		c.messageShipPlayers(msg);
	}

}
