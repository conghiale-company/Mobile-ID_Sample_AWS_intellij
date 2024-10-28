/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.example;

import org.apache.log4j.Logger;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.time.Instant;
import java.util.Date;
import java.util.TreeMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author DELL
 */
public class Utils {
    private static final Logger LOG = Logger.getLogger(Utils.class);
    private static long expirationTimeInSeconds = 0;

    public static boolean isNumeric(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {

        }
        return false;
    }

    public static boolean isNullOrEmpty(String value) {
        if (value == null) {
            return true;
        }
        if (value.compareTo("") == 0) {
            return true;
        }
        return false;
    }

    public static void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean isTokenValid(int expiresIn, boolean isChange) {
        long currentTimeInSeconds = Instant.now().getEpochSecond();
//        System.out.println("currentTimeInSeconds: " + currentTimeInSeconds);
        expirationTimeInSeconds = isChange ? currentTimeInSeconds + expiresIn : expirationTimeInSeconds;

        return currentTimeInSeconds < expirationTimeInSeconds;
    }

    public static boolean writeToFile(int index, String mst) {
        try (FileWriter fw = new FileWriter("src/main/log/log.txt", true); // Mở file với chế độ append
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println("INDEX = " + index + " -- MST = " + mst);
            return true;
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
            return false;
        }
    }

    public static TreeMap<String, Object> readFromAwsConfig(String path) {
        TreeMap<String, Object> map = new TreeMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("URL")) {
                    map.put("URL", line.split(" = ")[1]);
                } else if (line.contains("RELYING_PARTY")) {
                    map.put("RELYING_PARTY", line.split(" = ")[1]);
                } else if (line.contains("ACCESS_KEY")) {
                    map.put("ACCESS_KEY", line.split(" = ")[1]);
                } else if (line.contains("SECRET_KEY")) {
                    map.put("SECRET_KEY", line.split(" = ")[1]);
                } else if (line.contains("REGION")) {
                    map.put("REGION", line.split(" = ")[1]);
                } else if (line.contains("SERVICE_NAME")) {
                    map.put("SERVICE_NAME", line.split(" = ")[1]);
                } else if (line.contains("XAPI_KEY")) {
                    map.put("XAPI_KEY", line.split(" = ")[1]);
                } else if (line.contains("FUNCTION_TOKEN")) {
                    map.put("FUNCTION_TOKEN", line.split(" = ")[1]);
                } else if (line.contains("FUNCTION_DN")) {
                    map.put("FUNCTION_DN", line.split(" = ")[1]);
                } else if (line.contains("FUNCTION_REFRESH_DN")) {
                    map.put("FUNCTION_REFRESH_DN", line.split(" = ")[1]);
                } else if (line.contains("HTTP_METHOD_POST")) {
                    map.put("HTTP_METHOD_POST", line.split(" = ")[1]);
                } else if (line.contains("HTTP_METHOD_GET")) {
                    map.put("HTTP_METHOD_GET", line.split(" = ")[1]);
                } else if (line.contains("TIMEOUT")) {
                    map.put("TIMEOUT", line.split(" = ")[1]);
                } else if (line.contains("CONTENT_TYPE_APP_JSON")) {
                    map.put("CONTENT_TYPE_APP_JSON", line.split(" = ")[1]);
                } else if (line.contains("CONTENT_TYPE_APP_FORM_DATA")) {
                    map.put("CONTENT_TYPE_APP_FORM_DATA", line.split(" = ")[1]);
                } else if (line.contains("BASIC_TOKEN")) {
                    map.put("BASIC_TOKEN", line.split(" = ")[1]);
                }
            }

            return map;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static TreeMap<String, Object> readSendEmailConfig(String path) {
        TreeMap<String, Object> map = new TreeMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("FROM_EMAIL")) {
                    map.put("FROM_EMAIL", line.split(" = ")[1]);
                } else if (line.contains("PASSWORD")) {
                    map.put("PASSWORD", line.split(" = ")[1]);
                } else if (line.contains("TO_EMAIL")) {
                    map.put("TO_EMAIL", line.split(" = ")[1]);
                } else if (line.contains("SMTP_HOST")) {
                    map.put("SMTP_HOST", line.split(" = ")[1]);
                } else if (line.contains("TLS_PORT")) {
                    map.put("TLS_PORT", line.split(" = ")[1]);
                } else if (line.contains("ENABLE_AUTHENTICATION")) {
                    map.put("ENABLE_AUTHENTICATION", line.split(" = ")[1]);
                } else if (line.contains("ENABLE_STARTTLS")) {
                    map.put("ENABLE_STARTTLS", line.split(" = ")[1]);
                }
            }

            return map;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void sendEmail(Session session, String toEmail, String subject, String body){
        try
        {
            MimeMessage msg = new MimeMessage(session);
            //set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");

            msg.setFrom(new InternetAddress("nghialc@mobile-id.com", "Conghiale"));
            msg.setReplyTo(InternetAddress.parse("nghialc@mobile-id.com", false));
            msg.setSubject(subject, "UTF-8");
            msg.setText(body, "UTF-8");
            msg.setSentDate(new Date());

            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
            LOG.debug("Message is ready");

            Transport.send(msg);

            LOG.debug("Email Sent Successfully!!");
        }
        catch (Exception e) {
            e.printStackTrace();
            wait(5000);
            sendEmail(session, toEmail, subject, body);
        }
    }
}
