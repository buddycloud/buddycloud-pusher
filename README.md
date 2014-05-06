buddycloud-pusher
=================

Aim
---

-   Getting users back to buddycloud using email and push notifications
-   "Someone commented on your photo"
-   "Someone commented in a thread you commented in"

When a user registers
---------------------

1.  The HTTP API sends the email address to the buddycloud pusher
2.  The pusher creates a notification profile with default preferences
3.  The pusher creates an unique string to do one-click unsubscribe
    “click here to un-subuscribe”

Emails to send
--------------

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

Notes
-----

-   no email validation step - wrong email just goes to the wrong person
    and they hit unsubscribe / user can’t recover password
-   No unsubscribe link

Database schema design
----------------------

<https://github.com/buddycloud/buddycloud-pusher/blob/master/resources/schema/create-schema.sql>

Should we store time for last sent event in order to avoid oversending?

Component design
----------------

-   New component: pusher.example.com
-   Receives triggers from buddycloud channel server
-   Only reacts on triggers from buddycloud server (avoid strange uses)
-   Stores emails addresses during users signup
-   Stores preferences regarding email notification

![](design%20docs/Pusher-sequence.png "Sequence diagram")

Stanzas
-------

### Add or configure push records

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

### Remove push record

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

### Get push settings

#### Get all push settings for a given type

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

#### Get push settings for a given type and target

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

### User signed up listener

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


Email Templates
---------------

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

Useful links
------------

-   <https://github.com/seanpowell/Email-Boilerplate>
-   <http://www.mkyong.com/java/javamail-api-sending-email-via-gmail-smtp-example/>
-   Reference regarding unsubscribing:
    <http://usabilityhell.com/post/23041365039/email-unsubscribe-fails-top-10>


