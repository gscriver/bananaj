package com.github.bananaj.model.report;

import java.time.ZonedDateTime;

import org.json.JSONObject;

import com.github.bananaj.connection.MailChimpConnection;
import com.github.bananaj.model.JSONParser;
import com.github.bananaj.utils.DateConverter;
import com.github.bananaj.utils.JSONObjectCheck;

public class ClickReport implements JSONParser {
	private String campaignId;
	private String id;
	private String url;
	private Integer totalClicks;
	private Double clickPercentage;
	private Integer uniqueClicks;
	private Double uniqueClickPercentage;
	private ZonedDateTime lastClick;
	private ClickABSplit abSplit_a;
	private ClickABSplit abSplit_b;

	public ClickReport() {

	}

	public ClickReport(JSONObject jsonObj) {
		parse(null, jsonObj);
	}

	@Override
	public void parse(MailChimpConnection connection, JSONObject clickrpt) {
		JSONObjectCheck entity = new JSONObjectCheck(clickrpt);
		campaignId = entity.getString("campaign_id");
		id = entity.getString("id");
		url = entity.getString("url");
		totalClicks = entity.getInt("total_clicks");
		clickPercentage = entity.getDouble("click_percentage");
		uniqueClicks = entity.getInt("unique_clicks");
		uniqueClickPercentage = entity.getDouble("unique_click_percentage");
		lastClick = entity.getISO8601Date("last_click");

		if(entity.has("ab_split")) {
			JSONObject split = entity.getJSONObject("ab_split");
			if (split.has("a")) {
				abSplit_a = new ClickABSplit("a", split.getJSONObject("a"));
			}
			if (split.has("b")) { 
				abSplit_b = new ClickABSplit("b", split.getJSONObject("b"));
			}
		}
	}


	/**
	 * @return The campaign id.
	 */
	public String getCampaignId() {
		return campaignId;
	}

	/**
	 * @return The unique id for the link.
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return The URL for the link in the campaign.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return The number of total clicks for a link.
	 */
	public Integer getTotalClicks() {
		return totalClicks;
	}

	/**
	 * @return The percentage of total clicks a link generated for a campaign.
	 */
	public Double getClickPercentage() {
		return clickPercentage;
	}

	/**
	 * @return Number of unique clicks for a link.
	 */
	public Integer getUniqueClicks() {
		return uniqueClicks;
	}

	/**
	 * @return The percentage of unique clicks a link generated for a campaign.
	 */
	public Double getUniqueClickPercentage() {
		return uniqueClickPercentage;
	}

	/**
	 * @return The date and time for the last recorded click for a link.
	 */
	public ZonedDateTime getLastClick() {
		return lastClick;
	}

	/**
	 * @return Stats for Group A.
	 */
	public ClickABSplit getAbSplit_a() {
		return abSplit_a;
	}

	/**
	 * @return Stats for Group B.
	 */
	public ClickABSplit getAbSplit_b() {
		return abSplit_b;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(200);
		sb.append("Click Report: " + getId() + " " + getUrl() + System.lineSeparator());
		sb.append("    Total Clicks: " + getTotalClicks() + System.lineSeparator());
		sb.append("    Click Percentage: " + getClickPercentage() + System.lineSeparator());
		sb.append("    Unique Clicks: " + getUniqueClicks() + System.lineSeparator());
		sb.append("    Unique Click Percentage: " + getUniqueClickPercentage()+ System.lineSeparator());
		sb.append("    Campaign Id: " + getCampaignId());
		if (getAbSplit_a() != null) {sb.append(System.lineSeparator() + getAbSplit_a().toString());}
		if (getAbSplit_b() != null) {sb.append(System.lineSeparator() + getAbSplit_b().toString());}
		return sb.toString();
	}

}
