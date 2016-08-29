
package com.dibujaron.cardboardbox.meta;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.dibujaron.cardboardbox.utils.CardboardFireworkEffect;

public class CardboardMetaFireworkEffect implements CardboardItemMeta {

	private static final long serialVersionUID = -738623056624942344L;
	private int id;
	private CardboardFireworkEffect effect;

	public CardboardMetaFireworkEffect(ItemStack firework) {

		this.id = firework.getTypeId();
		FireworkEffectMeta meta = (FireworkEffectMeta) firework.getItemMeta();
		this.effect = new CardboardFireworkEffect(meta.getEffect());
	}

	@Override
	public ItemMeta unbox() {

		FireworkEffectMeta meta = (FireworkEffectMeta) new ItemStack(this.id).getItemMeta();
		meta.setEffect(this.effect.unbox());
		return meta;
	}
}
