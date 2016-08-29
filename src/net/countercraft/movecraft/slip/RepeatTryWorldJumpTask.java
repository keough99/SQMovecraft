package net.countercraft.movecraft.slip;

import net.countercraft.movecraft.craft.Craft;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RepeatTryWorldJumpTask extends BukkitRunnable{
	Craft c;
	Player pilot;
	Location locto;
	String targetWorldName;
	boolean repilot;
	boolean searchSlipSigns;
	
	public RepeatTryWorldJumpTask(Craft c, Player pilot, Location locto, boolean repilot, boolean searchSlipSigns){
		this.c = c;
		this.pilot = pilot;
		this.locto = locto;
		this.repilot = repilot;
		this.searchSlipSigns = searchSlipSigns;
	}
	@Override
	public void run(){
		int tries = 0;
		if(tries < 100){
			if (!c.isProcessing()){
				boolean success = false;
				while(!success){
					success = TeleportTask.worldJump(pilot, c, locto, repilot, searchSlipSigns);
					locto = new Location(locto.getWorld(), locto.getX() + 50, locto.getY(), locto.getZ());
				}
				c.setProcessingTeleport(false);
				this.cancel();
			}
			tries++;
		} else {
			this.cancel();
		}
	}
}
