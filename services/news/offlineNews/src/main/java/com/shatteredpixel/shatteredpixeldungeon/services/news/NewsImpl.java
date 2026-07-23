/*
 * Escape from Bukov offline news adapter.
 * Copyright (C) 2026 Leo Yuan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.services.news;

/** Production adapter: this personal edition never initializes a network news service. */
public final class NewsImpl {

	private NewsImpl() {
	}

	public static NewsService getNewsService() {
		return null;
	}

	public static boolean supportsNews() {
		return false;
	}
}
