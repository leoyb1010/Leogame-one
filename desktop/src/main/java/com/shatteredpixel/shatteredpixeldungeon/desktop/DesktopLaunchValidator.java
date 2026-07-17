/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.utils.Os;
import com.badlogic.gdx.utils.SharedLibraryLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;

public class DesktopLaunchValidator {

	//Validates that the launching JVM is correctly configured
	// and attempts to launch a new one if it is not
	// returns false if current JVM is invalid and should be killed.
	public static boolean verifyValidJVMState(String[] args){

		//mac computers require the -XstartOnFirstThread JVM argument
		if (SharedLibraryLoader.os == Os.MacOsX){

			// If XstartOnFirstThread is present and enabled, we can return true
			if ("1".equals(System.getenv("JAVA_STARTED_ON_FIRST_THREAD_" +
					ManagementFactory.getRuntimeMXBean().getName().split("@")[0]))) {
				return true;
			}

			// Check if we are the relaunched process, if so return true to avoid looping.
			// The game will likely crash, but that's unavoidable at this point.
			if ("true".equals(System.getProperty("shpdRelaunched"))){
				System.err.println(DesktopLaunchMessages.get("first_thread_unverified"));
				return true;
			}

			// Relaunch a new jvm process with the same arguments, plus -XstartOnFirstThread
			String sep = System.getProperty("file.separator");

			ArrayList<String> jvmArgs = new ArrayList<>();
			jvmArgs.add(System.getProperty("java.home") + sep + "bin" + sep + "java");
			jvmArgs.add("-XstartOnFirstThread");
			jvmArgs.add("-DshpdRelaunched=true");
			jvmArgs.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
			jvmArgs.add("-cp");
			jvmArgs.add(System.getProperty("java.class.path"));
			jvmArgs.add(DesktopLauncher.class.getName());

			System.err.println(DesktopLaunchMessages.get("first_thread_required"));
			System.err.println(DesktopLaunchMessages.get("first_thread_instruction"));
			System.err.println(DesktopLaunchMessages.get("first_thread_relaunch"));

			try {
				Process process = new ProcessBuilder(jvmArgs).redirectErrorStream(true).start();
				BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;

				//Relay console output from the relaunched process
				while ((line = out.readLine()) != null) {
					if (line.toLowerCase().startsWith("error")){
						System.err.println(line);
					} else {
						System.out.println(line);
					}
				}

				process.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return false;

		}

		return true;
	}

}
