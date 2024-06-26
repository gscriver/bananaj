package com.github.bananaj.model.automation.emails;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;

import org.json.JSONObject;

import com.github.bananaj.connection.MailChimpConnection;
import com.github.bananaj.model.JSONParser;
import com.github.bananaj.model.ReportSummary;
import com.github.bananaj.model.Tracking;
import com.github.bananaj.model.automation.AutomationDelay;
import com.github.bananaj.model.automation.AutomationStatus;
import com.github.bananaj.model.campaign.CampaignRecipients;
import com.github.bananaj.utils.DateConverter;
import com.github.bananaj.utils.JSONObjectCheck;

public class AutomationEmail implements JSONParser {

	private String id;
	private Integer webId;
	private String workflowId;
	private Integer position;
	private AutomationDelay delay;
	private ZonedDateTime createTime;
	private ZonedDateTime startTime;
	private String archiveUrl;
	private AutomationStatus status;
	private Integer emailsSent;
	private ZonedDateTime sendTime;
	private String contentType;
	private Boolean needsBlockRefresh;
	private Boolean hasLogoMergeTag;
	private CampaignRecipients recipients;
	private AutomationEmailSettings settings;
	private Tracking tracking;
	//private Object social_card;
	//private Object trigger_settings;
	private ReportSummary reportSummary;
	private MailChimpConnection connection;
	
	
	public AutomationEmail(MailChimpConnection connection, JSONObject jsonObj) {
		parse(connection, jsonObj);
	}

	public AutomationEmail() {

	}

	public void parse(MailChimpConnection connection, JSONObject automationemail) {
		JSONObjectCheck jObj = new JSONObjectCheck(automationemail);
		this.connection = connection;
		id = jObj.getString("id");
		webId = jObj.getInt("web_id");
		workflowId = jObj.getString("workflow_id");
		position = jObj.getInt("position");
		if (automationemail.has("delay")) {
			delay = new AutomationDelay(automationemail.getJSONObject("delay"));
		}
		createTime = jObj.getISO8601Date("create_time");
		startTime = jObj.getISO8601Date("start_time");
		archiveUrl = jObj.getString("archive_url");
		status = jObj.getEnum(AutomationStatus.class, "status");
		emailsSent = jObj.getInt("emails_sent");
		sendTime = jObj.getISO8601Date("send_time");
		contentType = jObj.getString("content_type");
		needsBlockRefresh = jObj.getBoolean("needs_block_refresh");
		hasLogoMergeTag = jObj.getBoolean("has_logo_merge_tag");
		if (automationemail.has("recipients")) {
			recipients = new CampaignRecipients(automationemail.getJSONObject("recipients"));
		}
		if (automationemail.has("settings")) {
			settings = new AutomationEmailSettings(automationemail.getJSONObject("settings"));
		}
		if (automationemail.has("tracking")) {
			tracking = new Tracking(automationemail.getJSONObject("tracking"));
		}
		if (automationemail.has("report_summary")) {
			reportSummary = new ReportSummary(automationemail.getJSONObject("report_summary"));
		}
	}

	/**
	 * List automated email subscribers
	 */
	public AutomationSubscriberQueue getSubscriberQueue() throws IOException, Exception {
		JSONObject jsonObj = new JSONObject(connection.do_Get(new URL(connection.getAutomationendpoint() + "/" + workflowId + "/emails/" + getId() + "/queue"), connection.getApikey()));
		return new AutomationSubscriberQueue(connection, jsonObj);
	}
	
	public AutomationSubscriber getSubscriber(String subscriberHash) throws IOException, Exception {
		JSONObject jsonObj = new JSONObject(connection.do_Get(new URL(connection.getAutomationendpoint() + "/" + workflowId + "/emails/" + getId() + "/queue/" + subscriberHash), connection.getApikey()));
		return new AutomationSubscriber(jsonObj);
	}
	
	/**
	 * Manually add a subscriber to a workflow, bypassing the default trigger
	 * settings. You can also use this endpoint to trigger a series of automated
	 * emails in an API 3.0 workflow type or add subscribers to an automated email
	 * queue that uses the API request delay type.
	 * 
	 * @param emailAddress The list member’s email address
	 * @throws Exception
	 */
	public void addSubscriber(String emailAddress) throws IOException, Exception {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("email_address", emailAddress);
		connection.do_Post(new URL(connection.getAutomationendpoint() + "/" + workflowId + "/emails/" + getId() + "/queue"), jsonObj.toString(), connection.getApikey());
		// Note: MailChimp documents this as returning an AutomationSubscriber but in practice it returns nothing
	}
	
	public void update() throws IOException, Exception {
		JSONObject json = getJsonRepresentation();
		String results = connection.do_Patch(new URL(connection.getAutomationendpoint() + "/" + workflowId + "/emails/" + getId()), json.toString(), connection.getApikey());
		parse(connection, new JSONObject(results));
	}
	
	public void delete() throws IOException, Exception {
		connection.do_Delete(new URL(connection.getAutomationendpoint() + "/" + workflowId + "/emails/" + getId()), connection.getApikey());
	}
	
    /**
	 * @return A string that uniquely identifies the Automation email.
	 */
	public String getId() {
		return id;
	}

	/**
	 * The ID used in the Mailchimp web application. View this automation in your
	 * Mailchimp account at
	 * https://{dc}.admin.mailchimp.com/campaigns/show/?id={web_id}.
	 */
	public Integer getWebId() {
		return webId;
	}

	/**
	 * A string that uniquely identifies an Automation workflow
	 */
	public String getWorkflowId() {
		return workflowId;
	}

	/**
	 * the position of an Automation email in a workflow
	 */
	public Integer getPosition() {
		return position;
	}

	/**
	 * The delay settings for an automation email
	 */
	public AutomationDelay getDelay() {
		return delay;
	}

	/**
	 * The date and time the campaign was created
	 */
	public ZonedDateTime getCreateTime() {
		return createTime;
	}

	/**
	 * The date and time the campaign was started
	 */
	public ZonedDateTime getStartTime() {
		return startTime;
	}

	/**
	 * The link to the campaign’s archive version in ISO 8601 format
	 */
	public String getArchiveUrl() {
		return archiveUrl;
	}

	/**
	 * The current status of the campaign
	 */
	public AutomationStatus getStatus() {
		return status;
	}

	/**
	 * The total number of emails sent for this campaign
	 */
	public Integer getEmailsSent() {
		return emailsSent;
	}

	/**
	 * The date and time a campaign was sent
	 */
	public ZonedDateTime getSendTime() {
		return sendTime;
	}

	/**
	 * How the campaign’s content is put together (‘template’, ‘drag_and_drop’, ‘html’, ‘url’)
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Determines if the automation email needs its blocks refreshed by opening the web-based campaign editor
	 */
	public Boolean isNeedsBlockRefresh() {
		return needsBlockRefresh;
	}

	/**
	 * Determines if the campaign contains the |BRAND:LOGO| merge tag
	 */
	public Boolean isHasLogoMergeTag() {
		return hasLogoMergeTag;
	}

	/**
	 * List settings for the campaign
	 */
	public CampaignRecipients getRecipients() {
		return recipients;
	}

	/**
	 * Settings for the campaign including the email subject, from name, and from email address
	 */
	public AutomationEmailSettings getSettings() {
		return settings;
	}

	/**
	 * The tracking options for a campaign
	 */
	public Tracking getTracking() {
		return tracking;
	}

	/**
	 * For sent campaigns, a summary of opens, clicks, and unsubscribes
	 */
	public ReportSummary getReportSummary() {
		return reportSummary;
	}

	/**
	 * Helper method to convert JSON for mailchimp PATCH/POST operations
	 */
	protected JSONObject getJsonRepresentation() throws Exception {
		JSONObject json = new JSONObject();
		
		if (settings != null) {
			JSONObject settingsObj = settings.getJsonRepresentation();
			json.put("settings", settingsObj);
		}
		
		if (delay != null) {
			JSONObject delayObj = delay.getJsonRepresentation();
			json.put("delay", delayObj);
		}
		
		return json;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return
				"Automation Email:" + System.lineSeparator() +
				"    Id: " + getId() + System.lineSeparator() +
				"    Web Id: " + getWebId() + System.lineSeparator() +
				"    Position: " + getPosition() + System.lineSeparator() +
				"    Created: " + DateConverter.toLocalString(getCreateTime()) + System.lineSeparator() +
				"    Started: " + (getStartTime()!=null ? DateConverter.toLocalString(getStartTime()) : "") + System.lineSeparator() +
				"    Archive URL: " + getArchiveUrl() + System.lineSeparator() +
				"    Status: " + getStatus().toString() + System.lineSeparator() +
				"    Emails Sent: " + getEmailsSent() + System.lineSeparator() +
				"    Send Time: " + (getSendTime()!=null ? DateConverter.toLocalString(getSendTime()) : "") + System.lineSeparator() +
				"    Content Type: " + getContentType() + System.lineSeparator() +
				"    Needs Block Refresh: " + isNeedsBlockRefresh() + System.lineSeparator() +
				"    Has Logo Merge Tag: " + isHasLogoMergeTag() + System.lineSeparator() +
				getDelay().toString() + System.lineSeparator() +
				getRecipients().toString() + System.lineSeparator() +
				getSettings().toString() + System.lineSeparator() +
				getTracking().toString() + 
				(reportSummary != null ? System.lineSeparator() + reportSummary.toString() : "");
	}
	
}
