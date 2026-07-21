package net.blixate.hideandseek;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.blixate.hideandseek.challenges.SeekerChallenge;
import net.blixate.hideandseek.state.GameState;
import net.blixate.hideandseek.state.State;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class HideAndSeekEventListener implements Listener {
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		e.joinMessage(Component.text("+ ").append(e.getPlayer().displayName()).color(NamedTextColor.GREEN));
		
		// make sure if they disconnect as a hider we allow them to rejoin properly
		e.getPlayer().setWalkSpeed(0.2f);
		e.getPlayer().setFlySpeed(0.1f);
		e.getPlayer().setInvisible(false);
		e.getPlayer().setInvulnerable(false);
		GameState.removeAttributes(e.getPlayer());
		
		if(HideAndSeek.getGameState() != null) {
			HideAndSeek.getGameState().rejoinPlayer(e.getPlayer());
			
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		e.quitMessage(Component.text("- ").append(e.getPlayer().displayName()).color(NamedTextColor.RED));
		
		if(HideAndSeek.isPlaying(e.getPlayer())) {
			
			if(HideAndSeek.getGameState().getState() == State.HIDING) {
				// fix obscure interaction #5413
				if(HideAndSeek.getGameState().getHider() == e.getPlayer()) {
					e.getPlayer().getInventory().clear();
				}
			}
			
			HideAndSeek.getGameState().disconnectPlayer(e.getPlayer());
		}
	}
	
	@EventHandler
	public void onItemRightClick(PlayerInteractEvent event) {
		if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Player player = event.getPlayer();
			
			ItemStack heldItem = player.getInventory().getItemInMainHand();
			if(heldItem == null) {
				return;
			}
			
			String itemId = heldItem.getPersistentDataContainer().get(HideAndSeek.instance.ITEM_KEY, PersistentDataType.STRING);
			if(itemId == null) {
				return;
			}
			
			if(itemId.equals("hider")) {
				if(HideAndSeek.getGameState() != null) {
					HideAndSeek.getGameState().hideFinishedHiding();
				}
			}
			
			if(itemId.equals("teleport")) {
				if(event.getAction() == Action.RIGHT_CLICK_AIR) {
					event.setUseItemInHand(Result.ALLOW);
				} else {
					event.setUseInteractedBlock(Result.ALLOW);
				}
			}
			
			if(itemId.equals("timebonus")) {
				event.setCancelled(true);
				event.setUseItemInHand(Result.DENY);
			}
			
			if(itemId.equals("cancelquestion")) {
				event.setCancelled(true);
				event.setUseItemInHand(Result.DENY);
				if(!HideAndSeek.getGameState().cancelNextQuestion) {
					HideAndSeek.getGameState().cancelNextQuestion = true;
					player.getInventory().setItemInMainHand(null);
					player.playSound(player, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 2f);
					player.sendMessage(HideAndSeek.PREFIX.append(Component.text("Next question asked will be cancelled!")));
				}
			}
			
			if(itemId.equals("challenge")) {
				if(HideAndSeek.getGameState().isAnyChallengeActive()) {
					player.sendActionBar(Component.text("Only one challenge can be active at once!").color(NamedTextColor.RED));
					return;
				}
				
				String challengeString = heldItem.getPersistentDataContainer().get(HideAndSeek.instance.CHALLENGE_KEY, PersistentDataType.STRING);
				SeekerChallenge challenge = SeekerChallenge.valueOf(challengeString);
				HideAndSeek.getGameState().setActiveChallenge(challenge);
				player.getInventory().setItemInMainHand(null);
			}
		}
	}
	
	@EventHandler
	public void onEnderPearlThrow(ProjectileLaunchEvent e) {
		if(e.getEntity() instanceof EnderPearl pearl) {
			ItemStack item = pearl.getItem();
			String itemId = item.getPersistentDataContainer().get(HideAndSeek.instance.ITEM_KEY, PersistentDataType.STRING);
			if(itemId == null) {
				return;
			}
			if(itemId.equals("teleport")) {
				Player player = (Player) e.getEntity().getShooter();
				
				Bukkit.getScheduler().runTaskLater(HideAndSeek.instance, () -> {
					if(e.getEntity() != null)
						e.getEntity().remove();
				}, 1);
				
				e.setCancelled(false);
				Location targetLocation = getTargetLocation(player, 100);
				
				if(targetLocation != null) {
					
					if(!HideAndSeek.getGameState().getWorld().getWorldBorder().isInside(targetLocation)) {
						e.setCancelled(true);
						player.sendMessage(Component.text("Cannot teleport out of the world!").color(NamedTextColor.RED));
						return;
					}
					
					targetLocation.setYaw(player.getYaw());
					targetLocation.setPitch(player.getPitch());
					player.teleport(targetLocation);
					HideAndSeek.getGameState().teleportCooldowns.put(player, System.currentTimeMillis());
					player.playSound(targetLocation, Sound.ENTITY_PLAYER_TELEPORT, 1, 1);
					player.getWorld().spawnParticle(Particle.PORTAL, targetLocation, 16, 0.1, 1, 0.1, 0.2);
				}
				else {
					e.setCancelled(true);
				}
			}
		}
	}
	
	private Location getTargetLocation(Player player, double range) {
		RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getLocation().getDirection(), range, FluidCollisionMode.SOURCE_ONLY, true);
		if(result != null && result.getHitBlock() != null) {
			Block tpBlock = result.getHitBlock().getRelative(result.getHitBlockFace());
			if(!tpBlock.getType().isSolid())
				return tpBlock.getLocation().add(0.5, 0.5, 0.5);
		}
		return null;
	}
	
	@EventHandler
	public void onEntityRightClick(PlayerInteractAtEntityEvent event) {
		Player clicker = event.getPlayer();
		
		Entity rightClicked = event.getRightClicked();
		
		if(HideAndSeek.isPlaying(clicker)) {
			if(rightClicked.equals(HideAndSeek.getGameState().getHider()) && HideAndSeek.getGameState().getState() == State.SEEKING) {
				HideAndSeek.getGameState().hiderFound(clicker);
			}
		}
	}
	
	@EventHandler
	public void onHungerDeplete(FoodLevelChangeEvent e) {
		// hiders should not lose hunger
		if(HideAndSeek.getGameState() != null) {
			
			if(!e.getEntity().getWorld().equals(HideAndSeek.getGameState().getWorld())) {
				return;
			}
			
			if(!HideAndSeek.isPlaying((Player)e.getEntity())) {
				return;
			}
			
			State state = HideAndSeek.getGameState().getState();
			if(HideAndSeek.instance.getConfig().getBoolean("game settings.prevent hunger on state." + state.name())) {
				e.setFoodLevel(20);
				e.setCancelled(true);
			}
			
			if(e.getEntity().equals(HideAndSeek.getGameState().getHider())) {
				e.setFoodLevel(20);
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onCustomClick(PlayerCustomClickEvent e) {
		if (e.getCommonConnection() instanceof PlayerGameConnection gameConnection) {
            
            // 4. Retrieve the actual player object
            Player player = gameConnection.getPlayer();
            HideAndSeek.getGameState().getQuestionHandler().handleQuestion(e.getIdentifier(), player.getLocation());
            // 5. Do your logic here (e.g., player.sendMessage("Dialog clicked!");)
        }
		
		
	}
	
	@EventHandler
	public void onTarget(EntityTargetEvent e) {
		if(HideAndSeek.getGameState() == null) {
			return;
		}
		
		// Entities cannot target the hider
		if(e.getTarget() == HideAndSeek.getGameState().getHider()) {
			e.setCancelled(true);
			return;
		}
	}
	
	@EventHandler
	public void omDamage(EntityDamageEvent e) {
		if(HideAndSeek.getGameState() == null) {
			return;
		}
		
		State state = HideAndSeek.getGameState().getState();
		if(e.getEntity() instanceof Player player && HideAndSeek.isPlaying(player) &&
				HideAndSeek.instance.getConfig().getBoolean("game settings.prevent damage on state." + state.name())) {
			e.setCancelled(true);
		}
		
		if(state == State.SEEKING) {
			if(e.getEntity() instanceof Player player && HideAndSeek.isPlaying(player)) {
				
				if(player == HideAndSeek.getGameState().getHider()) {
					e.setCancelled(true);
					return;
				}
				
				if(player.getHealth() - e.getFinalDamage() <= 0) {
					// if the damage is fatal
					e.setDamage(0);
					player.setHealth(10);
					player.setFoodLevel(20);
					player.setSaturation(10f);
					
					for(Player p : HideAndSeek.getGameState().getPlayers()) {
						p.sendMessage(HideAndSeek.PREFIX.append(player.name()).append(Component.text(" died.")));
					}
					
					HideAndSeek.getGameState().teleportToSpawn(player);
					HideAndSeek.getGameState().rewardHider();
				}
			}
		}
	}
	
	@EventHandler
	public void onSwapHands(PlayerSwapHandItemsEvent e) {
		if(HideAndSeek.isPlaying(e.getPlayer()) && HideAndSeek.getGameState().getState() == State.SEEKING) {
			if(HideAndSeek.getGameState().getHider() == e.getPlayer()) {
				return;
			}
			if(!HideAndSeek.isPlaying(e.getPlayer())) {
				return;
			}
			e.setCancelled(true);
			if(HideAndSeek.getGameState().isAnyChallengeActive()) {
				e.getPlayer().sendActionBar(Component.text("Questions are unavailable while a challenge is active!").color(NamedTextColor.RED));
				return;
			}
			if(HideAndSeek.getGameState().isQuestioningOnCooldown()) {
				e.getPlayer().sendActionBar(Component.text("Questions are currently on cooldown!").color(NamedTextColor.RED));
				return;
			}
			
			e.getPlayer().showDialog(HideAndSeek.getGameState().getQuestionDialog());
		}
	}
	
	@EventHandler
	public void onDropItem(PlayerDropItemEvent e) {
		if(HideAndSeek.isPlaying(e.getPlayer())) {
			if(HideAndSeek.getGameState().getHider() == e.getPlayer()) {
				e.setCancelled(true);
			} else {
				ItemStack itemDropped = e.getItemDrop().getItemStack();
				String itemId = itemDropped.getPersistentDataContainer().get(HideAndSeek.instance.ITEM_KEY, PersistentDataType.STRING);
				if(itemId != null) {
					e.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	public void onPortal(PlayerPortalEvent e) {
		// disable portal interactions
		if(HideAndSeek.isPlaying(e.getPlayer())) {
			e.setCancelled(true);
		}
	}
	
}
