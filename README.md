# buddycloud-pusher

## Aim
-   Getting users back to buddycloud using email and push notifications
-   "Someone commented on your photo"
-   "Someone commented in a thread you commented in"

## Database schema design

<https://github.com/buddycloud/buddycloud-pusher/blob/master/resources/schema/create-schema.sql>

Should we store time for last sent event in order to avoid oversending?

## Component design

-   New component: pusher.example.com
-   Receives triggers from buddycloud channel server
-   Only reacts on triggers from buddycloud server (avoid strange uses)
-   Stores emails addresses during users signup
-   Stores preferences regarding email notification

![](design%20docs/Pusher-sequence.png "Sequence diagram")

### Interaction with the Buddycloud server

The Buddycloud server sends standard XEP-0060 notification messages to the pusher, as in http://www.xmpp.org/extensions/xep-0060.html#intro-howitworks.

### Creating handlers for new notification types

The Pusher basic design relies on [Message Consumers](https://github.com/buddycloud/buddycloud-pusher/blob/master/src/main/java/org/buddycloud/pusher/message/MessageConsumer.java) and on [Query Handlers](https://github.com/buddycloud/buddycloud-pusher/blob/master/src/main/java/org/buddycloud/pusher/handler/QueryHandler.java).

A **Message Consumer** must figure out what notifications to trigger based on a message arriving from the Buddycloud server, and then create an IQ to be sent back to the pusher itself.

For instance, the [UserPostedMentionConsumer](https://github.com/buddycloud/buddycloud-pusher/blob/master/src/main/java/org/buddycloud/pusher/message/UserPostedMentionConsumer.java) applies a regular expression to the message content in order to figure out whether there is a mention in such a message, creates an IQ with the notification and sends it back to the pusher via loopback:

``` java
public void consume(Message message, List<String> recipients) {
		
	Element eventEl = message.getChildElement("event", ConsumerUtils.PUBSUB_EVENT_NS);
	Element itemsEl = eventEl.element("items");
	AtomEntry entry = AtomEntry.parse(itemsEl);
	if (entry == null) {
		return;
	}
	
	Matcher matcher = JID_REGEX.matcher(entry.getContent());
	while (matcher.find()) {
		String mentionedJid = matcher.group();
		newMention(mentionedJid, entry, recipients);
	}
}
	
private void newMention(String mentionedJid, AtomEntry entry, List<String> recipients) {
	if (mentionedJid.equals(entry.getAuthor()) || recipients.contains(mentionedJid)) {
		return;
	}
	
	IQ iq = createIq(entry.getAuthor(), mentionedJid, 
			entry.getNode(), entry.getContent());
	try {
		IQ iqResponse = getXmppComponent().handleIQLoopback(iq);
		if (iqResponse.getError() == null) {
			recipients.add(mentionedJid);
		}
	} catch (Exception e) {
		LOGGER.warn(e);
	}
}

private IQ createIq(String authorJid, String mentionedJid, 
		String channelJid, String content) {
	IQ iq = new IQ();
	Element queryEl = iq.getElement().addElement("query", 
			"http://buddycloud.com/pusher/userposted-mention");
	queryEl.addElement("authorJid").setText(authorJid);
	queryEl.addElement("mentionedJid").setText(mentionedJid);
	queryEl.addElement("channel").setText(ConsumerUtils.getChannelAddress(channelJid));
	queryEl.addElement("postContent").setText(content);
    return iq;
}	
```

A **Query Handler**, on its turn, will process the IQ sent by the Message Consumer based on its namespace. It will decide whether the recipient should receive this notification, create a data dictionary to be sent and then call all the push strategies (email, GCM, ...) passing this data dictionary.

The [UserPostedMentionQueryHandler](https://github.com/buddycloud/buddycloud-pusher/blob/master/src/main/java/org/buddycloud/pusher/handler/internal/UserPostedMentionQueryHandler.java), for instance, reads the IQ sent by the UserPostedMentionConsumer, creates the data dictionary and then decide if the recipient should receive this notification based on its notification settings:

``` java
Map<String, String> tokens = new HashMap<String, String>();
|tokens.put("AUTHOR_JID", authorJid);
tokens.put("MENTIONED_JID", mentionedJid);
tokens.put("CHANNEL_JID", channelJid);
tokens.put("CONTENT", postContent);

List<NotificationSettings> allNotificationSettings = NotificationUtils.getNotificationSettings(
		mentionedJid, getDataSource());

for (NotificationSettings notificationSettings : allNotificationSettings) {
	if (!notificationSettings.getPostMentionedMe()) {
		getLogger().warn("User " + mentionedJid + " won't receive mention notifications.");
		continue;
	}
			
	if (notificationSettings.getTarget() == null) {
		getLogger().warn("User " + mentionedJid + " has no target registered.");
		continue;
	}

	Pusher pusher = Pushers.getInstance(getProperties()).get(notificationSettings.getType());
	pusher.push(notificationSettings.getTarget(), Event.MENTION, tokens);
}
```

New **Query Handlers should be registered** in the [XMPPComponent.initHandlers] (https://github.com/buddycloud/buddycloud-pusher/blob/master/src/main/java/org/buddycloud/pusher/XMPPComponent.java#L83) method, while new **Message Consumers should be added** to the [MessageProcessor.initConsumers](https://github.com/buddycloud/buddycloud-pusher/blob/master/src/main/java/org/buddycloud/pusher/message/MessageProcessor.java#L21) method.

## Build from source

```shell
git clone https://github.com/buddycloud/buddycloud-pusher.git
cd buddycloud-pusher
cp configuration.properties.example configuration.properties
mvn package
java -jar target/pusher-0.1.0-jar-with-dependencies.jar
```

## GCM Pusher

### Configure the Pusher service
Create a GCM project and get an API key as per http://developer.android.com/google/gcm/gs.html#create-proj
Change GCM settings in the pusher's configuration.properties

```
# GCM project id 
gcm.google_project_id=
# GCM API key 
gcm.api_key=
```

### Discover project id
The first thing your app should do is to find out which GCM project to subscribe to and then subscribe to its notifications. Your app must send a metadata stanza to the pusher, as following:

~~~~ {.xml}
<!-- Only the fields to be modified need to be included here -->
<iq from='user@buddycloud.example.com' 
    to='pusher.example.com' 
    id='023FE3AA4'
    type='get'>
    <query xmlns='http://buddycloud.com/pusher/metadata'>
      <type>gcm</type>
    </query>
</iq>
~~~~

~~~~ {.xml}
<!-- Only the fields to be modified need to be included here -->
<iq from='pusher.example.com' 
    to='user@buddycloud.example.com' 
    id='023FE3AA4'
    type='result'>
    <query xmlns='http://buddycloud.com/pusher/metadata'>
      <google_project_id>123456789</google_project_id>
    </query>
</iq>
~~~~

### Register for GCM

As in http://developer.android.com/google/gcm/client.html#sample-register

### Register for Pusher notifications

With your registration id, you should register for Pusher notifications and configure what events you want to be notified of.

~~~~ {.xml}
<!-- Only the fields to be modified need to be included here -->
<iq from='user@buddycloud.example.com' 
    to='pusher.example.com' 
    id='023FE3AA4'
    type='set'>
    <query xmlns='http://buddycloud.com/pusher/notification-settings'>
      <notificationSettings>
         <type>gcm</type>
         <target>$REGISTRATION_ID</target>
         <postAfterMe>false</postAfterMe>
         <postMentionedMe>false</postMentionedMe>
      </notificationSettings>
    </query>
</iq>
~~~~

~~~~ {.xml}
<iq from='pusher.example.com' 
    id='023FE3AA4' 
    to='user@buddycloud.example.com'
    type='result'>
    <query xmlns='http://buddycloud.com/pusher/notification-settings'>
      <notificationSettings>
         <type>gcm</type>
         <target>$REGISTRATION_ID</target>
         <postAfterMe>false</postAfterMe>
         <postMentionedMe>false</postMentionedMe>
         <postOnMyChannel>true</postOnMyChannel>
         <postOnSubscribedChannel>false</postOnSubscribedChannel>
         <followMyChannel>true</followMyChannel>
         <followRequest>true</followRequest>
      </notificationSettings>
    </query>
</iq>
~~~~

### Receive Pusher notifications

As in http://developer.android.com/google/gcm/client.html#sample-receive, you need a GCMBroadcastReceiver and GCMIntentService to listen to GCM. Take a look at Buddycloud's [GCMIntentService](https://github.com/buddycloud/buddycloud-android/blob/master/src/com/buddycloud/GCMIntentService.java) for an example on how to handle those.

## E-mail pusher

### When a user registers

1.  The HTTP API sends the email address to the buddycloud pusher
2.  The pusher creates a notification profile with default preferences
3.  The pusher creates an unique string to do one-click unsubscribe
    “click here to un-subuscribe”

### Emails to send

send all emails from the friendly "please-reply@example.com"

-   0h: \# welcome email explaining buddycloud and giving them a link
    directly back to their channel.
-   every 24hours: activity report "posts in the channels you follow" +
    random channel post of the day.
-   other emails (triggered by events passed from buddycloud-server to
    pusher.example.com component:
    -   someone follows
    -   someone posts into their channel (plus holdoff for 24 hours)
    -   someone followed your channel (plus holdoff for 24 hours)
    -   someone comments on your post
    -   someone mentions you
    -   when they are invited to a channel, send an email

-   bonus: after two days "you can now create a topic channel" cool!

### Notes

-   no email validation step - wrong email just goes to the wrong person
    and they hit unsubscribe / user can’t recover password
-   No unsubscribe link

### Stanzas

#### Add or configure push records

HTTP API to pusher

~~~~ {.xml}
<!-- Only the fields to be modified need to be included here -->
<iq from='newuser@buddycloud.example.com' 
    to='pusher.example.com' 
    id='023FE3AA4'
    type='set'>
    <query xmlns='http://buddycloud.com/pusher/notification-settings'>
      <notificationSettings>
         <type>email</type>
         <target>newemail@newuser.com</target>
         <postAfterMe>false</postAfterMe>
         <postMentionedMe>false</postMentionedMe>
      </notificationSettings>
    </query>
</iq>
~~~~

pusher to HTTP API

~~~~ {.xml}
<iq from='pusher.example.com' 
    id='023FE3AA4' 
    to='newuser@buddycloud.example.com'
    type='result'>
    <query xmlns='http://buddycloud.com/pusher/notification-settings'>
      <notificationSettings>
         <type>email</type>
         <target>newemail@newuser.com</target>
         <postAfterMe>false</postAfterMe>
         <postMentionedMe>false</postMentionedMe>
         <postOnMyChannel>true</postOnMyChannel>
         <postOnSubscribedChannel>false</postOnSubscribedChannel>
         <followMyChannel>true</followMyChannel>
         <followRequest>true</followRequest>
      </notificationSettings>
    </query>
</iq>
~~~~

#### Remove push record

Removes all records associated to this jid.

HTTP API to pusher

~~~~ {.xml}
<iq from='newuser@buddycloud.example.com' 
    to='pusher.example.com' 
    id='023FE3AA4'
    type='set'>
    <query xmlns='jabber:iq:register'>
      <remove/>
    </query>
</iq>
~~~~

pusher to HTTP API

~~~~ {.xml}
<iq from='pusher.example.com' 
    id='023FE3AA4' 
    to='newuser@buddycloud.example.com'
    type='result'> 
    <query xmlns='jabber:iq:register'/>
</iq>
~~~~

The same stanza can be used to remove specific records
Both type and target fields are optional.

HTTP API to pusher

~~~~ {.xml}
<iq from='newuser@buddycloud.example.com' 
    to='pusher.example.com' 
    id='023FE3AA4'
    type='set'>
    <query xmlns='jabber:iq:register'>
      <remove>
        <type>email</type>
        <target>newuser@email.com</target>
      </remove>
    </query>
</iq>
~~~~

pusher to HTTP API

~~~~ {.xml}
<iq from='pusher.example.com' 
    id='023FE3AA4' 
    to='newuser@buddycloud.example.com'
    type='result'> 
    <query xmlns='jabber:iq:register'/>
</iq>
~~~~

#### Get push settings

##### Get all push settings for a given type

HTTP API to pusher

~~~~ {.xml}
<iq from='newuser@buddycloud.example.com' 
    to='pusher.example.com' 
    id='023FE3AA4'
    type='get'>
    <query xmlns='http://buddycloud.com/pusher/notification-settings'>
      <type>email</type>
    </query>
</iq>
~~~~

pusher to HTTP API

~~~~ {.xml}
<iq from='pusher.example.com' 
    id='023FE3AA4' 
    to='newuser@buddycloud.example.com'
    type='result'>
    <query xmlns='http://buddycloud.com/pusher/notification-settings'>"
      <notificationSettings>
         <type>email</type>
         <target>email@newuser.com</target>
         <postAfterMe>true</postAfterMe>
         <postMentionedMe>true</postMentionedMe>
         <postOnMyChannel>true</postOnMyChannel>
         <postOnSubscribedChannel>false</postOnSubscribedChannel>
         <followMyChannel>true</followMyChannel>
         <followRequest>true</followRequest>
      </notificationSettings>
      <notificationSettings>
         <type>email</type>
         <target>email@domain.com</target>
         <postAfterMe>true</postAfterMe>
         <postMentionedMe>true</postMentionedMe>
         <postOnMyChannel>true</postOnMyChannel>
         <postOnSubscribedChannel>false</postOnSubscribedChannel>
         <followMyChannel>false</followMyChannel>
         <followRequest>false</followRequest>
      </notificationSettings>
    </query>
</iq>
~~~~

##### Get push settings for a given type and target

HTTP API to pusher

~~~~ {.xml}
<iq from='newuser@buddycloud.example.com' 
    to='pusher.example.com' 
    id='023FE3AA4'
    type='get'>
    <query xmlns='http://buddycloud.com/pusher/notification-settings'>
      <type>email</type>
      <target>email@newuser.com</target>
    </query>
</iq>
~~~~

pusher to HTTP API

~~~~ {.xml}
<iq from='pusher.example.com' 
    id='023FE3AA4' 
    to='newuser@buddycloud.example.com'
    type='result'>
    <query xmlns='http://buddycloud.com/pusher/notification-settings'>"
      <notificationSettings>
         <type>email</type>
         <target>email@newuser.com</target>
         <postAfterMe>true</postAfterMe>
         <postMentionedMe>true</postMentionedMe>
         <postOnMyChannel>true</postOnMyChannel>
         <postOnSubscribedChannel>false</postOnSubscribedChannel>
         <followMyChannel>true</followMyChannel>
         <followRequest>true</followRequest>
      </notificationSettings>
    </query>
</iq>
~~~~

#### User signed up listener

HTTP API to pusher

~~~~ {.xml}
<iq from='newuser@buddycloud.example.com' 
    to='pusher.example.com' 
    id='023FE3AA4'
    type='set'>
    <query xmlns='http://buddycloud.com/pusher/signup'>
        <email>email@newuser.com</email>
    </query>
</iq>
~~~~

pusher to HTTP API

~~~~ {.xml}
<iq from='pusher.example.com' 
    id='023FE3AA4' 
    to='newuser@buddycloud.example.com'
    type='result'> 
    <query xmlns='http://buddycloud.com/pusher/signup'>
        <info>User [newuser@buddycloud.example.com] signed up.</info>
    </query>
</iq>
~~~~


### Email Templates

<https://github.com/buddycloud/buddycloud-pusher/tree/master/templates/email>

Email templates (tpl files) use the following structure

~~~~ {.html5}
SENDER_EMAIL_ADDRESS
RECIPIENT_NAME <RECIPIENT_EMAIL_ADDRESS>
SUBJECT
HTML CONTENT (From the 4th line on)
~~~~

Tokens use the \<%TOKEN\_NAME%\> format, and are replaced by their
values during runtime. Token values can be defined in the
configuration.properties file, using the mail.template. prefix.

### Useful links

-   <https://github.com/seanpowell/Email-Boilerplate>
-   <http://www.mkyong.com/java/javamail-api-sending-email-via-gmail-smtp-example/>
-   Reference regarding unsubscribing:
    <http://usabilityhell.com/post/23041365039/email-unsubscribe-fails-top-10>


