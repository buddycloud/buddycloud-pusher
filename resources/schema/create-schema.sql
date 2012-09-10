CREATE TABLE subscribers (
   jid VARCHAR(300),
   email VARCHAR(300),
   PRIMARY KEY (jid)
);
CREATE INDEX subscribers_jid_index ON subscribers(jid);

CREATE TABLE notification_settings (
   jid VARCHAR(300),
   post_after_me BOOLEAN DEFAULT TRUE,
   post_mentioned_me BOOLEAN DEFAULT TRUE,
   post_on_my_channel BOOLEAN DEFAULT TRUE,
   post_on_subscribed_channel BOOLEAN DEFAULT TRUE,
   follow_my_channel BOOLEAN DEFAULT TRUE,
   PRIMARY KEY (jid)
);
CREATE INDEX notification_settings_jid_index ON notification_settings(jid);
