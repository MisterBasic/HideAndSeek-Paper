package net.blixate.hideandseek.challenges;

public enum SeekerChallenge {
	// Easy Challenges
	FALL_HIGH(1, "Fall from 100 blocks."),
	BREED_ANIMALS(1, "Breed animals."),
	BECOME_POISONED(1, "Become Poisoned."),
	DEPLETE_IRON_TOOL(1, "Fully deplete an Iron tool."),
	CREATE_BANNER(1, "Craft a Banner."), // X
	DYE(1, "Dye any material."), // X
	// Medium Challenges
	TOUCH_BEDROCK(2, "Touch Bedrock."),
	CREATE_GOLEM(2, "Create a Golem."),
	MINE_DIAMONDS(2, "Mine Diamonds."),
	ACCUMULATE_DAMAGE(2, "Accumulate 10 hearts of damage."),
	LIGHT_TNT(2, "Light a TNT"),
	// Hard challenges
	CREATE_CAKE(3, "Bake a Cake!"),
	CREATE_NETHER_PORTAL(3, "Light a Nether Portal."),
	REACH_BUILD_LIMIT(3, "Reach the build height limit.")
	;
	
	private int difficulty;
	private String text;
	
	SeekerChallenge(int difficulty, String text) {
		this.difficulty = difficulty;
		this.text = text;
	}
	
	public int getDifficulty() {
		return difficulty;
	}
	
	public String getText() {
		return text;
	}
}
