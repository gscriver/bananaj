/**
 * @author alexanderweiss
 * @date 06.11.2015
 */
package com.github.bananaj.model.list.member;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

import com.github.bananaj.connection.MailChimpConnection;
import com.github.bananaj.connection.MailChimpQueryParameters;
import com.github.bananaj.model.JSONParser;
import com.github.bananaj.model.ModelIterator;
import com.github.bananaj.model.list.MailChimpList;
import com.github.bananaj.utils.DateConverter;
import com.github.bananaj.utils.EmailValidator;
import com.github.bananaj.utils.JSONObjectCheck;
import com.github.bananaj.utils.MD5;
import com.github.bananaj.utils.URLHelper;


/**
 * Object for representing a mailchimp member
 * @author alexanderweiss
 *
 */
public class Member implements JSONParser {

	private String id;
	private String emailAddress;
	private String uniqueEmailId;
	private EmailType emailType;
	private MemberStatus status;
	private String unsubscribeReason;
	private Map<String, Object> mergeFields;
	private Map<String, Boolean> interest;
	private MemberStats stats;
	private String ipSignup;
	private ZonedDateTime timestampSignup;
	private String ipOpt;
	private ZonedDateTime timestampOpt;
	private Integer rating;
	private ZonedDateTime lastChanged;
	private String language;
	private boolean vip;
	private String emailClient;
	//private MemberLocation location;
	//private List<MemberMarketingPermissions> marketingPermissions;
	private LastNote lastNote;
	private Integer tagsCount;
	private List<MemberTag> tags;
	private String listId;
	
	private MemberStatus statusIfNew;
	private MailChimpConnection connection;

	public Member(MailChimpConnection connection, JSONObject member) {
		parse(connection, member);
	}

	public Member(Builder b) {
		emailAddress = b.emailAddress;
		id = Member.subscriberHash(emailAddress);
		emailType = b.emailType;
		status = b.status;
		mergeFields = b.mergeFields;
		interest = b.interest;
		ipSignup = b.ipSignup;
		timestampSignup = b.timestampSignup;
		ipOpt = b.ipOpt;
		timestampOpt = b.timestampOpt;
		language = b.language;
		vip = b.vip;
		//location = b.location;
		//marketing_permissions = b.marketingPermissions;
		//last_note = b.lastNote;
		tags = b.tags;
		listId = b.listId;
		statusIfNew = b.statusIfNew;
		connection = b.connection;
	}

	public Member() {

	}

//	public static Member newInstance(MailChimpConnection connection, JSONObject member) {
//		return new Member(connection, member);
//	}
	
	/**
	 * Parse a JSON representation of a member into this.
	 * @param connection
	 * @param member
	 */
	public void parse(MailChimpConnection connection, JSONObject member) {
		JSONObjectCheck jObj = new JSONObjectCheck(member);
		this.connection = connection;
        id = jObj.getString("id");
		emailAddress = jObj.getString("email_address");
		uniqueEmailId = jObj.getString("unique_email_id");
		emailType =  jObj.getEnum(EmailType.class, "email_type");
		status = jObj.getEnum(MemberStatus.class, "status");
		unsubscribeReason = jObj.getString("unsubscribe_reason");
		
		mergeFields = new HashMap<String, Object>();
		final JSONObject mergeFieldsObj = jObj.getJSONObject("merge_fields");
		if (mergeFieldsObj != null) {
			for(String key : mergeFieldsObj.keySet()) {
				mergeFields.put(key, mergeFieldsObj.get(key));
			}
		}
		
		interest = new HashMap<String, Boolean>();
		final JSONObject interests = jObj.getJSONObject("interests");
		if (interests != null) {
			for(String key : interests.keySet()) {
				interest.put(key, interests.getBoolean(key));
			}
		}
		
		if (jObj.has("stats")) {
			stats = new MemberStats(jObj.getJSONObject("stats"));
		}
		
		ipSignup = jObj.getString("ip_signup");
		timestampSignup = jObj.getISO8601Date("timestamp_signup");
		rating = jObj.getInt("member_rating");
		ipOpt = jObj.getString("ip_opt");
		timestampOpt = jObj.getISO8601Date("timestamp_opt");
		lastChanged = jObj.getISO8601Date("last_changed");
		language = jObj.getString("language");
		vip = jObj.getBoolean("vip");
		emailClient = jObj.getString("email_client");
		//location
		//marketing_permissions
		
		if (jObj.has("last_note")) {
			lastNote =  new LastNote(jObj.getJSONObject("last_note"));
		}

		tagsCount = jObj.getInt("tags_count");
		tags = new ArrayList<MemberTag>(tagsCount != null ? tagsCount.intValue() : 0);
		final JSONArray tagsArray = jObj.getJSONArray("tags");
		if (tagsArray != null) {
			for(int i = 0; i < tagsArray.length(); i++) {
				tags.add(new MemberTag(tagsArray.getJSONObject(i)));
			}
		}

		listId = jObj.getString("list_id");
	}

	/**
	 * Change this subscribers email address.
	 * @param emailAddress
	 * @throws IOException
	 * @throws Exception 
	 */
	public void changeEmailAddress(String emailAddress) throws IOException, Exception {
		Objects.requireNonNull(connection, "MailChimpConnection");
		JSONObject updateMember = new JSONObject();
		updateMember.put("email_address", emailAddress);
		String results = connection.do_Patch(new URL(connection.getListendpoint()+"/"+getListId()+"/members/"+getId()), updateMember.toString(), connection.getApikey());
		parse(connection, new JSONObject(results));  // update member object with current data
	}

	/**
	 * Change the status of the subscriber.
	 * @param status
	 * @throws IOException
	 * @throws Exception 
	 */
	public void changeStatus(MemberStatus status) throws IOException, Exception {
		Objects.requireNonNull(connection, "MailChimpConnection");
		JSONObject updateMember = new JSONObject();
		updateMember.put("status", status.toString());
		String results = connection.do_Patch(new URL(connection.getListendpoint()+"/"+ getListId()+"/members/"+getId()), updateMember.toString(), connection.getApikey());
		parse(connection, new JSONObject(results));  // update member object with current data
	}

	/**
	 * Add or update a list member via a PUT operation. When a new member is added
	 * and no status_if_new has been specified SUBSCRIBED will be used. Member
	 * fields will be freshened from mailchimp.
	 * @throws IOException
	 * @throws Exception 
	 * 
	 */
	public void addOrUpdate() throws IOException, Exception {
		Objects.requireNonNull(connection, "MailChimpConnection");
		JSONObject json = getJsonRepresentation();

		if (!json.has("status_if_new")) {
			json.put("status_if_new", MemberStatus.SUBSCRIBED.toString());
		}

		String results = connection.do_Put(
				new URL(connection.getListendpoint() + "/" + getListId() + "/members/" + getId()), json.toString(),
				connection.getApikey());
		parse(connection, new JSONObject(results)); // update member object with current data
	}
	
	/**
	 * Add or remove tags from this list member. If a tag that does not exist is passed in and set as ‘active’, a new tag will be created.
	 * 
	 * @param tagName The name of the tag.
	 * @param status The status for the tag on the member, pass in active to add a tag or inactive to remove it.
	 * @throws IOException
	 * @throws Exception 
	 */
	public void applyTag(String tagName, TagStatus status) throws IOException, Exception {
		Map<String, TagStatus> tagsMap = new HashMap<String, TagStatus>(1);
		tagsMap.put(tagName, status);
		applyTags(tagsMap);
	}
	
	/**
	 * Add or remove tags in bulk from this list member. If a tag that does not exist is passed in and set as ‘active’, a new tag will be created.
	 * @param tagsMap
	 * @throws IOException
	 * @throws Exception 
	 */
	public void applyTags(Map<String, TagStatus> tagsMap) throws IOException, Exception {
		Objects.requireNonNull(connection, "MailChimpConnection");
		JSONObject tagObj = new JSONObject();
		JSONArray tagsArray = new JSONArray();
		for(Entry<String, TagStatus> e : tagsMap.entrySet()) {
			tagsArray.put(new JSONObject()
					.put("name", e.getKey())
					.put("status", e.getValue().toString()));
			
			Optional<MemberTag> optional = tags.stream()
					.filter(t -> e.getKey().equals(t.getName()))
					.findFirst();
			
			if (optional.isPresent() && e.getValue() == TagStatus.INACTIVE) {
				tags.remove(optional.get());
			} else if (!optional.isPresent() && e.getValue() == TagStatus.ACTIVE) {
				tags.add(new MemberTag(e.getKey()));
			}
		}
		tagObj.put("tags",tagsArray);
		connection.do_Post(new URL(connection.getListendpoint()+"/"+getListId()+"/members/"+getId()+"/tags"), tagObj.toString(), connection.getApikey());
	}
	
	/**
	 * @param tagName A tag name to check for
	 * @return true if the member has the specified tag name.
	 */
	public boolean hasTag(String tagName) {
		Optional<MemberTag> optional = tags.stream()
				.filter(t -> tagName.equals(t.getName()))
				.findFirst();
		return optional.isPresent();
	}
	
	//
	// TODO: Events -- Use the Events endpoint to collect website or in-app actions and trigger targeted automations.
	//
	
	/**
	 * Get details about subscribers' recent activity.
	 * 
	 * @return The last 50 events of a member's activity, including opens, clicks, and unsubscribes.
	 * @throws IOException
	 * @throws Exception 
	 */
	public List<MemberActivity> getActivities() throws IOException, Exception {
		Objects.requireNonNull(connection, "MailChimpConnection");
		final JSONObject activity = new JSONObject(connection.do_Get(new URL(connection.getListendpoint()+"/"+getListId()+"/members/"+getId()+"/activity"), connection.getApikey()));
		//String email_id = activity.getString("email_id");
		//String list_id = activity.getString("list_id");
		//Integer total_items = activity.getInt("total_items");	// The total number of items matching the query regardless of pagination
		final JSONArray activityArray = activity.getJSONArray("activity");
		List<MemberActivity> activities = new ArrayList<MemberActivity>(activityArray.length());

		for (int i = 0 ; i < activityArray.length();i++)
		{
			activities.add(new MemberActivity(activityArray.getJSONObject(i)));
		}

		return activities;
	}

	//
	// TODO: Member Goals -- Get information about recent goal events.
	//
	
	//
	// Member Notes -- Manage recent notes for a list/audience member.
	//
	
	/**
	 * Get recent notes for this list/audience member.
	 * @param queryParameters Optional query parameters to send to the MailChimp API. 
	 *   @see <a href="https://mailchimp.com/developer/marketing/api/list-member-notes/list-recent-member-notes/" target="MailchimpAPIDoc">Lists/Audiences Member Notes -- GET /lists/{list_id}/members/{subscriber_hash}/notes</a>
	 * @throws IOException
	 * @throws Exception 
	 */
	public Iterable<MemberNote> getNotes(final MailChimpQueryParameters queryParameters) throws IOException, Exception {
		Objects.requireNonNull(connection, "MailChimpConnection");
		final String baseURL = URLHelper.join(connection.getListendpoint(), "/", getListId(), "/members/", getId(), "/notes");
		return new ModelIterator<MemberNote>(MemberNote.class, baseURL, connection, queryParameters);
	}
	
	/**
	 * Get recent notes for this list member.
	 * @throws IOException
	 * @throws Exception 
	 */
	public Iterable<MemberNote> getNotes() throws IOException, Exception {
		Objects.requireNonNull(connection, "MailChimpConnection");
		final String baseURL = URLHelper.join(connection.getListendpoint(), "/", getListId(), "/members/", getId(), "/notes");
		return new ModelIterator<MemberNote>(MemberNote.class, baseURL, connection);
	}
	
	/**
	 * Get a specific note for the member
	 * @param noteId The id for the note.
	 * @throws IOException
	 * @throws Exception 
	 */
	public MemberNote getNote(int noteId) throws IOException, Exception {
		Objects.requireNonNull(connection, "MailChimpConnection");
		final JSONObject noteObj = new JSONObject(connection.do_Get(new URL(connection.getListendpoint()+"/"+getListId()+"/members/"+getId()+"/notes/"+noteId), connection.getApikey()));
		return new MemberNote(noteObj);
	}
	
	/**
	 * Delete a note
	 * @param noteId The id for the note to delete.
	 * @throws IOException
	 * @throws Exception 
	 */
	public void deleteNote(int noteId) throws IOException, Exception {
		Objects.requireNonNull(connection, "MailChimpConnection");
		connection.do_Delete(new URL(connection.getListendpoint()+"/"+getListId()+"/members/"+getId()+"/notes/"+noteId), connection.getApikey());
	}
	
	/**
	 * Add a new note to this subscriber.
	 * @param note The content of the note. Note length is limited to 1,000 characters.
	 * @throws IOException
	 * @throws Exception 
	 */
	public MemberNote createNote(String note) throws IOException, Exception {
		Objects.requireNonNull(connection, "MailChimpConnection");
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("note", note);
		final JSONObject noteObj = new JSONObject(connection.do_Post(new URL(connection.getListendpoint()+"/"+getListId()+"/members/"+getId()+"/notes"), jsonObj.toString(), connection.getApikey()));
		return new MemberNote(noteObj);
	}
	
	/**
	 * Update a specific note for this list member.
	 * @param noteId The id for the note to update.
	 * @param note The new content for the note. Note length is limited to 1,000 characters.
	 * @throws IOException
	 * @throws Exception 
	 */
	public MemberNote updateNote(int noteId, String note) throws IOException, Exception {
		Objects.requireNonNull(connection, "MailChimpConnection");
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("note", note);
		jsonObj.put("list_id", getListId());
		jsonObj.put("email_id", getId());
		final JSONObject noteObj = new JSONObject(connection.do_Patch(new URL(connection.getListendpoint()+"/"+getListId()+"/members/"+getId()+"/notes/"+noteId), jsonObj.toString(), connection.getApikey()));
		return new MemberNote(noteObj);
	}

    /**
	 * @return The MD5 hash of the lowercase version of the list member’s email address.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Email address for this subscriber
	 */
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * Change this subscribers email address. You must call {@link #update()},
	 * {@link #addOrUpdate()},
	 * {@link MailChimpList#addOrUpdateMember(Member)}, or
	 * {@link MailChimpList#updateMember(Member)} for changes to take effect.
	 * 
	 * @param emailAddress The new Email address for this subscriber.
	 */
	public Member setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
		return this;
	}

	/**
	 * An identifier for the address across all of Mailchimp
	 */
	public String getUniqueEmailId() {
		return uniqueEmailId;
	}

	/**
	 * Type of email this member asked to get (‘html’ or ‘text’)
	 */
	public EmailType getEmailType() {
		return emailType;
	}

	/**
	 * Type of email this member asked to get (‘html’ or ‘text’). You must call
	 * {@link #update()}, {@link #addOrUpdate()},
	 * {@link MailChimpList#addOrUpdateMember(Member)}, or
	 * {@link MailChimpList#updateMember(Member)} for changes to take effect.
	 * 
	 * @param emailType
	 */
	public Member setEmailType(EmailType emailType) {
		this.emailType = emailType;
		return this;
	}

	/**
	 * Subscriber’s current status 
	 */
	public MemberStatus getStatus() {
		return status;
	}

	/**
	 * Subscriber’s current status. You must call {@link #update()},
	 * {@link #addOrUpdate()},
	 * {@link MailChimpList#addOrUpdateMember(Member)}, or
	 * {@link MailChimpList#updateMember(Member)} for changes to take effect.
	 */
	public Member setStatus(MemberStatus status) {
		this.status = status;
		return this;
	}

	/**
	 * @return A subscriber’s reason for unsubscribing.
	 */
	public String getUnsubscribeReason() {
		return unsubscribeReason;
	}

	/**
	 * Subscriber’s status. This value is required only when calling
	 * {@link MailChimpList#addOrUpdateMember(Member)} or {@link #update()}.
	 * 
	 * @return the status_if_new
	 */
	public MemberStatus getStatusIfNew() {
		return statusIfNew;
	}

	/**
	 * Set the status for a new member when created through a call to
	 * {@link MailChimpList#addOrUpdateMember(Member)} or {@link #addOrUpdate()}.
	 * 
	 * @param statusIfNew
	 */
	public Member setStatusIfNew(MemberStatus statusIfNew) {
		this.statusIfNew = statusIfNew;
		return this;
	}

	/**
	 * Audience merge tags that corresponds to the data in an audience field.
	 * @return a Map of all merge field name value pairs
	 */
	public Map<String, Object> getMergeFields() {
		return mergeFields;
	}

	/**
	 * Add or update an audience merge tags that corresponds to the data in an
	 * audience field. You must call {@link #update()},
	 * {@link #addOrUpdate()},
	 * {@link MailChimpList#addOrUpdateMember(Member)}, or
	 * {@link MailChimpList#updateMember(Member)} for changes to take effect.
	 * 
	 * @param key
	 * @param object
	 * @return the previous value associated with key, or null if there was none.
	 */
	public Object putMergeFields(String key, String object) {
		return mergeFields.put(key, object);
	}

	/**
	 * The members collection of interests. 
	 * @return the member interests. The map key is the interest/segment identifier and value is the subscription boolean.
	 */
	public Map<String, Boolean> getInterest() {
		return interest;
	}

	/**
	 * Add or update an interest. You must call {@link #update()},
	 * {@link #addOrUpdate()}, {@link MailChimpList#addOrUpdateMember(Member)}, or
	 * {@link MailChimpList#updateMember(Member)} for changes to take effect.
	 * 
	 * @param id     The interest id
	 * @param active
	 * @return The previous value associated with id, or null if there was none.
	 */
	public Boolean putInterest(String id, boolean active) {
		return interest.put(id, active);
	}
	
	/**
	 * Open and click rates for this subscriber.
	 */
	public MemberStats getStats() {
		return stats;
	}

	/**
	 * IP address the subscriber signed up from.
	 */
	public String getIpSignup() {
		return ipSignup;
	}

	/**
	 * IP address the subscriber signed up from. You must call {@link #update()},
	 * {@link #addOrUpdate()}, {@link MailChimpList#addOrUpdateMember(Member)}, or
	 * {@link MailChimpList#updateMember(Member)} for changes to take effect.
	 * 
	 * @param ipSignup the ipSignup to set
	 */
	public Member setIpSignup(String ipSignup) {
		this.ipSignup = ipSignup;
		return this;
	}

	/**
	 * The date and time the subscriber signed up for the list.
	 */
	public ZonedDateTime getTimestampSignup() {
		return timestampSignup;
	}

	/**
	 * The date and time the subscriber signed up for the list. You must call
	 * {@link #update()}, {@link #addOrUpdate()},
	 * {@link MailChimpList#addOrUpdateMember(Member)}, or
	 * {@link MailChimpList#updateMember(Member)} for changes to take effect.
	 * 
	 * @param timestampSignup the timestampSignup to set
	 */
	public Member setTimestampSignup(ZonedDateTime timestampSignup) {
		this.timestampSignup = timestampSignup;
		return this;
	}

	/**
	 * The IP address the subscriber used to confirm their opt-in status.
	 */
	public String getIpOpt() {
		return ipOpt;
	}

	/**
	 * The IP address the subscriber used to confirm their opt-in status. You must
	 * call {@link #update()}, {@link #addOrUpdate()},
	 * {@link MailChimpList#addOrUpdateMember(Member)}, or
	 * {@link MailChimpList#updateMember(Member)} for changes to take effect.
	 * 
	 * @param ipOpt the ipOpt to set
	 */
	public Member setIpOpt(String ipOpt) {
		this.ipOpt = ipOpt;
		return this;
	}

	/**
	 * The date and time the subscribe confirmed their opt-in status.
	 */
	public ZonedDateTime getTimestampOpt() {
		return timestampOpt;
	}

	/**
	 * The date and time the subscribe confirmed their opt-in status. You must call
	 * {@link #update()}, {@link #addOrUpdate()},
	 * {@link MailChimpList#addOrUpdateMember(Member)}, or
	 * {@link MailChimpList#updateMember(Member)} for changes to take effect.
	 * 
	 * @param timestampOpt the timestampOpt to set
	 */
	public Member setTimestampOpt(ZonedDateTime timestampOpt) {
		this.timestampOpt = timestampOpt;
		return this;
	}

	/**
	 * Star rating for this member, between 1 and 5
	 */
	public Integer getRating() {
		return rating;
	}

	/**
	 * @return The date and time the member’s info was last changed
	 */
	public ZonedDateTime getLastChanged() {
		return lastChanged;
	}

	/**
	 * If set/detected, the subscriber’s language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * set/change the subscriber’s language. You must call {@link #update()},
	 * {@link #addOrUpdate()}, {@link MailChimpList#addOrUpdateMember(Member)}, or
	 * {@link MailChimpList#updateMember(Member)} for changes to take effect.
	 * 
	 * @param language the language to set
	 */
	public Member setLanguage(String language) {
		this.language = language;
		return this;
	}

	/**
	 * VIP status for subscriber
	 */
	public boolean isVip() {
		return vip;
	}

	/**
	 * Set VIP status for subscriber. You must call {@link #update()},
	 * {@link #addOrUpdate()}, {@link MailChimpList#addOrUpdateMember(Member)}, or
	 * {@link MailChimpList#updateMember(Member)} for changes to take effect.
	 * 
	 * @param vip the vip to set
	 */
	public Member setVip(boolean vip) {
		this.vip = vip;
		return this;
	}

	/**
	 * The list member’s email client
	 */
	public String getEmailClient() {
		return emailClient;
	}

	/**
	 * @return The most recent Note added about this member.
	 */
	public LastNote getLastNote() {
		return lastNote;
	}

	/**
	 * The number of tags applied to this member
	 */
	public Integer getTagsCount() {
		return tagsCount;
	}

	/**
	 * @return Returns up to 50 tags applied to this member. To retrieve all tags
	 *         see {@link #getAllTags()} or
	 *         {@link com.github.bananaj.model.list.MailChimpList#getMemberTags(String)}.
	 */
	public List<MemberTag> getTags() {
		return tags;
	}

	/**
	 * @return Returns an iterator for all tags applied to this member.
	 * @throws IOException
	 * @throws Exception 
	 */
	public Iterable<MemberTag> getAllTags() throws IOException, Exception {
		if (tags != null && tags.size() < 50) {
			return tags;	// already have full list so simply return it
		}
		
		final String baseURL = URLHelper.join(connection.getListendpoint(),"/",getListId(),"/members/", getId(), "/tags");
		return new ModelIterator<MemberTag>(MemberTag.class, baseURL, connection);
	}
	
	/**
	 * Get the tags for this list member.
	 * @param queryParameters Optional query parameters to send to the MailChimp API. 
	 *   @see <a href="https://mailchimp.com/developer/marketing/api/list-member-tags/list-member-tags/" target="MailchimpAPIDoc">Lists/Audiences Member Tags -- GET /lists/{list_id}/members/{subscriber_hash}/tags</a>
	 * @throws IOException
	 * @throws Exception 
	 */
	public Iterable<MemberTag> getTags(final MailChimpQueryParameters queryParameters) throws IOException, Exception {
		final String baseURL = URLHelper.join(connection.getListendpoint(),"/",getListId(),"/members/", getId(), "/tags");
		return new ModelIterator<MemberTag>(MemberTag.class, baseURL, connection, queryParameters);
	}

	/**
	 * The list id
	 */
	public String getListId() {
		return listId;
	}

	/**
	 * Add/Update an interests subscription
	 * @param key
	 * @param subscribe
	 * @return the previous value associated with key, or null if there was none.)
	 */
	public Boolean putInterest(String key, Boolean subscribe) {
		return interest.put(key, subscribe);
	}

	/**
	 * @return the MailChimp com.github.bananaj.connection
	 */
	public MailChimpConnection getConnection() {
		return connection;
	}

	/**
	 * Helper method to convert JSON for mailchimp PATCH/POST operations
	 */
	public JSONObject getJsonRepresentation() {
		JSONObject json = new JSONObject();
		json.put("email_address", getEmailAddress());
		
		if (getStatusIfNew() != null) {
			// used by PUT 'Add or update a list member'
			json.put("status_if_new", getStatusIfNew().toString());
		}
		
		if (getEmailType() != null) {
			json.put("email_type", getEmailType().toString());
		}
		if (getStatus() != null) {
			json.put( "status", getStatus().toString());
		}

		{
			JSONObject mergeFields = new JSONObject();
			Map<String, Object> mergeFieldsMap = getMergeFields();
			for (String key : mergeFieldsMap.keySet()) {
				mergeFields.put(key, mergeFieldsMap.get(key));
			}
			json.put("merge_fields", mergeFields);
		}

		{
			JSONObject interests = new JSONObject();
			Map<String, Boolean> interestsMap = getInterest();
			for (String key : interestsMap.keySet()) {
				interests.put(key, interestsMap.get(key));
			}
			json.put("interests",interests);
		}

		json.put("language", getLanguage());
		json.put("vip", isVip());
		
		// location
		// marketing_permissions

		if (ipSignup != null && ipSignup.length() > 0) {
			json.put("ip_signup", getIpSignup());
		}

		if (getTimestampSignup() != null) {
			json.put("timestamp_signup", DateConverter.toISO8601UTC(getTimestampSignup()));
		}
		
		if (ipOpt != null && ipOpt.length() > 0) {
			json.put( "ip_opt", getIpOpt());
		}
		
		if (getTimestampOpt() != null) {
			json.put("timestamp_opt", DateConverter.toISO8601UTC(getTimestampOpt()));
		}
		
		// tags used by POST 'Add a new list member'
		if (tags != null && tags.size() > 0 ) {
			JSONArray tagsArray = new JSONArray();
			for(MemberTag t: tags) {
				tagsArray.put(t.getName());
			}
			json.put("tags", tagsArray);
		}

		return json;
	}

	/**
	 * Update subscriber via a PATCH operation. Member fields will be freshened
	 * from MailChimp.
	 * @throws IOException
	 * @throws Exception 
	 */
	public void update() throws IOException, Exception {
		Objects.requireNonNull(connection, "MailChimpConnection");
		JSONObject json = getJsonRepresentation();

		String results = connection.do_Patch(
				new URL(connection.getListendpoint() + "/" + getListId() + "/members/" + getId()), json.toString(),
				connection.getApikey());
		parse(connection, new JSONObject(results)); // update member object with current data
	}
	
	/**
	 * Remove this list member
	 * @throws IOException
	 * @throws Exception 
	 */
	public void delete() throws IOException, Exception {
		Objects.requireNonNull(connection, "MailChimpConnection");
		connection.do_Delete(new URL(connection.getListendpoint()+"/"+getListId()+"/members/"+getId()), connection.getApikey());
	}
	
	/**
	 * Permanently delete this list member
	 * @throws IOException
	 * @throws Exception 
	 */
	public void deletePermanent() throws IOException, Exception {
		Objects.requireNonNull(connection, "MailChimpConnection");
		connection.do_Post(new URL(connection.getListendpoint()+"/"+getListId()+"/members/"+getId()+"/actions/delete-permanent"), connection.getApikey());
	}
	
	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("    Merge Fields:").append(System.lineSeparator());
		for (Entry<String, Object> pair : getMergeFields().entrySet()) {
			stringBuilder.append("        ").append(pair.getKey()).append(": ").append(pair.getValue()).append(System.lineSeparator());
		}
		if (tags != null && tags.size() > 0) {
			for (MemberTag tagObj : tags) {
				stringBuilder.append("    ").append(tagObj.toString()).append(System.lineSeparator());
			}
		}
		if (interest != null && interest.size() > 0) {
			stringBuilder.append("    Interests:").append(System.lineSeparator());
			for (Entry<String, Boolean> pair : interest.entrySet()) {
				stringBuilder.append("        ").append(pair.getKey()).append(":").append(pair.getValue().toString()).append(System.lineSeparator());
			}
		}

		return 
				"Member:" + System.lineSeparator() +
				"    Id: " + getId() + System.lineSeparator() +
				"    Email: " + getEmailAddress() + System.lineSeparator() +
				"    Email Id: " + getUniqueEmailId() + System.lineSeparator() +
				(getEmailType() != null ? "    Email Type: " + getEmailType().toString() + System.lineSeparator() : "") +
				(getStatus() != null ? "    Status: " + getStatus().toString() + System.lineSeparator() : "") +
				"    List Id: " + getListId() + System.lineSeparator() +
				"    Signup Timestamp: " + DateConverter.toLocalString(getTimestampSignup()) + System.lineSeparator() +
				"    Signup IP: " + getIpSignup() + System.lineSeparator() +
				"    Opt-in IP: " + getIpOpt() + System.lineSeparator() +
				"    Opt-in Timestamp: " + DateConverter.toLocalString(getTimestampOpt()) + System.lineSeparator() +
				"    Member Rating: " + getRating() + System.lineSeparator() +
				"    Last Changed: " + DateConverter.toLocalString(getLastChanged()) + System.lineSeparator() +
				"    Language: " + getLanguage() + System.lineSeparator() +
				"    VIP: " + isVip() + System.lineSeparator() +
				(getEmailClient() != null ? "    Email Client: " + getEmailClient() + System.lineSeparator() : "") +
				(getLastNote() != null ? getLastNote().toString() + System.lineSeparator() : "") +
				//getStats().toString() + System.lineSeparator() +
				stringBuilder.toString();
	}

	/**
	 * Convert an email address to a Mailchimp subscriber hash. Member uses an MD5
	 * hash of the lowercase email address as an identifier. This will generate
	 * mailchimp subscriber hash from email address. If a subscriber hash is
	 * provided it will be returned unaltered.
	 * 
	 * @param emailAddress An email address or Mailchimp subscriber hash
	 * @return The MD5 hash of the lowercase version of the email address.
	 */
	public static String subscriberHash(String emailAddress) {
		return EmailValidator.getInstance().validate(emailAddress) ? 
				MD5.getMD5(emailAddress.toLowerCase()) : 
					emailAddress;
	}

	public static class Builder {
		private String listId;
		private String emailAddress;
		private EmailType emailType;
		private MemberStatus status;
		private Map<String, Object> mergeFields = new HashMap<String, Object>();
		private Map<String, Boolean> interest = new HashMap<String, Boolean>();
		private String language;
		private boolean vip;
		//private MemberLocation location;
		//private List<MemberMarketingPermissions> marketing_permissions;
		private String ipSignup;
		private ZonedDateTime timestampSignup;
		private String ipOpt;
		private ZonedDateTime timestampOpt;
		private List<MemberTag> tags = new ArrayList<MemberTag>();
		private MemberStatus statusIfNew;
		private MailChimpConnection connection;

		public Member build() {
			Objects.requireNonNull(listId, "list_id");
			Objects.requireNonNull(emailAddress, "email_address");
			Objects.requireNonNull(status, "status");
			return new Member(this);
		}

		/**
		 * @param connection the connection to set
		 */
		public Builder connection(MailChimpConnection connection) {
			this.connection = connection;
			return this;
		}

		public Builder listId(String listId) {
			this.listId = listId;
			return this;
		}

		public Builder emailAddress(String emailAddress) {
			this.emailAddress = emailAddress;
			return this;
		}

		public Builder emailType(EmailType emailType) {
			this.emailType = emailType;
			return this;
		}

		public Builder status(MemberStatus status) {
			this.status = status;
			return this;
		}

		public Builder mergeFields(Map<String, Object> mergeFields) {
			this.mergeFields = mergeFields;
			return this;
		}
		
		public Builder mergeField(String key, Object value) {
			if (mergeFields == null) {
				mergeFields = new HashMap<String, Object>();
			}
			mergeFields.put(key, value);
			return this;
		}

		/**
		 * Adds a merge field var and value.
		 * 
		 * @param var
		 * @param value
		 */
		public Builder withMergeField(String var, String value) {
			mergeFields.put(var, value);
			return this;
		}

		public Builder memberInterest(Map<String, Boolean> memberInterest) {
			this.interest = memberInterest;
			return this;
		}

		public Builder withInterest(String interestName, boolean active) {
			interest.put(interestName, active);
			return this;
		}

		public Builder ipSignup(String ipSignup) {
			this.ipSignup = ipSignup;
			return this;
		}

		public Builder timestampSignup(ZonedDateTime timestampSignup) {
			this.timestampSignup = timestampSignup;
			return this;
		}

		public Builder ipOpt(String ipOpt) {
			this.ipOpt = ipOpt;
			return this;
		}

		public Builder timestampOpt(ZonedDateTime timestampOpt) {
			this.timestampOpt = timestampOpt;
			return this;
		}

		public Builder language(String language) {
			this.language = language;
			return this;
		}

		public Builder vip(boolean vip) {
			this.vip = vip;
			return this;
		}

		/**
		 * Add a tags to this member in preparation for creating a mailchimp list
	     * member. See: {@link MailChimpList#addMember(Member)}
	     * 
		 * @param tags
		 */
		public Builder tags(List<MemberTag> tags) {
			this.tags = tags;
			return this;
		}
		
		/**
		 * Add tag(s) to this member in preparation for creating a mailchimp list
	     * member. See: {@link MailChimpList#addMember(Member)}
		 * 
		 * @param tagName
		 */
		public Builder withTag(String tagName) {
			Optional<MemberTag> optional = tags.stream()
					.filter(t -> tagName.equals(t.getName()))
					.findFirst();
			if (!optional.isPresent()) {
				tags.add(new MemberTag(tagName));
			}
			return this;
		}

		public Builder statusIfNew(MemberStatus statusIfNew) {
			this.statusIfNew = statusIfNew;
			return this;
		}
	}
}
