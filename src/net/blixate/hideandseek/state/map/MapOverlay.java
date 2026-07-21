package net.blixate.hideandseek.state.map;

import java.awt.Color;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

public class MapOverlay extends MapRenderer {
	
	private static final int ALPHA_VALUE = 0xAB000000;
	private static final Color COLOR1 = new Color(0x0);
	private static final Color COLOR2 = new Color(0x4F4F4F | ALPHA_VALUE, true);
	
	private static final int MAP_SIZE = 128;
	
	boolean[][] isDarkened;
	
	public MapOverlay() {
		isDarkened = new boolean[MAP_SIZE][];
		for(int y = 0; y < MAP_SIZE; y++) {
			isDarkened[y] = new boolean[MAP_SIZE];
		}
	}
	
	public void reset() {
		for(int y = 0; y < MAP_SIZE; y++) {
			for(int x = 0; x < MAP_SIZE; x++) {
				isDarkened[y][x] = false;
			}
		}
	}
	
	@Override
	public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
		for(int y = 0; y < MAP_SIZE; y++) {
			for(int x = 0; x < MAP_SIZE; x++) {
				Color pixelColor = canvas.getBasePixelColor(x, y);
				if(isDarkened[y][x])
					canvas.setPixelColor(x, y, overlayColor(pixelColor, getDarkPixel(x, y)));
				else
					canvas.setPixelColor(x, y, pixelColor);
			}
		}
	}
	
	private Color getDarkPixel(int x, int y) {
		return ((x + y) % 4 == 0 ? COLOR1 : COLOR2);
	}

	public void darkenCoordinate(int x, int y) {
		// don't darken coordinates that have already been blacked out
		if(!isDarkened[y][x])
			isDarkened[y][x] = true;
	}
	
	public static Color overlayColor(Color background, Color foreground) {
	    // Get alpha as a percentage (0.0 to 1.0)
	    float alphaFg = foreground.getAlpha() / 255f; 
	    if(alphaFg >= 1f) {
	    	// speed up the calculations
	    	return foreground;
	    }
	    // Combine red, green, and blue channels
	    int r = Math.round((foreground.getRed() * alphaFg) + (background.getRed() * (1 - alphaFg)));
	    int g = Math.round((foreground.getGreen() * alphaFg) + (background.getGreen() * (1 - alphaFg)));
	    int b = Math.round((foreground.getBlue() * alphaFg) + (background.getBlue() * (1 - alphaFg)));

	    // Return the resulting fully opaque color
	    return new Color(r, g, b);
	}

}
