package com.amazon.customskill;

public class Tester {
	
	private static String[][] UMLAUT_REPLACEMENTS = { { "Ä", "Ae" }, { "Ü", "Ue" }, { "Ö", "Oe" }, { "ä", "ae" }, { "ü", "ue" }, { "ö", "oe" }, { "ß", "ss" } };
	public static String replaceUmlaute(String orig) {
		    String result = orig;

		    for (int i = 0; i < UMLAUT_REPLACEMENTS.length; i++) {
		        result = result.replaceAll(UMLAUT_REPLACEMENTS[i][0], UMLAUT_REPLACEMENTS[i][1]);
		    }

		    return result;
		}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String a = "münze";
		System.out.println(replaceUmlaute(a));
	}

}
