package net.blixate.hideandseek;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.blixate.hideandseek.challenges.ChallengeListener;
import net.blixate.hideandseek.commands.HideAndSeekCommand;
import net.blixate.hideandseek.state.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class HideAndSeek extends JavaPlugin {
	public static HideAndSeek instance;
	
	public static Component PREFIX = MiniMessage.miniMessage().deserialize("<blue>Hide and Seek</blue> <dark_gray>»</dark_gray><gray> ");
	
	public final NamespacedKey ITEM_KEY = new NamespacedKey(this, "item");
	public final NamespacedKey CHALLENGE_KEY = new NamespacedKey(this, "challenge");
	public final NamespacedKey TIME_BONUS_KEY = new NamespacedKey(this, "bonustime");
	
	public GameState gameState;
	public Random rng;

	public int preferredCenterX = 0;
	public int preferredCenterZ = 0;
	
	public void onEnable() {
		instance = this;
		rng = new Random();
		Bukkit.getPluginManager().registerEvents(new HideAndSeekEventListener(), this);
		Bukkit.getPluginManager().registerEvents(new ChallengeListener(), this);
		Bukkit.getScheduler().runTaskTimer(this, new ServerLoop(), 0, 10);
		this.getCommand("hideandseek").setExecutor(new HideAndSeekCommand());
		// TODO: Comment this out so config stuff saves
		//this.saveResource("config.yml", true);
		this.reloadConfig();
		if(getConfig().contains("center location")) {
			preferredCenterX = getConfig().getInt("center location.x");
			preferredCenterZ = getConfig().getInt("center location.z");
		}
	}

	public void onDisable() {
		if(gameState != null) {
			gameState.stop();
		}
	}
	
	public static GameState getGameState() {
		return instance.gameState;
	}
	
	/** Utility function to check if a game is active AND if the player is currently playing. */
	public static boolean isPlaying(Player player) {
		return instance != null && instance.gameState != null && instance.gameState.isPlayerInGame(player);
		
	}
}
