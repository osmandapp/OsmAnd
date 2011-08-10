package net.osmand.data.index;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;

public class MyCookieStore implements CookieStore {

	private final CookieStore cookieStore;

	public MyCookieStore(CookieStore cookieStore) {
		this.cookieStore = cookieStore;
	}

	@Override
	public void add(URI uri, HttpCookie cookie) {
		cookieStore.add(uri, cookie);
	}

	@Override
	public List<HttpCookie> get(URI uri) {
		return cookieStore.get(uri);
	}

	@Override
	public List<HttpCookie> getCookies() {
		return cookieStore.getCookies();
	}

	@Override
	public List<URI> getURIs() {
		return cookieStore.getURIs();
	}

	@Override
	public boolean remove(URI uri, HttpCookie cookie) {
		return cookieStore.remove(uri, cookie);
	}

	@Override
	public boolean removeAll() {
		return cookieStore.removeAll();
	}

	public String getCookie(String key) {
		for (HttpCookie c : cookieStore.getCookies()) {
			if (c.getName().equals(key)) {
				return c.getValue();
			}
		}
		return null;
	}
}
