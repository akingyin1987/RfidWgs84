package com.akingyin.rfidwgs.util;

public class HtmlUtils {

	public   static  final  String SPACE  = "&nbsp;";
	public static final String getRedHtml(String content) {
		return "<font color=#FF0000>" + content + "</font>";
	}

	public static final String getGreenHtml(String content) {
		return "<font color=#009966>" + content + "</font>";
	}

	public static final String getBlueHtml(String content) {
		return "<font color=#0000FF>" + content + "</font>";
	}

	public static final String getColorHtml(String content, String color) {
		return "<font color=" + color + ">" + content + "</font>";
	}

	public static final String getSmallColorHtml(String content, String color) {
		return "<font color=" + color + "><small>" + content + "</small></font>";
	}

	public static final String getReadPHtml(String content) {
		return "<p style=font-size:30px;color:red>" + content + "</p>";
	}

	public static final String getBigColorHtml(String content, String color) {
		return "<font color=" + color + "><big>" + content + "</big></font>";
	}

	public static final String getBigStrongColorHtml(String content, String color) {
		return "<font color=" + color + "><strong><big>" + content + "</big></strong></font>";
	}

	public static final String getBigRedHtml(String content) {
		 return "<font color=#FF0000><big>" + content + "</big></font>";
	}
	public static final String getBigStrongRedHtml(String content) {
		return "<font color=#FF0000><strong><big>" + content + "</big></strong></font>";
	}
}
