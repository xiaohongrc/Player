package com.hongenit.Aplay.bean;

public class UpdateInfo {
	private int versionCode;
	private String apkurl;
	private String title;
	private String sentence;
	
	

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSentence() {
		return sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}


	public int getVersionCode() {
		return versionCode;
	}

	public void setVersionCode(int versionCode) {
		this.versionCode = versionCode;
	}

	public String getApkurl() {
		return apkurl;
	}

	public void setApkurl(String apkurl) {
		this.apkurl = apkurl;
	}

	@Override
	public String toString() {
		return "UpdateInfo [versionCode=" + versionCode + ", apkurl=" + apkurl + ", title=" + title + ", sentence=" + sentence + "]";
	}
	
	

}
