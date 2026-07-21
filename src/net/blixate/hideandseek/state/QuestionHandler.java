package net.blixate.hideandseek.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

import net.blixate.hideandseek.HideAndSeek;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

public class QuestionHandler {
	
	private GameState gameState;
	
	private static final BlockType[] LEAVES = new BlockType[] {
		BlockType.ACACIA_LEAVES, BlockType.AZALEA_LEAVES,
		BlockType.BIRCH_LEAVES, BlockType.CHERRY_LEAVES,
		BlockType.DARK_OAK_LEAVES, BlockType.FLOWERING_AZALEA_LEAVES,
		BlockType.JUNGLE_LEAVES, BlockType.MANGROVE_LEAVES,
		BlockType.OAK_LEAVES, BlockType.PALE_OAK_LEAVES,
		BlockType.SPRUCE_LEAVES
	};
	
	public QuestionHandler(GameState state) {
		this.gameState = state;
	}
	
	public void handleQuestion(Key key, Location askerLocation) {
		//Location hiderLocation = gameState.getHider().getLocation();
		if(gameState.cancelNextQuestion) {
			for(Player player : gameState.getPlayers()) {
				player.sendMessage(HideAndSeek.PREFIX.append(Component.text("Question has been cancelled by the hider!")));
				player.showTitle(Title.title(Component.text("Question Cancelled!").color(NamedTextColor.RED), Component.text("")));
				player.closeDialog(); // make sure no players have the dialog opened.
				player.playSound(player, Sound.ENTITY_WITHER_SHOOT, 1, 0);
			}
			gameState.markQuestionAsAnswered(key);
			gameState.cancelNextQuestion = false;
			return; // Don't update the map or give a response.
		}
		final boolean answer;
		final int direction;
		switch(key.asString()) {
		case "hideandseek:question.zone_200":
			// player is within our coords
			answer = checkRange(askerLocation, 200);
			gameState.questionResponse(key, Component.text("Hider is " + (answer ? "within" : "not within") + " 200 blocks of " + locationString(askerLocation)));
			updateMap((coord) -> {
				if(answer) {
					// darken all but blocks within the range
					return checkRange(coord.getPixelLocation(), askerLocation, 200);
				}
				else {
					// darken all blocks inside the range
					return !checkRange(coord.getPixelLocation(), askerLocation, 200);
				}
			});
			break;
		case "hideandseek:question.east_west":
			direction = checkDirection(askerLocation, gameState.getHider().getLocation(), Axis.X, true);
			gameState.questionResponse(key, Component.text("Hider is " + (direction == 1 ? "West" : "East") + " of " + locationString(askerLocation)));
			updateMap(coord -> {
				return checkDirection(askerLocation, coord.getPixelLocation(), Axis.X) == direction;
			});
			break;
		case "hideandseek:question.same_block":
			answer = matchBlock(askerLocation, gameState.getHider().getLocation());
			gameState.questionResponse(key, Component.text("Hider is " + (answer ? "standing on" : "not standing on") + " ").append(Component.translatable(askerLocation.clone().toCenterLocation().subtract(0, 1, 0).getBlock().translationKey())));
			break;
		case "hideandseek:question.zone_100":
			answer = checkRange(askerLocation, 100);
			gameState.questionResponse(key, Component.text("Hider is " + (answer ? "within" : "not within") + " 100 blocks of " + locationString(askerLocation)));
			updateMap((coord) -> {
				if(answer) {
					return checkRange(coord.getPixelLocation(), askerLocation, 100);
				}
				else {
					return !checkRange(coord.getPixelLocation(), askerLocation, 100);
				}
			});
			break;
		case "hideandseek:question.north_south":
			direction = checkDirection(askerLocation, gameState.getHider().getLocation(), Axis.Z, true);
			gameState.questionResponse(key, Component.text("Hider is " + (direction == 1 ? "North" : "South") + " of " + locationString(askerLocation)));
			updateMap(coord -> {
				return checkDirection(askerLocation, coord.getPixelLocation(), Axis.Z) == direction;
			});
			break;
		case "hideandseek:question.same_biome":
			Biome biome = askerLocation.getBlock().getBiome();
			
			answer = biome == gameState.getHider().getLocation().getBlock().getBiome();
			gameState.questionResponse(key, Component.text("Hider is " + (answer ? "within" : "not within") + " the ").append(Component.translatable(biome.translationKey())).append(Component.text(" biome.")));
			updateMap(coord -> {
				if(answer) {
					return coord.getPixelLocation().getBlock().getBiome() == biome;
				} else {
					return coord.getPixelLocation().getBlock().getBiome() != biome;
				}
			});
			break;
		case "hideandseek:question.zone_50":
			answer = checkRange(askerLocation, 50);
			gameState.questionResponse(key, Component.text("Hider is " + (answer ? "within" : "not within") + " 50 blocks of " + locationString(askerLocation)));
			updateMap((coord) -> {
				if(answer) {
					return checkRange(coord.getPixelLocation(), askerLocation, 50);
				}
				else {
					return !checkRange(coord.getPixelLocation(), askerLocation, 50);
				}
			});
			break;
		case "hideandseek:question.higher_lower":
			answer = checkDirection(askerLocation, gameState.getHider().getLocation(), Axis.Y, true) == -1;
			gameState.questionResponse(key, Component.text("Hider is " + (answer ? "higher" : "lower") + " than Y level " + askerLocation.getBlockY()));
			break;
		case "hideandseek:question.near_water":
			answer = isNearMaterial(BlockType.WATER);
			gameState.questionResponse(key, Component.text("Hider is " + (answer ? "near" : "not near") + " water."));
			break;
		case "hideandseek:question.near_lava":
			answer = isNearMaterial(BlockType.LAVA);
			gameState.questionResponse(key, Component.text("Hider is " + (answer ? "near" : "not near") + " lava."));
			break;
		case "hideandseek:question.near_snow":
			answer = isNearMaterial(BlockType.SNOW, BlockType.SNOW_BLOCK, BlockType.POWDER_SNOW);
			gameState.questionResponse(key, Component.text("Hider is " + (answer ? "near" : "not near") + " any snow."));
			break;
		case "hideandseek:question.on_leaves":
			BlockType standingOn = gameState.getHider().getLocation().clone().subtract(0, 1, 0).getBlock().getType().asBlockType();
			
			boolean isStandingOnLeaves = false;
			for(BlockType leafBlock : LEAVES) {
				if(standingOn.equals(leafBlock)) {
					isStandingOnLeaves = true;
				}
			}
			answer = isStandingOnLeaves;
			gameState.questionResponse(key, Component.text("Hider is " + (answer ? "standing on" : "not standing on") + " leaves."));
			break;
		case "hideandseek:question.same_animals":
			List<EntityType> potentialEntities = new ArrayList<>();
			double range = 6;
			for(Entity entity : askerLocation.getNearbyEntities(range, range, range)) {
				if(entity instanceof LivingEntity && !(entity instanceof Player)) {
					potentialEntities.add(entity.getType());
				}
			}
			if(!potentialEntities.isEmpty()) {
				@NotNull Collection<Entity> hiderEntities = gameState.getHider().getLocation().getNearbyEntities(range, range, range);
				for(Entity entity : hiderEntities) {
					// Only check for living entities
					if(!(entity instanceof LivingEntity) || entity instanceof Player)
						continue;
					
					if(potentialEntities.contains(entity.getType())) {
						gameState.questionResponse(key, Component.text("Hider is within "+((int)range)+" blocks of a ").append(Component.translatable(entity.getType().translationKey())));
						return;
					}
				}
				gameState.questionResponse(key, Component.text("Hider doesn't have any similar entities nearby."));
				return;
			}
			// If we don't have any entities nearby that we can use,
			// just tell us if there are entities nearby.
			else if(!gameState.getHider().getNearbyEntities(range, range, range).isEmpty()) {
				gameState.questionResponse(key, Component.text("Hider has entities around them."));
				return;
			} else {
				gameState.questionResponse(key, Component.text("Hider doesn't have entities around them."));
				return;
			}
		case "hideandseek:question.in_structure":
			boolean insideStructure = false;
			Collection<GeneratedStructure> structures = gameState.getHider().getChunk().getStructures();
			for(GeneratedStructure structure : structures) {
				HideAndSeek.instance.getLogger().info(structure.getStructure().getStructureType().key().toString());
				if(structure.getBoundingBox().overlaps(gameState.getHider().getBoundingBox())) {
					insideStructure = true;
				}
			};
			
			answer = insideStructure;
			
			gameState.questionResponse(key, Component.text("Hider is " + (answer ? "inside" : "not inside") + " a structure."));
			break;
		case "hideandseek:question.sun_visible":
			answer = gameState.getHider().getLocation().getBlock().getLightFromSky() >= 15;
			gameState.questionResponse(key, Component.text("Hider has " + (answer ? "the sky visible above them" : "the sky hidden from their view") + "."));
			break;
		
		
		case "hideandseek:challenge.place_sand":
			break;
		case "hideandseek:challenge.place_gravel":
			break;
		case "hideandseek:challenge.spawn_cow":
			break;
		case "hideandseek:challenge.spawn_chicken":
			break;
		case "hideandseek:challenge.spawn_pig":
			break;
		case "hideandseek:challenge.use_firework":
			break;
		default:
			// Unknown question ???
			HideAndSeek.instance.getLogger().warning("Unknown question with ID " + key.asString() + ".");
			break;
		}
	}
	
	public boolean isNearMaterial(BlockType... blockType) {
		Location hiderLoc = gameState.getHider().getLocation();
		
		Location blockLocation = hiderLoc.clone();
		Block block;
		BlockType hiderBlockType;
		for(int y = -4; y <= 4; y++) {
			for(int x = -4; x <= 4; x++) {
				for(int z = -4; z <= 4; z++) {
					block = blockLocation.clone().add(x, y, z).getBlock();
					hiderBlockType = block.getType().asBlockType();
					if(blockType.length == 1) {
						if(hiderBlockType == blockType[0]) {
							return true;
						}
					} else {
						for(BlockType type : blockType) {
							if(hiderBlockType == type) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	public boolean checkRange(Location center, int range) {
		Location hiderLoc = gameState.getHider().getLocation();
		return checkRange(hiderLoc, center, range);
	}
	
	public boolean checkRange(Location c1, Location c2, int range) {
		double distanceSquared = NumberConversions.square(c1.x() - c2.x()) + NumberConversions.square(c1.z() - c2.z());
		if(Math.sqrt(distanceSquared) <= range) {
			return true;
		}
		return false;
	}
	
	/** 
	 * Returns 1 if the hider is in the positive direction, or -1 if the hider is in the negative direction.<br>
	 * NORTH = -1, Axis Z<br>
	 * SOUTH = 1, Axis Z<br>
	 * EAST = -1, Axis X<br>
	 * WEST = 1, Axis X<br>
	 * */
	public int checkDirection(Location c1, Location c2, Axis axis) {
		return checkDirection(c1, c2, axis, false);
	}
	
	// Setting "answer" to true guarentees the returned value is never zero.
	public int checkDirection(Location c1, Location c2, Axis axis, boolean answer) {
		double[] loc1 = { c1.getX(), c1.getY(), c1.getZ() };
		double[] loc2 = { c2.getX(), c2.getY(), c2.getZ() };
		
		// Negative, positive, or equal.
		int result = (int) Double.compare(loc1[axis.ordinal()], loc2[axis.ordinal()]);
		// If we're on the exact same coordinate as the hider.
		if(result == 0 && answer) {
			// Just pick a random direction and say the player is in that direction
			result = HideAndSeek.instance.rng.nextBoolean() ? 1 : -1;
		}
		
		return result;
	}
	
	public boolean matchBlock(Location c1, Location c2) {
		return c1.clone().toCenterLocation().subtract(0, 1, 0).getBlock().getType() == c2.clone().toCenterLocation().subtract(0, 1, 0).getBlock().getType();
	}
	
	public boolean matchBiome(Location c1, Location c2) {
		return c1.getBlock().getBiome() == c2.getBlock().getBiome();
	}
	
	private class MapCoordinate {
		protected int mapX;
		protected int mapY;
		protected int realX;
		protected int fakeY;
		protected int realZ;
		
		public Location getPixelLocation() {
			return new Location(gameState.getWorld(), realX, fakeY, realZ);
		}
		
		public String toString() {
			return mapX + "," + mapY + ":" + realX + "," + realZ;
		}
	}
	
	public void updateMap(Predicate<MapCoordinate> highlightBlock) {
		MapCoordinate currentCoordinate = new MapCoordinate();
		Location center = gameState.getCenter();
		for(int y = 0; y < 128; y++) {
			for(int x = 0; x < 128; x++) {
				currentCoordinate.mapX = x;
				currentCoordinate.mapY = y;
				
				// get the real coordinates
				int blocksPerPixel = 4;
				currentCoordinate.realX = center.getBlockX() + (x - 64) * blocksPerPixel;
				currentCoordinate.realZ = center.getBlockZ() + (y - 64) * blocksPerPixel;
				currentCoordinate.fakeY = gameState.getWorld().getHighestBlockYAt(currentCoordinate.realX, currentCoordinate.realZ);
				if(!highlightBlock.test(currentCoordinate)) {
					gameState.getMap().overlay.darkenCoordinate(x, y);
				}
			}
		}
	}
	
	private String locationString(Location location) {
		StringBuilder sb = new StringBuilder();
		sb.append("x: " + location.getBlockX() + ", ");
		sb.append("y: " + location.getBlockY() + ", ");
		sb.append("z: " + location.getBlockZ());
		return sb.toString();
	}
}
