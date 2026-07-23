package com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial;

/** One-shot, non-modal teaching moments from plan section 79. */
public enum BukovTutorialEvent {
	FIREARM_PICKUP("左键/R2 射击，R/X 装填"),
	EMPTY_MAGAZINE("弹匣已空 · 按 R/X 装填"),
	CONTAINER_OPENED("物品有重量，超重后需要丢弃取舍"),
	OVERWEIGHT("负重已满 · 打开背包丢弃低价值物品"),
	BLEEDING("正在流血 · 用快捷医疗或背包中的绷带"),
	EXTRACTION_NEAR("按住交互保持撤离读条，受击会回退"),
	BOSS_WARNING("Boss 可以绕开，打不打由你决定"),
	FIRST_DEATH("带入行动的会损失，仓库里的仍然保留\n布衣行动可以无本回仓");

	public final String message;

	BukovTutorialEvent(String message) {
		this.message = message;
	}
}
