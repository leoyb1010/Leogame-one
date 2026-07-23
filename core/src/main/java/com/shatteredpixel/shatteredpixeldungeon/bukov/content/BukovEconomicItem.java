package com.shatteredpixel.shatteredpixeldungeon.bukov.content;

/**
 * Optional metadata consumed by the raid ledger without coupling it to every
 * concrete host Item class.
 */
public interface BukovEconomicItem {

	String bukovDefinitionId();

	float bukovUnitWeight();

	int bukovUnitValue();
}
