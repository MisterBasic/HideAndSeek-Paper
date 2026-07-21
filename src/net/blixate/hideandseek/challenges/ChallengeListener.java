package net.blixate.hideandseek.challenges;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import io.papermc.paper.event.entity.EntityDyeEvent;
import net.blixate.hideandseek.HideAndSeek;

public class ChallengeListener implements Listener {
	
	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		
		if(HideAndSeek.getGameState() == null) return;
		
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.TOUCH_BEDROCK)) {
			if(e.getPlayer().getLocation().subtract(0, 1, 0).getBlock().getType() == Material.BEDROCK) {
				HideAndSeek.getGameState().markChallengeCompleted();
			}
		}
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.REACH_BUILD_LIMIT)) {
			if(e.getPlayer().getLocation().getBlockY() >= HideAndSeek.getGameState().getWorld().getMaxHeight()) {
				HideAndSeek.getGameState().markChallengeCompleted();
			}
		}
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.FALL_HIGH)) {
			if(HideAndSeek.isPlaying(e.getPlayer())) {
				if(e.getPlayer().getFallDistance() >= 100f) {
					HideAndSeek.getGameState().markChallengeCompleted();
				}
			}
		}
	}
	
	@EventHandler
	public void onPortalCreate(PortalCreateEvent e) {
		if(HideAndSeek.getGameState() == null) return;
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.CREATE_NETHER_PORTAL)) {
			if(e.getReason() == CreateReason.FIRE) {
				HideAndSeek.getGameState().markChallengeCompleted();
			}
		}
	}
	
	@EventHandler
	public void onBreed(EntityBreedEvent e) {
		if(HideAndSeek.getGameState() == null) return;
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.BREED_ANIMALS)) {
			if(HideAndSeek.isPlaying((Player)e.getBreeder())) {
				HideAndSeek.getGameState().markChallengeCompleted();
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		if(HideAndSeek.getGameState() == null) return;
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.MINE_DIAMONDS)) {
			if(HideAndSeek.isPlaying(e.getPlayer()) && e.getBlock().getType() == Material.DIAMOND_ORE || e.getBlock().getType() == Material.DEEPSLATE_DIAMOND_ORE) {
				HideAndSeek.getGameState().markChallengeCompleted();
			}
		}
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if(HideAndSeek.getGameState() == null) return;
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.ACCUMULATE_DAMAGE)) {
			if(e.getEntity() instanceof Player player && HideAndSeek.isPlaying(player)) {
				HideAndSeek.getGameState().damageAccumulated += e.getFinalDamage();
				if(HideAndSeek.getGameState().damageAccumulated >= 20.0) {
					HideAndSeek.getGameState().markChallengeCompleted();
					HideAndSeek.getGameState().damageAccumulated = 0;
				}
			}
		}
	}
	
	@EventHandler
	public void onEntityEffect(EntityPotionEffectEvent e) {
		if(HideAndSeek.getGameState() == null) return;
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.BECOME_POISONED)) {
			if(e.getAction() == Action.ADDED && e.getNewEffect().getType() == PotionEffectType.POISON) {
				HideAndSeek.getGameState().markChallengeCompleted();
			}
		}
	}
	
	@EventHandler
	public void onEntitySpawn(CreatureSpawnEvent e) {
		if(HideAndSeek.getGameState() == null) return;
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.CREATE_GOLEM)) {
			if(e.getSpawnReason() == SpawnReason.BUILD_IRONGOLEM
					|| e.getSpawnReason() == SpawnReason.BUILD_COPPERGOLEM
					|| e.getSpawnReason() == SpawnReason.BUILD_SNOWMAN) {
				HideAndSeek.getGameState().markChallengeCompleted();
			}
		}
	}
	
	@EventHandler
	public void onEntitySpawn(EntitySpawnEvent e) {
		if(HideAndSeek.getGameState() == null) return;
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.LIGHT_TNT)) {
			if(e.getEntityType() == EntityType.TNT) {
				HideAndSeek.getGameState().markChallengeCompleted();
			}
		}
	}
	
	@EventHandler
	public void onCraft(CraftItemEvent e) {
		if(HideAndSeek.getGameState() == null) return;
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.CREATE_CAKE)) {
			if(e.getRecipe().getResult().getType() == Material.CAKE) {
				HideAndSeek.getGameState().markChallengeCompleted();
			}
		}
		
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.CREATE_BANNER)) {
			// this should detect any banner?
			if(e.getRecipe().getResult().getType().name().endsWith("_BANNER")) {
				HideAndSeek.getGameState().markChallengeCompleted();
			}
		}
		
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.DYE)) {
			// Note: Any receipe that requires dye will complete the challenge
			for(ItemStack slot : e.getInventory().getMatrix()) {
				if(slot != null && isDyeItem(slot)) {
					HideAndSeek.getGameState().markChallengeCompleted();
				}
			}
		}
	}
	
	@EventHandler
	public void onDeplete(PlayerItemBreakEvent e) {
		if(HideAndSeek.getGameState() == null) return;
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.DEPLETE_IRON_TOOL)) {
			if(isIronTool(e.getBrokenItem())) {
				HideAndSeek.getGameState().markChallengeCompleted();
			}
		}
	}
	
	@EventHandler
	public void onDyed(EntityDyeEvent e) {
		if(HideAndSeek.getGameState() == null) return;
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.DYE)) {
			HideAndSeek.getGameState().markChallengeCompleted();
		}
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if(HideAndSeek.getGameState() == null) return;
		if(HideAndSeek.getGameState().isChallengeActive(SeekerChallenge.DYE)) {
			if(e.hasBlock() && e.getAction().isRightClick()) {
				Player player = e.getPlayer();
				ItemStack heldItem = player.getInventory().getItemInMainHand();
				Block clickedBlock = e.getClickedBlock();
				if(clickedBlock.getState() instanceof Sign sign) {
					if(isDyeItem(heldItem)) {
						HideAndSeek.getGameState().markChallengeCompleted();
					}
				}
			}
		}
	}
	
	private static boolean isDyeItem(ItemStack item) {
		// I can just cheat and see if it's called Dye
		return item.getType().name().contains("DYE");
	}
	
	private static boolean isIronTool(ItemStack item) {
		
		return item.getType() == Material.IRON_SHOVEL ||
				item.getType() == Material.IRON_AXE ||
				item.getType() == Material.IRON_PICKAXE ||
				item.getType() == Material.IRON_SWORD ||
				item.getType() == Material.IRON_HOE ||
				item.getType() == Material.IRON_SPEAR;
		
	}
	
}
