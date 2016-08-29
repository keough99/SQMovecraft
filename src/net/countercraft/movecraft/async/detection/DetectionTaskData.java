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

package net.countercraft.movecraft.async.detection;

import java.util.ArrayList;

import net.countercraft.movecraft.utils.MovecraftLocation;

import org.bukkit.World;
import org.bukkit.block.Sign;

/**
 * @author AJCStriker, Dibujaron
 * Holds relevant data for the DetectionTask
 */
public class DetectionTaskData {
	private World w;
	private boolean failed;
	private String failMessage;
	private MovecraftLocation[] blockList;
	private String playerName;
	private int[][][] hitBox;
	private Integer minX, minZ;
	private Integer[] allowedBlocks, forbiddenBlocks;
	private ArrayList<MovecraftLocation> signLocations;
	private MovecraftLocation startLocation;
	private Sign mainSign;

	public DetectionTaskData( World w, String playerName, Integer[] allowedBlocks, Integer[] forbiddenBlocks, MovecraftLocation startLocation ) {
		this.w = w;
		this.playerName = playerName;
		this.allowedBlocks = allowedBlocks;
		this.forbiddenBlocks = forbiddenBlocks;
		this.startLocation = startLocation;
	}

	public DetectionTaskData() {
	}

	public Integer[] getAllowedBlocks() {
		return allowedBlocks;
	}

	public Integer[] getForbiddenBlocks() {
		return forbiddenBlocks;
	}

	public World getWorld() {
		return w;
	}

	void setWorld( World w ) {
		this.w = w;
	}

	public boolean failed() {
		return failed;
	}

	public String getFailMessage() {
		return failMessage;
	}

	void setFailMessage( String failMessage ) {
		this.failMessage = failMessage;
	}

	public MovecraftLocation[] getBlockList() {
		return blockList;
	}

	void setBlockList( MovecraftLocation[] blockList ) {
		this.blockList = blockList;
	}

	public String getPlayername() {
		return playerName;
	}

	public int[][][] getHitBox() {
		return hitBox;
	}

	void setHitBox( int[][][] hitBox ) {
		this.hitBox = hitBox;
	}

	public Integer getMinX() {
		return minX;
	}

	void setMinX( Integer minX ) {
		this.minX = minX;
	}

	public Integer getMinZ() {
		return minZ;
	}

	void setMinZ( Integer minZ ) {
		this.minZ = minZ;
	}

	void setFailed( boolean failed ) {
		this.failed = failed;
	}

	void setPlayerName( String playerName ) {
		this.playerName = playerName;
	}
	void setSignLocations(ArrayList<MovecraftLocation> list){
		signLocations = list;
	}
	public ArrayList<MovecraftLocation> getSignLocations(){
		return signLocations;
	}

	public MovecraftLocation getStartLocation() {
		return startLocation;
	}
	public void setMainSign(Sign s){
		this.mainSign = s;
	}
	public Sign getMainSign(){
		return mainSign;
	}
}
