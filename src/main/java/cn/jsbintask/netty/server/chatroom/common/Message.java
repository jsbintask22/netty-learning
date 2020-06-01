package cn.jsbintask.netty.server.chatroom.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author jsbintask@gmail.com
 * @date 2019/1/30 14:36
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String username;
    private Date sentTime;
    private String msg;
}
