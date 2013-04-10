CREATE TABLE notification_settings (
   jid VARCHAR(300),
   target VARCHAR(300),
   type VARCHAR(50),
   post_after_me BOOLEAN DEFAULT TRUE,
   post_mentioned_me BOOLEAN DEFAULT TRUE,
   post_on_my_channel BOOLEAN DEFAULT TRUE,
   post_on_subscribed_channel BOOLEAN DEFAULT FALSE,
   follow_my_channel BOOLEAN DEFAULT TRUE,
   follow_request BOOLEAN DEFAULT TRUE,
   PRIMARY KEY (jid, type)
);
CREATE INDEX notification_settings_jid_index ON notification_settings(jid);
