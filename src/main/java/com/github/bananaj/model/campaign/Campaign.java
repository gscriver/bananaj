/**
 * @author alexanderweiss
 * @date 19.11.2015
 */
package com.github.bananaj.model.campaign;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.github.bananaj.connection.MailChimpConnection;
import com.github.bananaj.exceptions.CampaignSettingsException;
import com.github.bananaj.model.JSONParser;
import com.github.bananaj.model.ReportSummary;
import com.github.bananaj.model.Tracking;
import com.github.bananaj.model.report.Report;
import com.github.bananaj.utils.DateConverter;
import com.github.bananaj.utils.JSONObjectCheck;

/**
 * Class for representing a mailchimp campaign
 * @author alexanderweiss
 *
 */
public class Campaign implements JSONParser {

	private MailChimpConnection connection;
	
	private String id;
	private Integer webId;
	private String parentCampaignId;
	private CampaignType type;
	private ZonedDateTime createTime;
	private String archiveUrl;
	private String longArchiveUrl;
	private CampaignStatus status;
	private Integer emailsSent;
	private ZonedDateTime sendTime;
	private CampaignContentType contentType;
	private Boolean needsBlockRefresh;
	private Boolean resendable;
	private CampaignRecipients recipients;
	private CampaignSettings settings;
	//private VariateSettings variate_settings;
	private Tracking tracking;
	//private RssOpts rss_opts;
	//private AbSplitOpts ab_split_opts;
	//private SocialCard social_card;
	private ReportSummary reportSummary;
	//private DeliveryStatus delivery_status;
	
	private CampaignContent content;

	public Campaign() {
		
	}
	
	public Campaign(MailChimpConnection connection, JSONObject jsonObj) throws Exception {
		parse(connection, jsonObj);
	}

	public void parse(MailChimpConnection connection, JSONObject campaign) {
		JSONObjectCheck jObj = new JSONObjectCheck(campaign);
		this.connection = connection;
		id = jObj.getString("id");
		this.webId = jObj.getInt("web_id");
		this.parentCampaignId = jObj.getString("parent_campaign_id");
		this.type = jObj.getEnum(CampaignType.class, "type");
		this.createTime = jObj.getISO8601Date("create_time");
		this.archiveUrl = jObj.getString("archive_url");
		this.longArchiveUrl = jObj.getString("long_archive_url");
		this.status =  jObj.getEnum(CampaignStatus.class, "status");
		this.emailsSent = jObj.getInt("emails_sent");
		this.sendTime = jObj.getISO8601Date("send_time");
		this.contentType = jObj.getEnum(CampaignContentType.class, "content_type");
		this.needsBlockRefresh = jObj.getBoolean("needs_block_refresh");
		this.resendable = jObj.getBoolean("resendable");

		if (campaign.has("recipients")) {
			this.recipients = new CampaignRecipients(campaign.getJSONObject("recipients"));
		}
		if (campaign.has("settings")) {
			this.settings = new CampaignSettings(campaign.getJSONObject("settings"));
		}
		if (campaign.has("tracking")) {
			this.tracking = new Tracking(campaign.getJSONObject("tracking"));
		}
		if (campaign.has("report_summary")) {
			this.reportSummary = new ReportSummary(campaign.getJSONObject("report_summary"));
		}
	}
	
	/**
	 * Update the campaign settings given specified CampaignSettings.
	 * @param campaignSettings
	 */
	public void updateSettings(CampaignSettings campaignSettings) throws CampaignSettingsException, IOException, Exception {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("settings", campaignSettings.getJsonRepresentation());
		String response = getConnection().do_Patch(new URL(getConnection().getCampaignendpoint()+"/"+getId()), jsonObj.toString(), getConnection().getApikey());
		parse(connection, new JSONObject(response));
	}

	public void update() throws IOException, Exception {
		JSONObject jsonObj = new JSONObject();
		//jsonObj.put("recipients", recipients.getJsonRepresentation());
		jsonObj.put("settings", settings.getJsonRepresentation());
		//jsonObj.put("variate_settings", settings.getJsonRepresentation());
		jsonObj.put("tracking", settings.getJsonRepresentation());
		//jsonObj.put("rss_opts", settings.getJsonRepresentation());
		//jsonObj.put("social_card", settings.getJsonRepresentation());
		String response = getConnection().do_Patch(new URL(getConnection().getCampaignendpoint()+"/"+getId()), jsonObj.toString(), getConnection().getApikey());
		parse(connection, new JSONObject(response));
	}
	
	public void delete() throws IOException, Exception {
		getConnection().do_Delete(new URL(getConnection().getCampaignendpoint() +"/"+getId()), getConnection().getApikey());
	}
	
	/**
	 * Send the campaign to the mailChimpList members
	 */
	public void send() throws IOException, Exception{
		getConnection().do_Post(new URL(connection.getCampaignendpoint()+"/"+this.getId()+"/actions/send"),connection.getApikey());
	}
	
	/**
	 * Send the campaign to the mailChimpList members
	 */
	public void sendTestEmail(String[] emails, CampaignSendType type) throws IOException, Exception {
		JSONObject data = new JSONObject();
		JSONArray testEmails = new JSONArray();
		for (String email : emails) {
			testEmails.put(email);
		}
		data.put("test_emails", testEmails);
		data.put("send_type", type.toString());
		getConnection().do_Post(new URL(getConnection().getCampaignendpoint()+"/"+getId()+"/actions/test"), data.toString(), getConnection().getApikey());
	}
	
	/**
	 * Cancel a Regular or Plain-Text Campaign after you send, before all of your
	 * recipients receive it. This feature is included with Mailchimp Pro.
	 * 
	 * @throws Exception
	 */
	public void cancel() throws IOException, Exception {
		getConnection().do_Post(new URL(getConnection().getCampaignendpoint()+"/"+getId()+"/actions/cancel-send"), getConnection().getApikey());
	}
	
	/**
	 * Creates a Resend to Non-Openers version of this campaign. We will also check
	 * if this campaign meets the criteria for Resend to Non-Openers campaigns.
	 * 
	 * @throws Exception
	 */
	public Campaign resend() throws IOException, Exception {
		String results = getConnection().do_Post(new URL(getConnection().getCampaignendpoint()+"/"+getId()+"/actions/create-resend"), getConnection().getApikey());
		return new Campaign(getConnection(), new JSONObject(results));
	}
	
	/**
	 * Pause an RSS-Driven campaign
	 * @throws Exception
	 */
	public void pause() throws IOException, Exception {
		getConnection().do_Post(new URL(getConnection().getCampaignendpoint()+"/"+getId()+"/actions/pause"), getConnection().getApikey());
	}

	/**
	 * Resume an RSS-Driven campaign.
	 * @throws Exception
	 */
	public void resume() throws IOException, Exception {
		getConnection().do_Post(new URL(getConnection().getCampaignendpoint()+"/"+getId()+"/actions/resume"), getConnection().getApikey());
	}

	/**
	 * Replicate a campaign in saved or send status
	 * @throws Exception
	 */
	public Campaign replicate() throws IOException, Exception {
		String results = getConnection().do_Post(new URL(getConnection().getCampaignendpoint()+"/"+getId()+"/actions/replicate"), getConnection().getApikey());
		return new Campaign(getConnection(), new JSONObject(results));
	}
	
	/**
	 * Get the send checklist for campaign. Review the send checklist for your campaign, and resolve any issues before sending. 
	 * @throws IOException 
	 * @throws Exception 
	 */
	public CampaignSendChecklist getSendChecklist() throws IOException, Exception {
		String results = getConnection().do_Get(new URL(getConnection().getCampaignendpoint()+"/"+getId()+"/send-checklist"), getConnection().getApikey());
		return new CampaignSendChecklist(new JSONObject(results));
	}
	
	// TODO: additional actions (schedule, unschedule)
//	public void schedule(ZonedDateTime schedule_time, Boolean timewarp, BatchDelivery batch_delivery) throws Exception {
//		getConnection().do_Post(new URL(getConnection().getCampaignendpoint()+"/"+getId()+"/actions/schedule"), getConnection().getApikey());
//	}
//	
//	/**
//	 * Unschedule a scheduled campaign that hasn’t started sending.
//	 * @throws Exception
//	 */
//	public void unschedule() throws Exception {
//		getConnection().do_Post(new URL(getConnection().getCampaignendpoint()+"/"+getId()+"/actions/unschedule"), getConnection().getApikey());
//	}
	
	/**
	 * Stops the sending of your campaign
	 * (!Only included in mailchimp pro)
	 */
	public void cancelSend() throws IOException, Exception {
		getConnection().do_Post(new URL(getConnection().getCampaignendpoint()+"/"+getId()+"/actions/cancel-send"), getConnection().getApikey());
	}

	/**
	 * Get the report of this campaign
	 * @throws Exception
	 */
	public Report getReport() throws IOException, Exception {
		final JSONObject report = new JSONObject(connection.do_Get(new URL(connection.getReportsendpoint()+"/"+getId()), connection.getApikey()));
		return new Report(connection, report);
	}

	/**
	 * @return the com.github.bananaj.connection
	 */
	public MailChimpConnection getConnection() {
		return connection;
	}

    /**
	 * @return A string that uniquely identifies this campaign.
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the content
	 * @throws Exception 
	 */
	public CampaignContent getContent() throws Exception {
		if (content == null) {
			getCampaignContent();
		}
		return content;
	}

	/**
	 * Get the content of this campaign from mailchimp API
	 * @throws IOException 
	 * @throws Exception 
	 */
	private void getCampaignContent() throws IOException, Exception {
		JSONObject content = new JSONObject(getConnection().do_Get(new URL(connection.getCampaignendpoint()+"/"+this.getId()+"/content"),connection.getApikey()));
		this.content = new CampaignContent(this, content);
	}

	/**
	 * Get feedback about a campaign
	 * @throws IOException 
	 * @throws Exception 
	 */
	public List<CampaignFeedback> getFeedback() throws IOException, Exception {
		List<CampaignFeedback> feedback = new ArrayList<CampaignFeedback>();
		JSONObject campaignFeedback = new JSONObject(getConnection().do_Get(new URL(connection.getCampaignendpoint()+"/"+this.getId()+"/feedback"),connection.getApikey()));
		
		JSONArray feedbackArray = campaignFeedback.getJSONArray("feedback");
		for( Integer i = 0; i< feedbackArray.length();i++)
		{
			JSONObject jsonObj = feedbackArray.getJSONObject(i);
			CampaignFeedback f = new CampaignFeedback(connection, jsonObj);
			feedback.add(f);
		}
		
		return feedback;
	}

	/**
	 * Get a specific feedback message
	 * @param feedbackId
	 * @throws IOException 
	 * @throws Exception 
	 */
	public CampaignFeedback getFeedback(String feedbackId) throws IOException, Exception  {
		JSONObject jsonObj = new JSONObject(getConnection().do_Get(new URL(connection.getCampaignendpoint()+"/"+getId()+"/feedback/"+feedbackId),connection.getApikey()));
		CampaignFeedback feedback = new CampaignFeedback(connection, jsonObj);
		return feedback;
	}

	/**
	 * Add campaign feedback
	 * @param message
	 * @return The newly created campaign feedback
	 * @throws IOException 
	 * @throws Exception
	 */
	public CampaignFeedback createFeedback(String message) throws IOException, Exception {
		CampaignFeedback feedback = new CampaignFeedback.Builder()
				.connection(connection)
				.campaignId(getId())
				.blockId(0)
				.message(message)
				.isComplete(true)
				.build();
		feedback.create();
		return feedback;
	}
	
	/**
	 * Delete a campaign feedback message
	 * @param feedbackId
	 * @throws IOException 
	 * @throws Exception
	 */
	public void deleteFeedback(String feedbackId) throws IOException, Exception {
		getConnection().do_Delete(new URL(connection.getCampaignendpoint()+"/"+getId()+"/feedback/"+feedbackId),connection.getApikey());
	}

	/**
	 * The ID used in the Mailchimp web application. View this campaign in your Mailchimp account at https://{dc}.admin.mailchimp.com/campaigns/show/?id={web_id}.
	 */
	public Integer getWebId() {
		return webId;
	}

	/**
	 * If this campaign is the child of another campaign, this identifies the parent campaign. For Example, for RSS or Automation children.
	 */
	public String getParentCampaignId() {
		return parentCampaignId;
	}

	/**
	 * There are four types of campaigns you can create in Mailchimp. A/B Split campaigns have been deprecated and variate campaigns should be used instead.
	 */
	public CampaignType getType() {
		return type;
	}

	/**
	 * The date and time the campaign was created
	 */
	public ZonedDateTime getCreateTime() {
		return createTime;
	}

	/**
	 * The link to the campaign’s archive version in ISO 8601 format
	 */
	public String getArchiveUrl() {
		return archiveUrl;
	}

	/**
	 * The original link to the campaign’s archive version
	 */
	public String getLongArchiveUrl() {
		return longArchiveUrl;
	}

	/**
	 * The current status of the campaign
	 */
	public CampaignStatus getStatus() {
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
	 * How the campaign’s content is put together 
	 */
	public CampaignContentType getContentType() {
		return contentType;
	}

	/**
	 * Determines if the campaign needs its blocks refreshed by opening the web-based campaign editor.
	 */
	public Boolean isNeedsBlockRefresh() {
		return needsBlockRefresh;
	}

	/**
	 * Determines if the campaign qualifies to be resent to non-openers
	 */
	public Boolean isResendable() {
		return resendable;
	}

	/**
	 * List settings for the campaign
	 */
	public CampaignRecipients getRecipients() {
		return recipients;
	}

	/**
	 * The settings for your campaign, including subject, from name, reply-to address, and more
	 */
	public CampaignSettings getSettings() {
		return settings;
	}

	/**
	 * The tracking options for a campaign
	 */
	public Tracking getTracking() {
		return tracking;
	}

	/**
	 * For sent campaigns, a summary of opens, clicks, and e-commerce data
	 */
	public ReportSummary getReportSummary() {
		return reportSummary;
	}

	@Override
	public String toString() {
		return "ID: " + getId() + " " + getSettings().getTitle() + System.lineSeparator() +
				"    WebId: " + getWebId() + System.lineSeparator() +
				(getType() != null ? "    Type: " + getType().toString() + System.lineSeparator() : "") +
				(getStatus() != null ? "    Status: " + getStatus().toString() + System.lineSeparator() : "") +
				"    Content Type: " + getContentType().toString() + System.lineSeparator() +
				"    Created: " +  DateConverter.toLocalString(getCreateTime()) + System.lineSeparator() +
				"    Sent: " + (getSendTime()!=null ? DateConverter.toLocalString(getSendTime()) : "Draft") + System.lineSeparator() +
				"    Archive URL: " + getArchiveUrl() + System.lineSeparator() +
				"    Emails Sent: " + getEmailsSent() + System.lineSeparator() +
				getSettings().toString() + System.lineSeparator() +
				(getRecipients() != null ? getRecipients().toString() + System.lineSeparator() : "") +
				getTracking().toString() + 
				(getReportSummary() != null ? System.lineSeparator() + getReportSummary().toString() : "");
	}

}
