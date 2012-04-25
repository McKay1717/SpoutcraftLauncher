/*
 * This file is part of LauncherAPI (http://www.spout.org/).
 *
 * LauncherAPI is licensed under the SpoutDev License Version 1.
 *
 * LauncherAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the SpoutDev License Version 1.
 *
 * LauncherAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the SpoutDev License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */

package org.spoutcraft.launcher.api.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.spoutcraft.launcher.api.util.DownloadListener;
import org.spoutcraft.launcher.api.util.Utils;
import org.spoutcraft.launcher.api.util.YAMLProcessor;

public class MirrorUtils {
	private static boolean updated = false;
	private static File mirrorsYML = new File(Utils.getWorkingDirectory(), "spoutcraft" + File.separator + "mirrors.yml");
	private static final String baseURL = "http://get.spout.org/";
	private static List<String> mirrors = null;

	public static String getMirrorUrl(String mirrorURI, String fallbackUrl, DownloadListener listener) {
		if (MirrorUtils.mirrors == null) {
			Map<String, Integer> mirrors = getMirrors();
			Set<Entry<String, Integer>> set = mirrors.entrySet();

			ArrayList<String> goodMirrors = new ArrayList<String>(mirrors.size());
			Iterator<Entry<String, Integer>> iterator = set.iterator();
			while (iterator.hasNext()) {
				Entry<String, Integer> e = iterator.next();
				String mirror = "http://" + e.getKey();
				if (isAddressReachable(mirror, 250)) { //short timeout for fast mirrors
					goodMirrors.add(e.getKey());
				}
			}
			
			Collections.sort(goodMirrors, new MirrorComparator(mirrors));
			for (String mirror : goodMirrors) {
				System.out.println("Mirror " + mirror + " value " + mirrors.get(mirror));
			}
			
			MirrorUtils.mirrors = goodMirrors;
		}
		
		for (String mirror : MirrorUtils.mirrors){
			String lookup = "http://" + mirror + "/" + mirrorURI;
			if (isAddressReachable(lookup, 1000)) {
				return lookup;
			}
		}

		return fallbackUrl;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Integer> getMirrors() {
		YAMLProcessor config = getMirrorsYML();
		return (Map<String, Integer>) config.getProperty("mirrors");
	}

	public static boolean isAddressReachable(String url, int timeout) {
		try {
			URL test = new URL(url);
			HttpURLConnection.setFollowRedirects(false);
			HttpURLConnection urlConnect = (HttpURLConnection) test.openConnection();
			System.setProperty("http.agent", "");
			urlConnect.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.100 Safari/534.30");
			urlConnect.setRequestMethod("HEAD");
			urlConnect.setConnectTimeout(timeout);
			return (urlConnect.getResponseCode() == HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			return false;
		}
	}

	public static YAMLProcessor getMirrorsYML() {
		updateMirrorsYMLCache();
		YAMLProcessor config = new YAMLProcessor(mirrorsYML, false);
		try {
			config.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return config;
	}

	public static void updateMirrorsYMLCache() {
		if (!updated) {
			try {
				URL url = new URL("http://cdn.spout.org/mirrors.yml");
				HttpURLConnection con = (HttpURLConnection) (url.openConnection());
				System.setProperty("http.agent", "");
				con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.100 Safari/534.30");
				Utils.copy(con.getInputStream(), new FileOutputStream(mirrorsYML));
			} catch (IOException e) {
				e.printStackTrace();
			}
			updated = true;
		}
	}

	public static String getBaseURL() {
		return baseURL;
	}
}

class MirrorComparator implements Comparator<String> {
	final Map<String, Integer> values = new HashMap<String, Integer>();
	final Random rand = new Random();
	final Map<String, Integer> mirrors;
	public MirrorComparator(Map<String, Integer> mirrors) {
		this.mirrors = mirrors;
	}
	public int compare(String o1, String o2) {
		return getValue(o2) - getValue(o1);
	}
	
	private int getValue(String mirror) {
		if (values.containsKey(mirror)) {
			return values.get(mirror);
		}
		int value = rand.nextInt(mirrors.get(mirror)) + 1;
		values.put(mirror, value);
		return value;
	}
	
}