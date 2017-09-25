package lod.utils;

import java.util.ArrayList;
import java.util.List;

public class NGram {
	private final char[] delimiters = new char[] { ' ', '\'', '-', ',', '_' };
	private final int n;
	private final String text;

	private final int[] indexes;
	private int index = -1;
	private int found = 0;

	public NGram(String text, int n) {
		this.text = text;
		this.n = n;
		indexes = new int[n];
	}

	private boolean seek() {
		if (index >= text.length()) {
			return false;
		}
		push();
		while (++index < text.length()) {
			if (text.charAt(index) == ' ' || text.charAt(index) == '-'
					|| text.charAt(index) == '_' || text.charAt(index) == '\''
					|| text.charAt(index) == '(' || text.charAt(index) == ')') {
				found++;
				if (found < n) {
					push();
				} else {
					return true;
				}
			}
		}
		return true;
	}

	private void push() {
		for (int i = 0; i < n - 1; i++) {
			indexes[i] = indexes[i + 1];
		}
		indexes[n - 1] = index + 1;
	}

	public List<String> list() {
		List<String> ngrams = new ArrayList<String>();
		while (seek()) {
			String gram = get();
			if (gram.length() > 2)
				ngrams.add(get());
		}
		return ngrams;
	}

	private String get() {
		return text.substring(indexes[0], index);
	}
}
