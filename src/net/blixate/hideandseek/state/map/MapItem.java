package net.blixate.hideandseek.state.map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

public class MapItem {
	private MapView mapView;
	public MapOverlay overlay;
	
	public MapItem(World world) {
		// because apparently getMap() can be null, so
		// we create a new map anyway.
		mapView = Bukkit.createMap(world);
		mapView.setCenterX(world.getWorldBorder().getCenter().getBlockX());
		mapView.setCenterZ(world.getWorldBorder().getCenter().getBlockZ());
		mapView.getRenderers().clear();
		mapView.addRenderer(overlay = new MapOverlay());
		mapView.setTrackingPosition(true);
		mapView.setUnlimitedTracking(true);
		mapView.setScale(MapView.Scale.NORMAL); // 512x512
	}
	
	/** Sets the center of the map to the specified location. If this
	 * map isn't initalized, or the location is null, this silently fails. */
	public void setCenter(Location center) {
		if(mapView != null && center != null) {
			mapView.setCenterX(center.getBlockX());
			mapView.setCenterZ(center.getBlockZ());
		}
	}
	
	public ItemStack getMap() {
		ItemStack item = new ItemStack(Material.FILLED_MAP, 1);
		MapMeta meta = (MapMeta) item.getItemMeta();
		meta.setMapView(mapView);
		item.setItemMeta(meta);
		return item;
	}
}
