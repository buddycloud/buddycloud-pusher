buddycloud-pusher
=================

Aim
---

-   Getting users back to buddycloud using email and push notifications
-   Helping users find existing friends using a token (email, jabberID,
    phone number, twitterID etc)
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

project steps
-------------

1.  design email text
    1.  ~~welcome email - ST~~
    2.  daily activity email
    3.  ~~follow your channel -ST~~
    4.  ~~post in your channel email - ST~~
    5.  invitation to channel email

2.  ~~schema design -ST~~
3.  ~~adaptation to buddycloud-server to add event triggers that talk to
    the pusher.example.com -ST~~
4.  ~~adapt webclient to push email address to server at reg time -ST~~
5.  design auto unsubscribe page
6.  decide on API links <https://api.example.com/mail/unsubscribe>
7.  ~~add http API support -ST~~

Notes
-----

-   no email validation step - wrong email just goes to the wrong person
    and they hit unsubscribe / user can’t recover password

Unsolved
--------

-   No mobile push: no means to address [Apple's Push
    Service](http://developer.apple.com/library/mac/#documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/ApplePushService/ApplePushService.html)
    and [Google's Cloud Messaging for
    Android](http://developer.android.com/guide/google/gcm/index.html)
-   Daily activity report batch
-   Unsubscribe link

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

![](Pusher-sequence.png "Pusher-sequence.png")

Stanzas
-------

### Signup stanza

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

### Leave buddycloud stanza

HTTP API to pusher

~~~~ {.xml}
<iq from='newuser@buddycloud.example.com' 
    to='pusher.example.com' 
    id='023FE3AA4'
    type='set'>
    <query xmlns='http://buddycloud.com/pusher/deleteuser'/>
</iq>
~~~~

pusher to HTTP API

~~~~ {.xml}
<iq from='pusher.example.com' 
    id='023FE3AA4' 
    to='newuser@buddycloud.example.com'
    type='result'> 
    <query xmlns='http://buddycloud.com/pusher/deleteuser'>
        <info>User [newuser@buddycloud.example.com] was deleted.</info>
    </query>
</iq>
~~~~

### Get notification settings stanza

HTTP API to pusher

~~~~ {.xml}
<iq from='newuser@buddycloud.example.com' 
    to='pusher.example.com' 
    id='023FE3AA4'
    type='get'>
    <query xmlns='http://buddycloud.com/pusher/notification-settings'/>
</iq>
~~~~

pusher to HTTP API

~~~~ {.xml}
<iq from='pusher.example.com' 
    id='023FE3AA4' 
    to='newuser@buddycloud.example.com'
    type='result'>
    <query xmlns='http://buddycloud.com/pusher/notification-settings'>
         <email>email@newuser.com</email>
         <postAfterMe>true</postAfterMe>
         <postMentionedMe>true</postMentionedMe>
         <postOnMyChannel>true</postOnMyChannel>
         <postOnSubscribedChannel>false</postOnSubscribedChannel>
         <followMyChannel>true</followMyChannel>
         <followRequest>true</followRequest>
    </query>
</iq>
~~~~

### Set notification settings stanza

HTTP API to pusher

~~~~ {.xml}
<!-- Only the fields to be modified need to be included here -->
<iq from='newuser@buddycloud.example.com' 
    to='pusher.example.com' 
    id='023FE3AA4'
    type='set'>
    <query xmlns='http://buddycloud.com/pusher/notification-settings'>
         <email>newemail@newuser.com</email>
         <postAfterMe>false</postAfterMe>
         <postMentionedMe>false</postMentionedMe>
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
         <email>newemail@newuser.com</email>
         <postAfterMe>false</postAfterMe>
         <postMentionedMe>false</postMentionedMe>
         <postOnMyChannel>true</postOnMyChannel>
         <postOnSubscribedChannel>false</postOnSubscribedChannel>
         <followMyChannel>true</followMyChannel>
         <followRequest>true</followRequest>
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


