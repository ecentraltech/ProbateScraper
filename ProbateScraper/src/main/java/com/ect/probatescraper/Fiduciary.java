package com.ect.probatescraper;

import java.util.List;

public class Fiduciary {

	public static final String REPRESENTED_BY = "represented by";
	public static final String PHONE = "phone:";
	public static final String FAX = "fax:";

	private String name;
	private String representedBy;
	private String addrLn1;
	private String addrLn2;
	private String cityStateZip;
	private String phone;
	private String fax;

	public Fiduciary() {
		//remember how this works
		this.name = "";
		this.representedBy = "";
		this.addrLn1 = "";
		this.addrLn2 = "";
		this.cityStateZip = "";
		this.phone = "";
		this.fax = "";
	}

	public Fiduciary(String name) {
		this();
		this.name = name;
	}

	public Fiduciary(List<String> groupedInfo){
		this();
		this.setGroupInfo(groupedInfo);
	}
	
	public void setGroupInfo(List<String> groupedInfo) {
		if (groupedInfo != null && !groupedInfo.isEmpty()) {
			if (isPhoneFaxGroup(groupedInfo)) {
				setPhoneFaxGroupInfo (groupedInfo);
			}
			else {
				setAddressGroupInfo(groupedInfo);
			}
		}
	}
	
	public void setPhoneFaxGroupInfo (List<String> groupedInfo) {
		for (String info : groupedInfo) {
			if (info.toLowerCase().startsWith(PHONE)) {
				setPhone(info.substring(PHONE.length()).trim());
			}
			else if (info.toLowerCase().startsWith(FAX)) {
				setFax(info.substring(FAX.length()).trim());
			}
		}
	}

	public void setAddressGroupInfo (List<String> groupedInfo) {
		if (groupedInfo.size() > 0) {
			String name = groupedInfo.get(0);
			int pos = name.toLowerCase().indexOf(REPRESENTED_BY);
			if (pos > -1) {
				this.name = name.substring(0, pos);
				this.representedBy = name.substring((pos + REPRESENTED_BY.length()));
			}
			else {
				this.name = name;
			}
			
			if (groupedInfo.size() > 1) {
				this.addrLn1 = groupedInfo.get(1);
			}
			
			if (groupedInfo.size() > 2) {
				if (groupedInfo.size() > 3) {
					this.addrLn2 = groupedInfo.get(2);
					this.cityStateZip = groupedInfo.get(3);
				}
				else {
					this.cityStateZip = groupedInfo.get(2);
				}
			}
		}
	}

	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		int pos = name.toLowerCase().indexOf(REPRESENTED_BY);
		if (pos > -1) {
			this.name = name.substring(0, pos);
			this.representedBy = name.substring((pos + REPRESENTED_BY.length()));
		}
		else {
			this.name = name;
		}
	}

	public String getRepresentedBy() {
		return representedBy;
	}

	public void setRepresentedBy(String representedBy) {
		this.representedBy = representedBy;
	}

	public String getAddrLn1() {
		return addrLn1;
	}

	public void setAddrLn1(String addrLn1) {
		this.addrLn1 = addrLn1;
	}

	public String getAddrLn2() {
		return addrLn2;
	}

	public void setAddrLn2(String addrLn2) {
		this.addrLn2 = addrLn2;
	}

	public String getCityStateZip() {
		return cityStateZip;
	}

	public void setCityStateZip(String cityStateZip) {
		this.cityStateZip = cityStateZip;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getFax() {
		return fax;
	}

	public void setFax(String fax) {
		this.fax = fax;
	}
	
	public static final boolean isPhoneFaxGroup(List<String> groupedInfo) {
		if (groupedInfo != null && !groupedInfo.isEmpty()) {
			String firstRow = groupedInfo.get(0); 
			if (firstRow.toLowerCase().startsWith(PHONE) || firstRow.toLowerCase().startsWith(FAX)) {
				return true;
			}
		}

		return false;
	}
}