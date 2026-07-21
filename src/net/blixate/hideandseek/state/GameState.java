package net.blixate.hideandseek.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Lists;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.blixate.hideandseek.HideAndSeek;
import net.blixate.hideandseek.challenges.SeekerChallenge;
import net.blixate.hideandseek.state.map.MapItem;
import net.blixate.hideandseek.utils.TimeParser;
import net.blixate.hideandseek.utils.TimeParser.Timespan;
import net.blixate.hideandseek.utils.WeightedDropTable;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.title.TitlePart;
import net.kyori.adventure.util.Ticks;

public class GameState {
	
	private static final Component RIGHT_CLICK_COMPONENT = Component.text(" (").append(Component.keybind("key.use")).append(Component.text(")")).decoration(TextDecoration.BOLD, false).color(NamedTextColor.GRAY);
	
	private static NamespacedKey waypointHiderKey = new NamespacedKey(HideAndSeek.instance, "hider_waypoint");
	private static AttributeModifier waypointHider = new AttributeModifier(waypointHiderKey, -1.0, Operation.MULTIPLY_SCALAR_1);
	private static NamespacedKey nametagHiderKey = new NamespacedKey(HideAndSeek.instance, "hider_nametag");
	private static AttributeModifier nametagHider = new AttributeModifier(nametagHiderKey, -1.0, Operation.MULTIPLY_SCALAR_1);
	
	private static NamespacedKey jumpHiderKey = new NamespacedKey(HideAndSeek.instance, "hider_jump");
	private static AttributeModifier jumpHider = new AttributeModifier(jumpHiderKey, -1.0, Operation.MULTIPLY_SCALAR_1);
	
	//private static NamespacedKey clickEventTpKey = new NamespacedKey(HideAndSeek.instance, "teleport");
	
	private Player currentHider;
	private List<Player> seekers;
	
	private Scoreboard scoreboard = null;
	private Objective hidingTimes;
	private Team globalTeam;
	// Contains all players actively in the game.
	private Player[] playerList;
	private int nextHiderIndex = 0;
	
	private State state;
	private QuestionHandler questionHandler;
	
	private long hideCooldown = 0;
	private long hidingTimer = 0;
	private long hidingDelta = 0;
	private long intermissionStartTime = 0;
	private boolean timerIsPaused = false;
	public HashMap<Player, Long> teleportCooldowns = new HashMap<>();
	
	public Dialog questionDialog;
	
	private Location center;
	private World world;
	private MapItem map;
	
	// Challenges
	private List<SeekerChallenge> challengePool = new ArrayList<>();
	private SeekerChallenge activeChallenge;
	public double damageAccumulated;
	private BossBar challengeBossBar;
	
	// Settings
	private long intermissionDuration;
	private long intermissionWarningDuration;
	private long teleportReturnCooldown;
	private long hiderDisconnectedRejoinTimer;
	private long questionCooldownDuration;
	private long sameQuestionCooldownDuration;
	
	private long questionCooldown = 0;
	private boolean wasDoubleQuestion = false;
	private List<String> previousQuestions;
	
	// Handling rejoining
	private List<UUID> endedSessions;
	private UUID disconnectedHiderUUID;
	private long hiderDisconnectTime;
	private Location disconnectedHiderLocation;
	
	private final ItemStack[] STARTING_KIT = {
		new ItemStack(Material.STONE_SWORD, 1),
		new ItemStack(Material.STONE_AXE, 1),
		new ItemStack(Material.STONE_PICKAXE, 1),
		new ItemStack(Material.STONE_SHOVEL, 1)
	};

	public boolean cancelNextQuestion = false;
	
	public GameState(World world ) {
		this(new Location(world, HideAndSeek.instance.preferredCenterX, world.getHighestBlockYAt(0, 0), HideAndSeek.instance.preferredCenterZ), world);
	}
	
	public GameState(Location center, World world) {
		currentHider = null;
		endedSessions = new ArrayList<UUID>();
		seekers = new ArrayList<Player>();
		previousQuestions = new ArrayList<>();
		state = State.NOT_STARTED;
		// make the starting kit unbreakable
		for(ItemStack item : STARTING_KIT) {
			ItemMeta meta = item.getItemMeta();
			meta.setUnbreakable(true);
			item.setItemMeta(meta);
		}
		this.world = world;
		this.map = new MapItem(world);
		this.center = center;
		this.questionDialog = buildQuestionDialog();
		
	}
	
	public void setCenter(@NotNull Location centerLocation) {
		this.center = centerLocation;
		this.setupWorldBorder();
		this.map.setCenter(centerLocation);;
	}
	
	public ConfigurationSection gameSettings() {
		return HideAndSeek.instance.getConfig().getConfigurationSection("game settings");
	}
	
	// Requires HideAndSeek.instance.reloadConfig(); to be called before this function.
	public void reloadGameSettings() {
		intermissionDuration = TimeParser.parseTime(gameSettings().getString("intermission duration"));
		intermissionWarningDuration = TimeParser.parseTime(gameSettings().getString("intermission warning"));
		teleportReturnCooldown = TimeParser.parseTime(gameSettings().getString("seeking.teleport item cooldown"));
		hiderDisconnectedRejoinTimer = TimeParser.parseTime(gameSettings().getString("hider disconnect rejoin timer"));
		
		questionCooldownDuration = TimeParser.parseTime(gameSettings().getString("seeking.question cooldown"));
		sameQuestionCooldownDuration = TimeParser.parseTime(gameSettings().getString("seeking.same question cooldown"));
	}
	
	public void tick() {
		// Handle finding a hiding spot
		if(getState() == State.HIDING) {
			getHider().sendActionBar(Component.text("Find a hiding place!").color(NamedTextColor.GREEN));
			getSeekers().forEach((player) -> {
				player.sendActionBar(Component.text("Waiting for hider to hide....").color(NamedTextColor.YELLOW));
			});
			validateHidingSpot();
		}
		
		// Handle pearl returns and the hider's timer
		if(getState() == State.SEEKING) {
			List<Player> returnPearl = null;
			for(Player player : this.teleportCooldowns.keySet()) {
				if(System.currentTimeMillis() - this.teleportCooldowns.get(player) > teleportReturnCooldown) {
					if(returnPearl == null) {
						// don't allocate this if we don't need it
						returnPearl = new ArrayList<>();
					}
					returnPearl.add(player);
				}
			}
			if(returnPearl != null) {
				for(Player player : returnPearl) {
					this.teleportCooldowns.remove(player);
					if(player == currentHider) continue;
					givePearl(player);
				}
			}
			
			if(!timerIsPaused) {
				hidingTimer += System.currentTimeMillis()-hidingDelta;
				hidingDelta = System.currentTimeMillis();
				//updateScore(currentHider, hidingTimer);
				if(currentHider != null) currentHider.sendActionBar(Component.text(TimeParser.toClockTime(hidingTimer)).color(NamedTextColor.LIGHT_PURPLE));
			}
		}
		
		// Handle intermissions
		if(state == State.INTERMISSION) {
			if(System.currentTimeMillis()-intermissionStartTime > intermissionDuration-intermissionWarningDuration) {
				for(Player player : playerList) {
					player.sendActionBar(Component.text("Starting in " + TimeParser.toFancyTime(30000 - (System.currentTimeMillis()-intermissionStartTime))).color(NamedTextColor.YELLOW));
				}
			}
			if(System.currentTimeMillis()-intermissionStartTime > intermissionDuration) {
				selectRandomHider();
				setState(State.HIDING);
				setupHider(currentHider);
				setupSeekers();
				intermissionStartTime = 0;
			}
		}
		
		// Handle hider disconnects
		if(disconnectedHiderUUID != null && endedSessions.contains(disconnectedHiderUUID)) {
			if(System.currentTimeMillis()-hiderDisconnectTime > hiderDisconnectedRejoinTimer) {
				if(state == State.SEEKING) {
					hiderFound(null);
				} else if(state == State.HIDING) {
					setState(State.HIDING);
					selectRandomHider();
					setupHider(currentHider);
					setupSeekers();
					intermissionStartTime = 0;
				}
				
				disconnectedHiderUUID = null;
				
			} else {
				for(Player player : playerList) {
					player.sendActionBar(Component.text("Hider must reconnect in " + TimeParser.toFancyTime(hiderDisconnectedRejoinTimer - (System.currentTimeMillis()-hiderDisconnectTime))).color(NamedTextColor.RED));
				}
			}
		}
	}
	
	public void rejoinPlayer(Player player) {
		// Ensure this player was already in this session before
		if(endedSessions.contains(player.getUniqueId())) {
			// try to reconnect this player
			endedSessions.remove(player.getUniqueId());
			HideAndSeek.instance.getLogger().info("Reconnecting " + player.getName() + " to the current game.");
			addPlayer(player);
			
			if(player.getUniqueId().equals(disconnectedHiderUUID)) {
				player.teleport(disconnectedHiderLocation);
				currentHider = player;
				setupHider(player);
				disconnectedHiderLocation = null;
				disconnectedHiderUUID = null;
				if(state == State.SEEKING) { hidingDelta = System.currentTimeMillis(); timerIsPaused = false; }
			} else {
				if(state == State.INTERMISSION) {
					return;
				}
				if(!seekers.contains(player)) {
					seekers.add(player);
				}
				setupSeeker(player, true);
			}
		}
	}
	
	public void disconnectPlayer(Player player) {
		HideAndSeek.instance.getLogger().info("Player " + player.getName() + " disconnected during the current game instance. Saving state for reconnection.");
		
		endedSessions.add(player.getUniqueId());
		if(state == State.SEEKING || state == State.HIDING) {
			if(player == currentHider) {
				disconnectedHiderLocation = player.getLocation();
				disconnectedHiderUUID = player.getUniqueId();
				hiderDisconnectTime = System.currentTimeMillis();
				timerIsPaused = true;
			} else {
				teleportCooldowns.remove(player);
			}
		}
		removePlayer(player);
	}
	
	public void addPlayer(Player player) {
		Player[] playerListCopy = playerList;
		playerList = new Player[playerList.length + 1];
		int i = 0;
		for(Player p: playerListCopy) {
			playerList[i++] = p;
		}
		playerList[playerList.length-1] = player;
		
		if(challengeBossBar != null)
			challengeBossBar.addViewer(player);
		globalTeam.addPlayer(player);
		player.setScoreboard(scoreboard);
	}
	
	public void removePlayer(Player player) {
		Player[] playerListCopy = playerList;
		playerList = new Player[playerList.length - 1];
		int i = 0;
		for(Player p: playerListCopy) {
			if(p != player) {
				playerList[i++] = p;
			}
		}
		
		if(seekers.contains(player)) {
			seekers.remove(player);
		}
		if(playerList.length == 0) {
			stop(); // end game session
			HideAndSeek.instance.gameState = null;
		}
	}
	
	public void setState(State state) {
		this.state = state;
		this.teleportCooldowns.clear();
		if(gameSettings().getBoolean("prevent hunger on state." + state.name())) {
			
			if(playerList.length == 0) {
				return;
			}
			for(Player p : playerList) {
				p.setFoodLevel(20);
			}
		}
	}
	
	public void start() {
		// Add ALL players on the server to the game.
		refreshPlayerList();
		questionHandler = new QuestionHandler(this);
		
		setState(State.HIDING);
		
		for(Player player : playerList) {
			removeAttributes(player);
			player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
			player.setFoodLevel(20);
			player.setSaturation(1f);
		}
		map.overlay.reset();
		setupWorldBorder();
		setupScoreboard();
		selectRandomHider();
		setupHider(currentHider);
		setupSeekers();
	}
	
	public void stop() {
		if(state == State.NOT_STARTED) {
			return; // game already stopped
		}
		
		setState(State.NOT_STARTED);
		currentHider = null;
		endedSessions.clear();
		this.clearActiveChallenge();
		if(playerList.length != 0) {
			for(Player player : playerList) {
				player.setWalkSpeed(0.2f);
				player.setFlySpeed(0.1f);
				player.setAllowFlight(false);
				player.setInvisible(false);
				player.setGameMode(GameMode.SURVIVAL);
				player.getInventory().clear();
				// shouldn't be needed
				player.getPassengers().forEach((e) -> {
					e.remove();
				});
				player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
				removeAttributes(player);
				Bukkit.getOnlinePlayers().forEach(p -> player.showPlayer(HideAndSeek.instance, p));
			}
		}
	}
	
	private void setupHider(Player hider) {
		hider.setInvisible(gameSettings().getBoolean("hiding.partial invisibility"));
		
		if(hider.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE).getModifier(waypointHiderKey) == null) {
			hider.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE).addTransientModifier(waypointHider);
		}
		if(hider.getAttribute(Attribute.NAME_TAG_DISTANCE).getModifier(nametagHiderKey) == null) {
			hider.getAttribute(Attribute.NAME_TAG_DISTANCE).addTransientModifier(nametagHider);
		}
		
		if(state == State.HIDING) {
			hider.setGameMode(GameMode.ADVENTURE);
			hider.setAllowFlight(true);
			hider.getInventory().clear(); // Only clear inventory in hiding state.
			hider.setInvulnerable(true);
			hider.setWalkSpeed((float)gameSettings().getDouble("hiding.walk speed"));
			hider.setFlySpeed((float)gameSettings().getDouble("hiding.fly speed"));
			removeAttributes(hider);
			
			ItemStack hideItem = new ItemStack(Material.GRAY_DYE, 1);
			ItemMeta meta = hideItem.getItemMeta();
			meta.displayName(Component.text("Set Hiding Place").decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN).append(RIGHT_CLICK_COMPONENT));
			meta.customName(meta.customName().decoration(TextDecoration.ITALIC, false));
			meta.getPersistentDataContainer().set(HideAndSeek.instance.ITEM_KEY, PersistentDataType.STRING, "hider");
			hideItem.setItemMeta(meta);
			hider.getInventory().setItem(0, hideItem);
		} else if(state == State.SEEKING) {
			hider.setAllowFlight(false);
			hider.setWalkSpeed(0);
			hider.setFlySpeed(0.1f);
			if(hider.getAttribute(Attribute.JUMP_STRENGTH).getModifier(jumpHiderKey) == null) {
				hider.getAttribute(Attribute.JUMP_STRENGTH).addTransientModifier(jumpHider);
			}
		}
	}
	
	private void setupSeeker(Player player) {
		setupSeeker(player, false);
	}
	
	public void setupSeeker(Player player, boolean rejoin) {
		if(!rejoin) {
			player.getInventory().clear();
			player.setFoodLevel(20);
		}
		player.setInvisible(false);
		player.setInvulnerable(false);
		player.setWalkSpeed(0.2f);
		player.setGameMode(GameMode.SURVIVAL);
		removeAttributes(player);
		if(state == State.HIDING) {
			player.hidePlayer(HideAndSeek.instance, currentHider);
			player.setAllowFlight(gameSettings().getBoolean("intermission allow flight"));
		} else if(state == State.SEEKING) {
			player.setAllowFlight(false);
			player.showPlayer(HideAndSeek.instance, currentHider);
			if(!rejoin) {
				player.getInventory().addItem(STARTING_KIT);
				player.getInventory().setItem(EquipmentSlot.OFF_HAND, map.getMap());
				givePearl(player);
				teleportToSpawn(player);
			} else {
				if(!player.getInventory().contains(Material.ENDER_PEARL)) {
					// hacky solution to make sure the player doesn't get duplicate pearls on rejoin
					// but guarentees they do receive a pearl.
					// why not
					teleportCooldowns.put(player, System.currentTimeMillis());
				}
			}
			player.sendMessage(Component.text("You are a Seeker! Use all tools available to you to locate the hiding player!").appendNewline()
					.color(NamedTextColor.GREEN)
					.append(Component.text("Press ")
							.append(Component.keybind("key.swapOffhand").color(NamedTextColor.YELLOW))
							.append(Component.text(" to open the Ask Question menu."))));
		}
		
		if(!rejoin)
			player.playSound(player, Sound.BLOCK_BELL_RESONATE, SoundCategory.MASTER, 1, 2);
		
	}
	
	private void givePearl(Player player) {
		player.getInventory().addItem(createTeleportItem());
	}
	
	private ItemStack createTeleportItem() {
		ItemStack teleportItem = new ItemStack(Material.ENDER_PEARL, 1);
		ItemMeta meta = teleportItem.getItemMeta();
		meta.customName(Component.text("Teleport").decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN).append(RIGHT_CLICK_COMPONENT));
		meta.customName(meta.customName().decoration(TextDecoration.ITALIC, false));
		meta.getPersistentDataContainer().set(HideAndSeek.instance.ITEM_KEY, PersistentDataType.STRING, "teleport");
		teleportItem.setItemMeta(meta);
		return teleportItem;
	}
	
	public void giveHiderChallengeReward(SeekerChallenge challenge) {
		if(challenge == null) return;
		
		ItemStack item = new ItemStack(Material.PAPER);
		ItemMeta meta = item.getItemMeta();
		meta.customName(
				Component.text("Challenge: ").decorate(TextDecoration.BOLD).color(NamedTextColor.GOLD)
				.append(Component.text(challenge.getText()).color(NamedTextColor.YELLOW))
				.append(RIGHT_CLICK_COMPONENT)
			);
		meta.customName(meta.customName().decoration(TextDecoration.ITALIC, false));
		meta.getPersistentDataContainer().set(HideAndSeek.instance.ITEM_KEY, PersistentDataType.STRING, "challenge");
		meta.getPersistentDataContainer().set(HideAndSeek.instance.CHALLENGE_KEY, PersistentDataType.STRING, challenge.name());
		meta.setMaxStackSize(1);
		item.setItemMeta(meta);
		currentHider.getInventory().addItem(item);
	}
	
	public void giveHiderCancelQuestion() {
		ItemStack item = new ItemStack(Material.BARRIER);
		ItemMeta meta = item.getItemMeta();
		meta.customName(
				Component.text("Cancel Next Question").decorate(TextDecoration.BOLD).color(NamedTextColor.GOLD)
				.append(RIGHT_CLICK_COMPONENT)
			);
		meta.customName(meta.customName().decoration(TextDecoration.ITALIC, false));
		meta.getPersistentDataContainer().set(HideAndSeek.instance.ITEM_KEY, PersistentDataType.STRING, "cancelquestion");
		meta.setMaxStackSize(1);
		item.setItemMeta(meta);
		currentHider.getInventory().addItem(item);
	}
	
	public void giveHiderTimeBonus(int duration) {
		if(duration <= 0) return;
		
		ItemStack item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
		ItemMeta meta = item.getItemMeta();
		meta.customName(
				Component.text("Time Bonus: ").decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN)
				.append(Component.text(duration + " minutes").color(NamedTextColor.GRAY))
			);
		meta.customName(meta.customName().decoration(TextDecoration.ITALIC, false));
		meta.getPersistentDataContainer().set(HideAndSeek.instance.ITEM_KEY, PersistentDataType.STRING, "timebonus");
		meta.getPersistentDataContainer().set(HideAndSeek.instance.TIME_BONUS_KEY, PersistentDataType.INTEGER, duration);
		meta.setMaxStackSize(1);
		item.setItemMeta(meta);
		currentHider.getInventory().addItem(item);
	}
	
	private SeekerChallenge pickChallenge() {
		// Harder challenges = Rarer
		WeightedDropTable<SeekerChallenge> table = new WeightedDropTable<>(challengePool);
		// set weights
		for(int i = 0; i < table.size(); i++) {
			table.setWeight(i, 4 - table.get(i).getDifficulty());
		}
		
		SeekerChallenge challenge = table.pick();
		
		challengePool.remove(challenge);
		if(challengePool.isEmpty()) {
			// If we ran out of challenges, just restock the challenge pool.
			challengePool = List.of(SeekerChallenge.values());
		}
		return challenge;
	}
	
	public Player selectRandomHider() {
		if(nextHiderIndex + 1 > playerList.length) {
			nextHiderIndex = 0;
		}
		
		return currentHider = playerList[nextHiderIndex++];
	}
	
	public boolean isPlayerInGame(Player player) {
		for(Player p : playerList) {
			if(p == player) return true;
		}
		return false;
	}
	
	private void setupWorldBorder() {
		WorldBorder border = world.getWorldBorder();
		border.setCenter(center);
		border.changeSize(500, 1);
	}
	
	private void setupSeekers() {
		seekers.clear();
		cancelNextQuestion = false;
		map.overlay.reset();
		previousQuestions.clear();
		questionDialog = buildQuestionDialog();
		challengePool = Lists.newArrayList(SeekerChallenge.values());
		clearActiveChallenge();
		for(Player player : playerList) {
			if(player != currentHider) {
				seekers.add(player);
				setupSeeker(player);
			}
		}
	}
	
	private void setupScoreboard() {
		if(scoreboard == null) {
			scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
			hidingTimes = scoreboard.registerNewObjective("hidingTimer", Criteria.DUMMY, Component.text("Hiding Times").color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD), RenderType.INTEGER);
			hidingTimes.setDisplaySlot(DisplaySlot.SIDEBAR);
			hidingTimes.numberFormat(NumberFormat.blank());
			
			hidingTimes.setAutoUpdateDisplay(true);
			globalTeam = scoreboard.registerNewTeam("players");
			globalTeam.setOption(Option.COLLISION_RULE, OptionStatus.NEVER);
			
			Bukkit.getOnlinePlayers().forEach((p) -> {
				globalTeam.addPlayer(p);
				updateScore(p, 0);
				p.setScoreboard(scoreboard);
			});
		}
	}
	
	private void updateScore(Player p, long timeSpentHiding) {
		Component scoreName = p.playerListName();
		
		hidingTimes.getScore(p).setScore((int)(timeSpentHiding & 0x7FFFFFFF) / 10);
		hidingTimes.getScore(p).customName(scoreName);
		String time = "";
		if(timeSpentHiding > TimeParser.hours(1)) {
			time = TimeParser.toClockTime(timeSpentHiding, Timespan.HOUR, Timespan.MINUTE, Timespan.SECOND);
		} else {
			time = TimeParser.toClockTime(timeSpentHiding, Timespan.MINUTE, Timespan.SECOND);
		}
		hidingTimes.getScore(p).numberFormat(NumberFormat.fixed(Component.text(time).color(NamedTextColor.YELLOW)));
	}
	
	private int getScore(Player p) {
		return hidingTimes.getScore(p).getScore() * 10;
	}
	
	public void hideFinishedHiding() {
		// they have to be on the ground
		if(!validateHidingSpot()) {
			return;
		}
		// if it has been less than 5 seconds.
		if(System.currentTimeMillis()-hideCooldown < 5000) {
			return;
		}
		this.hideCooldown = System.currentTimeMillis();
		this.hidingTimer = 0;
		this.hidingDelta = System.currentTimeMillis();
		setState(State.SEEKING);
		for(Player p : seekers) {
			p.showTitle(Title.title(Component.text("Hider is Hidden!").color(NamedTextColor.GREEN), Component.text("Start looking for them!").color(NamedTextColor.GRAY)));
			p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 0.5f, 1.0f);
		}
		currentHider.sendMessage(Component.text("Seekers will now be looking for you!").color(NamedTextColor.GREEN));
		currentHider.getInventory().clear();
		setupHider(currentHider);
		setupSeekers();
	}
	
	public void hiderFound(Player foundBy) {
		this.clearActiveChallenge();
		for(Player p : playerList) {
			p.sendMessage(HideAndSeek.PREFIX
					.append(currentHider.name().color(NamedTextColor.YELLOW))
					.append(Component.text(" was found by ").color(NamedTextColor.GREEN))
					.append(foundBy != null ? foundBy.name().color(NamedTextColor.YELLOW) : Component.text("Nobody").color(NamedTextColor.RED))
					.hoverEvent(HoverEvent.showText(Component.text(locationString(currentHider.getLocation())).decorate(TextDecoration.ITALIC)))
					//.clickEvent(ClickEvent.custom(clickEventTpKey, BinaryTagHolder.binaryTagHolder(locSerialized(currentHider.getLocation())).asBinaryTag()))
					);
			p.showTitle(Title.title(
					Component.text("Hider Found!").color(NamedTextColor.YELLOW),
					Component.text("Found by ").color(NamedTextColor.GRAY)
					.append(foundBy != null ? foundBy.name().color(NamedTextColor.YELLOW) : Component.text("Nobody").color(NamedTextColor.RED))));
			
			if(p != currentHider && foundBy != null && foundBy != p) {
				// teleport all players to the seeker's position when the hider is found.
				p.teleport(foundBy);
			}
			p.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
		}
		
		
		long tb = 0;
		for(ItemStack item : currentHider.getInventory()) {
			if(item == null) continue;
			String itemId = item.getPersistentDataContainer().get(HideAndSeek.instance.ITEM_KEY, PersistentDataType.STRING);
			if(itemId == null) {
				return;
			}
			
			if(itemId.equals("timebonus")) {
				int duration = item.getPersistentDataContainer().get(HideAndSeek.instance.TIME_BONUS_KEY, PersistentDataType.INTEGER);
				
				tb += TimeParser.minutes(duration);
			}
		}
		if(tb > 0) {
			final long timeBonus = tb;
			final Player hider = currentHider;
			Bukkit.getScheduler().runTaskLater(HideAndSeek.instance, () -> {
				updateScore(currentHider, getScore(hider) + timeBonus);
				for(Player p : playerList) {
					p.showTitle(Title.title(
							Component.text("Time Bonus").color(NamedTextColor.GREEN),
							Component.text("+" + TimeParser.toClockTime(timeBonus, Timespan.MINUTE, Timespan.SECOND)).decorate(TextDecoration.BOLD)
						));
					p.playSound(p, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
				}
			}, 60);
		}
		updateScore(currentHider, this.hidingTimer);
		removeAttributes(currentHider);
		currentHider.getInventory().clear();
		currentHider.setInvisible(false);
		currentHider.setInvulnerable(false);
		currentHider.setWalkSpeed(0.2f);
		seekers.clear();
		intermissionStartTime = System.currentTimeMillis();
		setState(State.INTERMISSION);
		if(gameSettings().getBoolean("intermission allow flight")) {
			for(Player p : playerList) {
				p.setAllowFlight(true);
			}
		}
	}
	
	public static void removeAttributes(Player player) {
		if(player.getAttribute(Attribute.NAME_TAG_DISTANCE).getModifier(nametagHiderKey) != null)
			player.getAttribute(Attribute.NAME_TAG_DISTANCE).removeModifier(nametagHiderKey);
		if(player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE).getModifier(waypointHiderKey) != null)
			player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE).removeModifier(waypointHiderKey);
		if(player.getAttribute(Attribute.JUMP_STRENGTH).getModifier(jumpHiderKey) != null)
			player.getAttribute(Attribute.JUMP_STRENGTH).removeModifier(jumpHiderKey);
	}

	public State getState() {
		return state;
	}

	public @NotNull Player getHider() {
		return currentHider;
	}
	
	public @NotNull List<Player> getSeekers() {
		return seekers;
	}

	public void refreshPlayerList() {
		List<Player> players = Lists.newArrayList(Bukkit.getOnlinePlayers());
		// randomize the order
		Collections.shuffle(players, HideAndSeek.instance.rng);
		playerList = players.toArray(new Player[0]);
	}
	
	private String locationString(Location location) {
		StringBuilder sb = new StringBuilder();
		sb.append("x: " + location.getBlockX() + ", ");
		sb.append("y: " + location.getBlockY() + ", ");
		sb.append("z: " + location.getBlockZ());
		return sb.toString();
	}
	
	@SuppressWarnings("deprecation")
	private boolean validateHidingSpot() {
		// They must have light coming from they sky.
		if(currentHider.getLocation().getBlock().getLightFromSky() <= gameSettings().getInt("hiding requirements.minimum sky light")) {
			currentHider.sendActionBar(Component.text("You must be above ground!").color(NamedTextColor.RED));
			return false;
		}
		
		// They must have light coming from they sky.
		if(currentHider.getLocation().getBlockY() < gameSettings().getInt("hiding requirements.minimum y level")) {
			currentHider.sendActionBar(Component.text("You must be above sea level!").color(NamedTextColor.RED));
			return false;
		}
		
		// They cannot be in a liquid
		if(currentHider.getLocation().getBlock().isLiquid() || currentHider.isInPowderedSnow()) {
			currentHider.sendActionBar(Component.text("Your hiding spot must be dry!").color(NamedTextColor.RED));
			return false;
		}
		
		if(currentHider.getLocation().add(0, 1, 0).getBlock().isSolid()) {
			currentHider.sendActionBar(Component.text("Invalid hiding spot.").color(NamedTextColor.RED));
			return false;
		}
		
		// they have to be on the ground
		if(!currentHider.isOnGround()) {
			currentHider.sendActionBar(Component.text("You must be on the ground!").color(NamedTextColor.RED));
			return false;
		}
		
		currentHider.sendActionBar(Component.text("You can hide here!").color(NamedTextColor.GREEN));
		return true;
	}

	public World getWorld() {
		return world;
	}

	public QuestionHandler getQuestionHandler() {
		return questionHandler;
	}

	public MapItem getMap() {
		return map;
	}
	
	public void teleportToSpawn(Player player) {
		player.teleport(center.clone().set(center.getX(), world.getHighestBlockYAt(center.getBlockX(), center.getBlockZ()) + 1, center.getZ()));
	}

	public Player[] getPlayers() {
		return playerList;
	}

	public Location getCenter() {
		return center;
	}
	
	public void questionResponse(Key question, Component response) {
		if(cancelNextQuestion) {
			for(Player player : playerList) {
				player.sendMessage(HideAndSeek.PREFIX.append(Component.text("Question has been cancelled by the hider!")));
				player.showTitle(Title.title(Component.text("Question Cancelled!").color(NamedTextColor.RED), Component.text("")));
				player.closeDialog(); // make sure no players have the dialog opened.
				player.playSound(player, Sound.ENTITY_WITHER_SHOOT, 1, 0);
			}
		} else {
			for(Player player : playerList) {
				player.sendMessage(HideAndSeek.PREFIX.append(response));
				player.showTitle(Title.title(Component.text("Question Asked!").color(NamedTextColor.GREEN), response.color(NamedTextColor.YELLOW)));
				player.closeDialog(); // make sure no players have the dialog opened.
				player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1, 0);
			}
			rewardHider();
		}
		
		markQuestionAsAnswered(question);
		if(wasDoubleQuestion) {
			rewardHider();
		}
	}
	
	public void markQuestionAsAnswered(Key question) {
		wasDoubleQuestion = previousQuestions.contains(question.asString());
		previousQuestions.add(question.asString());
		questionCooldown = System.currentTimeMillis();
		questionDialog = buildQuestionDialog();
	}
	
	public void rewardHider() {
		
		WeightedDropTable<String> rewards = new WeightedDropTable<>();
		
		rewards.addElement("timebonus", 3);
		rewards.addElement("challenge", 2);
		rewards.addElement("cancelquestion", 1);
		
		switch(rewards.pick()) {
		case "timebonus":
			this.giveHiderTimeBonus(HideAndSeek.instance.rng.nextInt(1, 15/3) * 3);
			break;
		case "cancelquestion":
			this.giveHiderCancelQuestion();
			break;
		case "challenge":
			this.giveHiderChallengeReward(pickChallenge());
			break;
		}
	}
	
	public boolean isQuestioningOnCooldown() {
		if(wasDoubleQuestion) {
			return System.currentTimeMillis()-questionCooldown < sameQuestionCooldownDuration;
		}
		return System.currentTimeMillis()-questionCooldown < questionCooldownDuration;
	}
	
	private Dialog buildQuestionDialog() {
		List<ActionButton> buttons = Lists.newArrayList();
		
		ConfigurationSection section = HideAndSeek.instance.getConfig().getConfigurationSection("questions.data");
		for(String key : section.getKeys(false)) {
			String small = section.getString(key + ".short");
			String large = section.getString(key + ".long");
			//boolean seperator = section.contains(key + ".seperator");
			Component questionComponent = Component.text("Ask: ").decorate(TextDecoration.BOLD)
						.color(NamedTextColor.LIGHT_PURPLE).append(Component.text(large).color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, false));
			buttons.add(ActionButton.builder(Component.text(small))
					.action(DialogAction.customClick(Key.key("hideandseek:question." + key), null))
					.width(90)
					.tooltip(previousQuestions.contains("hideandseek:question." + key) ?
							questionComponent
								
								.appendNewline()
								.append(
										Component.text("This question was already asked before!").decorate(TextDecoration.BOLD)
										.appendNewline()
										.append(Component.text("Asking the same question will result in a longer cooldown and double rewards for the hider!").decoration(TextDecoration.BOLD, false))
										.color(NamedTextColor.RED)
									)
							: questionComponent)
					.build());
		}
		return Dialog.create(builder -> {
			builder.empty()
			.base(DialogBase.builder(Component.text("Ask Question")).canCloseWithEscape(true)
					.body(List.of(
							DialogBody.plainMessage(Component.text("Select a question to ask the hider about their whereabouts.").decorate(TextDecoration.BOLD))))
					.build())
			.type(DialogType.multiAction(buttons)
				.columns(HideAndSeek.instance.getConfig().getInt("questions.columns"))
				.exitAction(ActionButton.builder(Component.text("Exit"))
						.width(100).tooltip(Component.text("Left Click to exit.")).build())
				
				.build());
		});
	}
	
	public DialogLike getQuestionDialog() {
		return questionDialog;
	}

	public void setActiveChallenge(SeekerChallenge challenge) {
		if(challenge == null) {
			HideAndSeek.instance.getLogger().warning("Challenge cannot be null. Use markChallengeCompleted() to remove a challenge.");
			return;
		}
		String title = challenge.getText();
		if(title == null) {
			return;
		}
		this.activeChallenge = challenge;
		challengeBossBar = BossBar.bossBar(Component.text("Challenge: ").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD).append(Component.text(title).color(NamedTextColor.YELLOW)),
				1f, Color.YELLOW, Overlay.NOTCHED_10);
		for(Player player : playerList) {
			challengeBossBar.addViewer(player);
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1f, 0f);
			
			player.showTitle(Title.title(Component.text("Challenge Activated!").color(NamedTextColor.GOLD), Component.text(title).color(NamedTextColor.YELLOW), 5, 60, 5));
		}
	}
	
	public boolean isChallengeActive(SeekerChallenge challenge) {
		return activeChallenge == challenge;
	}
	
	public void markChallengeCompleted() {
		if(this.activeChallenge == null) {
			HideAndSeek.instance.getLogger().warning("Called for challenge completion, but no challenge is active!");
		}
		
		for(Player player : playerList) {
			challengeBossBar.removeViewer(player);
			player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.4f, 1f);
			player.sendTitlePart(TitlePart.TIMES, Times.times(Ticks.duration(5), Ticks.duration(20), Ticks.duration(5)));
			player.sendTitlePart(TitlePart.TITLE, Component.text("Challenge Completed!").color(NamedTextColor.GOLD));
		}
		this.activeChallenge = null;
		this.challengeBossBar = null;
	}
	
	public void clearActiveChallenge() {
		if(this.challengeBossBar != null) {
			for(Player player : playerList) {
				challengeBossBar.removeViewer(player);
			}
			this.challengeBossBar = null;
		}
		this.activeChallenge = null;
	}

	public boolean isAnyChallengeActive() {
		return this.activeChallenge != null;
	}

	public void setHider(Player hiderPlayer) {
		currentHider = hiderPlayer;
		setState(State.HIDING);
		setupHider(hiderPlayer);
		setupSeekers();
	}
	
	/** Pauses the hider's timer */
	public void setHiderTimerPaused(boolean b) {
		// dump any passed time to keep the timer accurate.
		// don't do this when we unpause the timer
		if(b) hidingTimer += System.currentTimeMillis()-hidingDelta;
		hidingDelta = System.currentTimeMillis();
		timerIsPaused = b;
	}
}
