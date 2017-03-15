package com.flicktek.clip.SmartphoneEvents;

/**
 * Created by alfredo on 06/10/16.
 */
public class YoutubeEvent {
    public static final int YT_STARTED=1;
    public static final int YT_FINISHED=2;

        public Integer video_id;
        public Integer status;
        public YoutubeEvent(Integer video_id, Integer status){this.video_id=video_id;
        this.status=status;}

    }
