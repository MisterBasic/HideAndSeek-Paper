package net.blixate.hideandseek;

public class ServerLoop implements Runnable {
	
	public void run() {
		if(HideAndSeek.instance.gameState != null) {
			
			HideAndSeek.instance.gameState.tick();
		}
	}
	
}
