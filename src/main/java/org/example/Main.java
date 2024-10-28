package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Admin
 */
public class Main {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String PATH_TAX_CODE = "src/main/resources/Tax_Code.txt";
    private static final String PATH_TAX_CODE_02 = "src/main/resources/Tax_Code_02.txt";

    private static final int PAGE = 1000;
    private static final int TIME_WAIT = 300;
    private static final int PROCESS_TIME_PER_TAXCODE_FIRST = 2000; // 2000 ms = 2 seconds
    private static final int PROCESS_TIME_PER_TAXCODE_AFTER = 3000; // 3000 ms = 3 seconds
    private static final int MAX_TIME_FOR_ADDING_QUEUE = 5 * 60 * 1000; // 5 minutes in milliseconds
    private static final int MAX_RETRIES = 60; // Số lần thử tối đa
    private static final Object lock = new Object();
    private static final Logger LOG = Logger.getLogger(Main.class);

    private static String FROM_EMAIL; //requires valid gmail id
    private static String PASSWORD; // correct password for gmail id
    private static String TO_EMAIL; // can be any email id
    private static String SMTP_HOST;
    private static String TLS_PORT;
    private static String ENABLE_AUTHENTICATION;
    private static String ENABLE_STARTTLS;

    private static int expiresIn = 0;
    private static int posStartPage = 0;
    private static int posEndPage = 1000;
    private static int lineNumber = 0;

    private static boolean isLogin = false;
    private static boolean isSendTaxCodeToServer = true;

    private static String jsonResp;
    private static String accessToken = null;
    private static String PATH = "";
    private static String PATH_AWS_CONFIG = "";
    private static String PATH_SEND_EMAIL_CONFIG = "";
    private static String startDay = "";
    private static String endDay = "";

    private static JsonNode jsonNode;
    private static Function func;

    //    private static List<String> taxCodes;
    private static List<String> taxCodeList;

    public static void main(String[] args) throws Exception {
        if (args.length > 1 && args[0] != null && !args[0].isEmpty() && args[1] != null && !args[1].isEmpty()) {
            PATH_AWS_CONFIG = args[0];
            PATH_SEND_EMAIL_CONFIG = args[1];
        } else {
            System.out.println("Invalid parameter");
            System.exit(0);
        }

        func = new Function(PATH_AWS_CONFIG);

        login();

        if (isLogin) {
            if (args.length > 2 && args[2] != null && !args[2].isEmpty()) {
                if (args[2].contains(".txt")) {
                    PATH = args[2];

                    if (args.length > 3 && args[3] != null && !args[3].isEmpty())
                        loadData(args[3], false);
                    else
                        loadData(null, false);
                } else {
                    if (args[3] != null && !args[3].isEmpty()) {
                        if (args[3].contains(".txt")) {
                            PATH = args[3];
                            loadData(args[2], true);
                        } else {
                            System.out.println("The fourth parameter is invalid");
                            System.exit(0);
                        }
                    } else {
                        System.out.println("Invalid parameter");
                        System.exit(0);
                    }
                }
            } else {
//                PATH = PATH_TAX_CODE;
//                loadData("0305810961", true);
                System.out.println("Invalid parameter");
                System.exit(0);
            }
        }
    }

    private static void login() throws Exception {
        System.out.println();
        System.out.println("Logging...");
        isLogin = false;

        jsonResp = func.login();
        jsonNode = objectMapper.readTree(jsonResp);
//        System.out.println("JSON Response: " + jsonNode.toString());
        accessToken = jsonNode.get("access_token").asText();
        expiresIn = jsonNode.path("expires_in").asInt();
//        expiresIn = 5;

        if (expiresIn > 0)
            isLogin = Utils.isTokenValid(expiresIn, true);

        if (isLogin) {
            System.out.println("Login successfully");

//            Tao mot ScheduledExecutorService de gui email dinh ky
            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

//            Dinh ky gui email moi 2h
            scheduledExecutorService.scheduleAtFixedRate(Main::sendHeartbeatEmail, 2, 2, TimeUnit.HOURS);
            System.out.println("Heartbeat monitor started...");
        }
        else
            System.out.println("Login failed");
    }

    private static void sendHeartbeatEmail() {
        sendEmail("HEARTBEAT TAX-INFO", "This is a heartbeat email sent every 2 hours to monitor the " +
                "                    TAX-INFO tool.\nStatus: NORMAL ACTIVITY");
    }

    private static void loadData(String param, boolean isMST) throws Exception {
        if (param != null && !param.isEmpty()) {
            if (isMST)
                handleLoadData(param);
//                findTaxCode(param);
            else {
                posStartPage = Integer.parseInt(param);
                System.out.println("Loading data at index: " + posStartPage);
                handleLoadData(null);
            }
        } else
            handleLoadData(null);
    }

    private static void handleLoadData(String taxCode) throws Exception {
        System.out.println();

        if (taxCodeList == null || taxCodeList.isEmpty()) {
            taxCodeList = new ArrayList<>();
            System.out.println("Reading file into ram...");
            try (BufferedReader br = new BufferedReader(new FileReader(PATH))) {
                String line;
                lineNumber = 0;
                boolean isLoad = false;

                if (taxCode != null && !taxCode.isEmpty())
                    posStartPage = -1;

                while ((line = br.readLine()) != null) {
                    if (!isLoad) {
                        if (lineNumber == posStartPage) {
                            isLoad = true;
                            startDay = getDayTime();
                        }

                        if (taxCode != null && !taxCode.isEmpty() && line.equals(taxCode)) {
                            System.out.println("Found Tax_Code " + taxCode + " with index " + lineNumber);
                            posStartPage = lineNumber;
                            isLoad = true;
                        }
                    }

                    if (isLoad)
                        getTaxCodeToRam(line);

                    lineNumber++;
                }

                if (!taxCodeList.isEmpty()) {
                    System.out.println("Loaded " + taxCodeList.size() + " tax code from " + posStartPage);
                    getInfoDN();
                }

                if (posStartPage == -1) {
                    System.out.println("Not found tax code: " + taxCode + " with path " + PATH);
                    System.exit(0);
                }

                System.out.println("Successfully got all business information with path " + PATH);

                endDay = getDayTime();

                String subject = "ALL TAX CODES COMPLETED";
                String body = "INFORMATION OF ALL TAX CODES RETRIEVED: \n" +
                        "\tStart day: " + startDay + "\n" +
                        "\tEnd day: " + endDay + "\n" +
                        "\tPath File: " + PATH + "\n";

                sendEmail(subject, body);

                System.exit(0);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void handleLoadAllData() throws Exception {
        System.out.println();

//        taxCodes = new ArrayList<>();
        taxCodeList = new ArrayList<>();
        System.out.println("Reading file into ram...");
        try (BufferedReader br = new BufferedReader(new FileReader(PATH))) {
            String line;
            int lineNumber = 0;
            boolean isLoad = false;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                if (lineNumber == posStartPage)
                    isLoad = true;

                if (isLoad)
                    taxCodeList.add(line);
            }
        } catch (IOException e) {
            System.out.println("Load data failed with path " + PATH);
            e.printStackTrace();
        }

        if (taxCodeList != null && !taxCodeList.isEmpty()) {
            getInfoDN();
//            if (posStartPage >= taxCodeList.size()) {
//                System.out.println("Successfully got all business information with path " + PATH);
//                System.exit(0);
//            }
//
//            posEndPage = Math.min((posStartPage + PAGE), taxCodeList.size());
//
//            System.out.println("Loading " + PAGE + " tax code");
//            for (int i = posStartPage; i < posEndPage; i++) {
//                taxCodes.add(taxCodeList.get(i));
//
//                if (taxCodes.size() == PAGE) {
//                    getInfoDN();
//                    break;
//                }
//            }
        }
    }

    private static void findTaxCode(String taxCode) throws Exception {
        System.out.println();

        posStartPage = -1;
        if (taxCodeList == null || taxCodeList.isEmpty()) {
            taxCodeList = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(PATH))) {
                String line;
                int lineNumberSearch = 0;
                boolean isLoad = false;

                System.out.println("Searching...");
                while ((line = br.readLine()) != null) {
                    if (line.equals(taxCode)) {
                        System.out.println("Found Tax_Code " + taxCode + " with index " + lineNumberSearch);
                        posStartPage = lineNumberSearch;
                        isLoad = true;
//                        handleLoadData();
//                        break;
                    }

                    if (isLoad)
                        getTaxCodeToRam(line);
                    lineNumberSearch++;
                }

                if (!taxCodeList.isEmpty()) {
                    System.out.println("Loaded " + taxCodeList.size() + " tax code from " + posStartPage);
                    getInfoDN();
                }

                if (posStartPage == -1) {
                    System.out.println("Not found tax code: " + taxCode + " with path " + PATH);
                    System.exit(0);
                }

            } catch (IOException e) {
                System.out.println("Load data failed with path " + PATH);
                e.printStackTrace();
            }
        }
    }

    private static void getTaxCodeToRam(String line) throws Exception {
        taxCodeList.add(line);

        if (taxCodeList.size() % PAGE == 0) {
            System.out.println();
            System.out.println("Loaded " + PAGE + " tax code from " + posStartPage);
            getInfoDN();
            taxCodeList.clear();
        }
    }

    private static void getInfoDN() throws Exception {
        System.out.println();

        if (taxCodeList != null && !taxCodeList.isEmpty()) {
            System.out.println("Retrieving business information... ");
            for (int i = 0; i < taxCodeList.size(); i++) {
                if (isLogin && Utils.isTokenValid(expiresIn, false))
                    handleActionGetDN(i);
                else {
                    System.out.println("The access token has expired");
                    login();
                    if (isLogin)
                        handleActionGetDN(i);
                    else
                        System.out.println("login failed");
                }

                if (isSendTaxCodeToServer)
                    Utils.wait(PROCESS_TIME_PER_TAXCODE_FIRST);
            }
        }

    }

    private static void handleActionGetDN(int i) throws Exception {
        System.out.println();
        String mst = taxCodeList.get(i);

        System.out.println("Retrieving business information with tax code " + taxCodeList.get(i));

        jsonResp = null;

        if (mst.length() < 10) {
            isSendTaxCodeToServer = false;
            System.out.println("BUG: 4000 - PARAMETER IS INVALID" + " -- TAX CODE: " + mst + " -- INDEX: " + (posStartPage + i));
            LOG.info("BUG: 4000 - PARAMETER IS INVALID" + " -- INDEX = " + (posStartPage + i) + " -- MST = " + mst);
        }
        else {
            try {
                jsonResp = func.getDN(accessToken, mst);
                waitJsonResp(mst, i);

                if (jsonResp != null) {
                    isSendTaxCodeToServer = true;
                    try {
                        jsonNode = objectMapper.readTree(jsonResp);
                        int status = jsonNode.path("status").asInt();
                        String mess = jsonNode.path("message").asText();

                        System.out.println("posStartPage: " + posStartPage + " posEndPage: " + (posStartPage + taxCodeList.size() - 1) + " index: " + (posStartPage + i));
                        System.out.println("status: " + status + " --- mess: " + mess);

                        if (status == 0 || mess.equals("SUCCESSFULLY")) {
                            LOG.info("INDEX = " + (posStartPage + i) + " -- MST = " + mst);
                        } else if (status == 4044 || mess.contains("BUSINESS INFORMATION NOT FOUND")) {
                            LOG.info("BUG: " + status + " - " + mess + " -- INDEX = " + (posStartPage + i) + " -- MST = " + mst);
                        } else if (status == 4001 || mess.contains("UNKNOWN EXCEPTION") || status == 4045 || mess.contains("ERROR CONNECTING TO ENTITY, PLEASE TRY AGAIN LATER")) {
//                    Retry MST
                            boolean isErrorUnknownException = true;

                            String formattedDateTime = getDayTime();

                            for (int j = 0; j < 3; j++) {
                                System.out.println("\n" + formattedDateTime + " - " + mess + " - Retry MST " + mst + ": [" + (j + 1) + "]\n");
                                try {
                                    jsonResp = func.getDN(accessToken, mst);
                                    waitJsonResp(mst, i);

                                    if (jsonResp != null) {
                                        jsonNode = objectMapper.readTree(jsonResp);
                                        status = jsonNode.path("status").asInt();
                                        mess = jsonNode.path("message").asText();

                                        System.out.println("posStartPage: " + posStartPage + " posEndPage: " + (posStartPage + taxCodeList.size() - 1) + " index: " + (posStartPage + i));
                                        System.out.println("status: " + status + " --- mess: " + mess);

                                        if (status == 0 || mess.contains("SUCCESSFULLY")) {
                                            LOG.info("INDEX = " + (posStartPage + i) + " -- MST = " + mst);
                                            isErrorUnknownException = false;
                                            break;
                                        } else if (status == 4044 && mess.contains("BUSINESS INFORMATION NOT FOUND")) {
                                            LOG.info("BUG: " + status + " - " + mess + " -- INDEX = " + (posStartPage + i) + " -- MST = " + mst);
                                            isErrorUnknownException = false;
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    System.out.println(e);
                                    formattedDateTime = getDayTime();

                                    String subject = "TOOL TAX INFO AN ERROR OCCURRED";
                                    String body = "ERROR INFORMATION: \n" +
                                            "\tCode: NULL\n" +
                                            "\tMessage: " + e.getMessage() + "\n" +
                                            "\tDay: " + formattedDateTime + "\n" +
                                            "\tMST: " + mst + "\n" +
                                            "\tIndex: " + (posStartPage + i);

                                    sendEmail(subject, body);
                                    System.exit(0);
                                }
                            }

                            if (isErrorUnknownException) {
                                System.out.println("BUG: " + status + " - " + mess + " -- TAX CODE = " + mst + " -- INDEX = " + (posStartPage + i) + " -- [RETRIED 3 TIMES]");
                                LOG.info("BUG: " + status + " - " + mess + " -- INDEX = " + (posStartPage + i) + " -- MST = " + mst);
                                LOG.warn(mst);

                                formattedDateTime = getDayTime();

                                String subject = "TOOL TAX INFO AN " + mess + " HAS OCCURRED";
                                String body = "ERROR INFORMATION: \n" +
                                        "\tCode: " + status + "\n" +
                                        "\tMessage: " + mess + "\n" +
                                        "\tDay: " + formattedDateTime + "\n" +
                                        "\tMST: " + mst + "\n" +
                                        "\tIndex: " + (posStartPage + i);

                                sendEmail(subject, body);
                                System.exit(0);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                        String formattedDateTime = getDayTime();

                        String subject = "TOOL TAX INFO AN ERROR OCCURRED";
                        String body = "ERROR INFORMATION: \n" +
                                "\tCode: NULL\n" +
                                "\tMessage: " + e.getMessage() + "\n" +
                                "\tDay: " + formattedDateTime + "\n" +
                                "\tMST: " + mst + "\n" +
                                "\tIndex: " + (posStartPage + i);

                        sendEmail(subject, body);
                        System.exit(0);
                    }
                } else {
                    System.out.println("status: ERROR --- mess: DATA RESPONSE IS NULL");
                    LOG.info("BUG: DATA RESPONSE IS NULL" + " -- INDEX = " + (posStartPage + i) + " -- MST = " + mst);
                    String formattedDateTime = getDayTime();

                    String subject = "TOOL TAX INFO AN REQUEST TIME OUT HAS OCCURRED";
                    String body = "ERROR INFORMATION: \n" +
                            "Code: NULL\n" +
                            "Message: " + "DATA RESPONSE IS NULL" + "\n" +
                            "Day: " + formattedDateTime + "\n" +
                            "MST: " + mst + "\n" +
                            "Index: " + (posStartPage + i);

                    sendEmail(subject, body);
                    System.exit(0);
                }
            } catch (Exception e) {
                System.out.println(e);
                String formattedDateTime = getDayTime();

                String subject = "TOOL TAX INFO AN ERROR OCCURRED";
                String body = "ERROR INFORMATION: \n" +
                        "\tCode: NULL\n" +
                        "\tMessage: " + e.getMessage() + "\n" +
                        "\tDay: " + formattedDateTime + "\n" +
                        "\tMST: " + mst + "\n" +
                        "\tIndex: " + (posStartPage + i);

                sendEmail(subject, body);
                System.exit(0);
            }
        }

        if (i == (taxCodeList.size() - 1)) {
            posStartPage = posStartPage + i + 1;
//                posStartPage = posEndPage;
//                lock.notify();

//                With algorithm load all into ram
//                System.out.println("Successfully got all business information with path " + PATH);
//                System.exit(0);

//                loadData(null);
        }
    }

    private static void sendEmail(String subject, String body) {
        System.out.println();
        getSendEmailConfig(PATH_SEND_EMAIL_CONFIG); // Read file config to send email
        System.out.println("TLSEmail Start");
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST); //SMTP Host
        props.put("mail.smtp.port", TLS_PORT); //TLS Port
        props.put("mail.smtp.auth", ENABLE_AUTHENTICATION); //enable authentication
        props.put("mail.smtp.starttls.enable", ENABLE_STARTTLS); //enable STARTTLS

        //create Authenticator object to pass in Session.getInstance argument
        Authenticator auth = new Authenticator() {
            //override the getPasswordAuthentication method
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        };
        Session session = Session.getInstance(props, auth);

        String hostAndPort = "[" + getHostAndPort() + "]";
        String subName = " ";
        if (hostAndPort.contains("192.168.2.2"))
            subName = "DEV";
        else if (hostAndPort.contains("192.168.2.4")) {
            subName = "ISAPP";
        }

        String infoServer = "HOST NAME (POST): " + hostAndPort + "\n" + "SUB-NAME: " + subName;
        body = infoServer + "\n" + body;

        Utils.sendEmail(session, TO_EMAIL,subject, body);
    }

    private static String getHostAndPort() {
        String url = Function.getURL();
        String[] parts = url.split("//");
        return parts[1].split("/")[0];
    }

    private static String getDayTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }

    private static void getSendEmailConfig(String pathConfig) {
        TreeMap<String, Object> map = Utils.readSendEmailConfig(pathConfig);
        if (map != null) {
            for (String key : map.keySet()) {
                switch (key) {
                    case "FROM_EMAIL":
                        FROM_EMAIL = String.valueOf(map.get(key));
                        break;

                    case "PASSWORD":
                        PASSWORD = String.valueOf(map.get(key));
                        break;

                    case "TO_EMAIL":
                        TO_EMAIL = String.valueOf(map.get(key));
                        break;

                    case "SMTP_HOST":
                        SMTP_HOST = String.valueOf(map.get(key));
                        break;

                    case "TLS_PORT":
                        TLS_PORT = String.valueOf(map.get(key));
                        break;

                    case "ENABLE_AUTHENTICATION":
                        ENABLE_AUTHENTICATION = String.valueOf(map.get(key));
                        break;

                    case "ENABLE_STARTTLS":
                        ENABLE_STARTTLS = String.valueOf(map.get(key));
                        break;
                }
            }

            if (FROM_EMAIL == null || FROM_EMAIL.isEmpty() || PASSWORD == null || PASSWORD.isEmpty() ||
                    TO_EMAIL == null || TO_EMAIL.isEmpty() || SMTP_HOST == null || SMTP_HOST.isEmpty() ||
                    TLS_PORT == null || TLS_PORT.isEmpty() || ENABLE_AUTHENTICATION == null || ENABLE_AUTHENTICATION.isEmpty() ||
                    ENABLE_STARTTLS == null || ENABLE_STARTTLS.isEmpty()) {
                System.out.println("Invalid configuration parameter");
                System.exit(0);
            } else
                System.out.println("Configuration send Email parameters loaded successfully");
        }
    }

    private static void waitJsonResp(String mst, int i) {
        int attempts = 0;
        while (jsonResp == null && attempts < MAX_RETRIES) {
            Utils.wait(TIME_WAIT);
            attempts++;

            // Check for timeout here (neu goi qua nhieu lan)
            if (attempts == MAX_RETRIES) {
                LOG.info("BUG: REQUEST TIME OUT" + " -- INDEX = " + (posStartPage + i) + " -- MST = " + mst);
                String formattedDateTime = getDayTime();

                String subject = "TOOL TAX INFO AN REQUEST TIME OUT HAS OCCURRED";
                String body = "ERROR INFORMATION: \n" +
                        "\tCode: \n" +
                        "\tMessage: " + "REQUEST TIME OUT" + "\n" +
                        "\tDay: " + formattedDateTime + "\n" +
                        "\tMST: " + mst + "\n" +
                        "\tIndex: " + (posStartPage + i);

                sendEmail(subject, body);
                System.exit(0);
            }
        }
    }
}