package net.blixate.hideandseek.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.blixate.hideandseek.HideAndSeek;
import net.blixate.hideandseek.state.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class HideAndSeekCommand implements CommandExecutor, TabCompleter {
	
	private static final String CREDIT_MESSAGE = "<aqua>HideAndSeek Plugin written by <click:open_url:https://github.com/MisterBasic><u>MisterBasic</u></click><br>" +
			"❤ Inspired by <click:open_url:https://modrinth.com/mod/hide+seek><u>Hide And Seek Mod</u></click> made by <click:open_url:https://www.youtube.com/watch?v=e1bbYM9d_QA>EightSidedSquare</click> ❤</aqua>";
	
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
			@NotNull String @NotNull [] args) {
		if(!(sender instanceof Player)) {
			sender.sendMessage("Command can only be executed by players.");
			return false;
		}
		Player player = (Player)sender;
		if(args.length <= 0) {
			sender.sendMessage(MiniMessage.miniMessage().deserialize(CREDIT_MESSAGE));
			return true;
		}
		
		if(args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("forcestart")) {
			if(!sender.hasPermission("hideandseek.command")) {
				sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
				return true;
			}
			HideAndSeek.instance.gameState = new GameState(((Player)sender).getWorld());
			// load game settings
			HideAndSeek.instance.gameState.reloadGameSettings();
			if(Bukkit.getOnlinePlayers().size() < 2 && !args[0].equalsIgnoreCase("forcestart")) {
				sender.sendMessage(
						Component.text("Too few players to start a match. Use ").color(NamedTextColor.RED)
						.append(Component.text("/hideandseek forcestart")
								.color(NamedTextColor.YELLOW)
								.clickEvent(ClickEvent.suggestCommand("/hideandseek forcestart")))
						.append(Component.text(" to start a game anyway.")));
				return true;
			}
			HideAndSeek.instance.gameState.start();
		}
		
		else if(args[0].equalsIgnoreCase("stop")) {
			if(!sender.hasPermission("hideandseek.command")) {
				sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
				return true;
			}
			if(HideAndSeek.instance.gameState == null) {
				sender.sendMessage(Component.text("Game hasn't been started yet.").color(NamedTextColor.RED));
				return true;
			}
			HideAndSeek.instance.gameState.stop();
			HideAndSeek.instance.gameState = null;
		}
		
		else if(args[0].equalsIgnoreCase("reload")) {
			if(!sender.hasPermission("hideandseek.command")) {
				sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
				return true;
			}
			HideAndSeek.instance.reloadConfig();
			if(HideAndSeek.getGameState() != null) {
				HideAndSeek.getGameState().reloadGameSettings();
			}
		}
		
		else if(args[0].equals("join")) {
			if(HideAndSeek.getGameState() == null) {
				sender.sendMessage(Component.text("No game is running to join.").color(NamedTextColor.RED));
				return true;
			}
			
			
			if(HideAndSeek.isPlaying(player)) {
				sender.sendMessage(Component.text("You are already in this game session!").color(NamedTextColor.RED));
				return true;
			}
			HideAndSeek.getGameState().addPlayer(player);
			HideAndSeek.getGameState().setupSeeker(player, false);
			sender.sendMessage(Component.text("Joined current game session!").color(NamedTextColor.GREEN));
		}
		
		else if(args[0].equals("leave") ) {
			if(HideAndSeek.getGameState() == null) {
				sender.sendMessage(Component.text("No game is running to leave.").color(NamedTextColor.RED));
				return true;
			}
			
			if(!HideAndSeek.isPlaying(player)) {
				sender.sendMessage(Component.text("You are not in a game session!").color(NamedTextColor.RED));
				return true;
			}
			
			if(HideAndSeek.getGameState().getHider() == player) {
				HideAndSeek.getGameState().hiderFound(null);
			}
			
			HideAndSeek.getGameState().teleportCooldowns.remove(player);
			player.setWalkSpeed(0.2f);
			player.setFlySpeed(0.1f);
			player.setAllowFlight(false);
			player.setInvisible(false);
			player.setGameMode(GameMode.SURVIVAL);
			player.getInventory().clear();
			player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
			GameState.removeAttributes(player);
			Bukkit.getOnlinePlayers().forEach(p -> player.showPlayer(HideAndSeek.instance, p));
			HideAndSeek.getGameState().removePlayer(player);
			sender.sendMessage(Component.text("Left current game session!").color(NamedTextColor.RED));
			
			if(HideAndSeek.getGameState().getPlayers().length == 1) {
				// Not enough players to continue.
				// Might cause problems with force-start but force starting is dumb and shouldn't be done.
				HideAndSeek.getGameState().stop();
			}
		}
		
		else if(args[0].equalsIgnoreCase("hider")) {
			
			if(!sender.hasPermission("hideandseek.command")) {
				sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
				return true;
			}
			
			if(HideAndSeek.getGameState() == null) {
				sender.sendMessage(Component.text("No game is running to modify.").color(NamedTextColor.RED));
				return true;
			}
			if(args.length > 1) {
				
				if(args[1].equalsIgnoreCase("set")) {
					if(args.length >= 3) {
						String hider = args[2];
						Player hiderPlayer = Bukkit.getPlayer(hider);
						
						if(hiderPlayer == null) {
							sender.sendMessage(Component.text("Unknown player.").color(NamedTextColor.RED));
							return true;
						}
						HideAndSeek.getGameState().setHider(hiderPlayer);
						sender.sendMessage(Component.text("Set the current hider to " + hiderPlayer.getName()));
					}
				}
				
				if(args[1].equalsIgnoreCase("pause")) {
					HideAndSeek.getGameState().setHiderTimerPaused(true);
					sender.sendMessage(Component.text("The hider's timer has been paused.").color(NamedTextColor.GREEN));
				}
				
				if(args[1].equalsIgnoreCase("unpause")) {
					HideAndSeek.getGameState().setHiderTimerPaused(false);
					sender.sendMessage(Component.text("The hider's timer has been unpaused.").color(NamedTextColor.GREEN));
				}
				
				if(args[1].equalsIgnoreCase("reward")) {
					HideAndSeek.getGameState().rewardHider();
				}
			} else {
				
				sender.sendMessage(Component.text("Current hider is " + HideAndSeek.getGameState().getHider().getName()));
			}
		}
		
		else if(args[0].equals("movecenter")) {
			if(!sender.hasPermission("hideandseek.command")) {
				sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
				return true;
			}
			HideAndSeek.instance.preferredCenterX = player.getLocation().toCenterLocation().getBlockX();
			HideAndSeek.instance.preferredCenterZ = player.getLocation().toCenterLocation().getBlockZ();
			ConfigurationSection centerLocation = HideAndSeek.instance.getConfig().createSection("center location");
			centerLocation.set("x", HideAndSeek.instance.preferredCenterX);
			centerLocation.set("z", HideAndSeek.instance.preferredCenterZ);
			HideAndSeek.instance.saveConfig();
			
			if(HideAndSeek.getGameState() != null) {
				HideAndSeek.getGameState().setCenter(player.getLocation().toCenterLocation());
			}
		}
		
		else if(args[0].equals("healall")) {
			if(!sender.hasPermission("hideandseek.command")) {
				sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
				return true;
			}
			for(Player p : Bukkit.getOnlinePlayers()) {
				p.setHealth(20);
				p.setFoodLevel(20);
			}
		} else {
			sender.sendMessage(Component.text("Unknown sub-command.").color(NamedTextColor.RED));
		}
		
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
			@NotNull String label, @NotNull String @NotNull [] args) {
		
		if(args.length <= 1) {
			if(!sender.hasPermission("hideandseek.command")) {
				return List.of("join", "leave");
			}
			return List.of("start", "stop", "reload", "hider", "movecenter", "join", "leave");
		}
		if(args.length > 0 && args.length < 3 && args[0].equalsIgnoreCase("hider") && sender.hasPermission("hideandseek.command")) {
			return List.of("set", "pause", "unpause", "reward");
		}
		return List.of();
	}
}
