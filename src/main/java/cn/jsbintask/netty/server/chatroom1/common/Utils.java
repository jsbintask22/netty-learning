package cn.jsbintask.netty.server.chatroom1.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author jsbintask@gmail.com
 * @date 2019/1/30 16:23
 */
public class Utils {
    public static String encodeMsg(Message message) {
        return message.getUsername() + "~" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(message.getSentTime())) + "~" + message.getMsg();
    }

    public static String formatDateTime(Date time) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
    }

    public static Date parseDateTime(String time) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd Hh:mm:ss").parse(time);
        } catch (ParseException e) {
            return null;
        }
    }

    public static void printMsg(Message msg) {
        System.out.println("=================================================================================================");
        System.out.println("                      " + Utils.formatDateTime(msg.getSentTime()) + "                     ");
        System.out.println(msg.getUsername() + ": " + msg.getMsg());
        System.out.println("=================================================================================================");

    }
}
