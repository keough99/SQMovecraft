
package com.dibujaron.cardboardbox.meta;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class CardboardMetaSkull implements CardboardItemMeta {

	private static final long serialVersionUID = 8207182730575059588L;
	private int id;
	private String owner;
	private boolean hasowner;

	public CardboardMetaSkull(ItemStack head) {

		this.id = head.getTypeId();
		SkullMeta meta = (SkullMeta) head.getItemMeta();
		if (meta.hasOwner()) {
			this.hasowner = true;
			this.owner = meta.getOwner();
		} else {
			this.hasowner = false;
		}
	}

	@Override
	public ItemMeta unbox() {

		SkullMeta meta = (SkullMeta) new ItemStack(this.id).getItemMeta();
		if (this.hasowner) {
			meta.setOwner(this.owner);
		}

		return meta;
	}

}