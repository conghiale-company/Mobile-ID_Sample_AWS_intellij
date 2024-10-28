/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.example;

import org.apache.log4j.Logger;

import java.net.URL;
import java.util.TreeMap;

/**
 *
 * @author Tan_Hung
 */
public class Function {

//    final private static String URL = "https://id-dev.mobile-id.vn/dtis";
//
////    final private static String RELYING_PARTY = "VEIFICATION_SERVICE";
//    final private static String ACCESS_KEY = "VW7SYCJVTUJZFADIEEE3";
//    final private static String SECRET_KEY = "Ha2dTIChzVoqnE8EdOatuChPNLyuk1MEFAGqB1W8";
//    final private static String REGION = "vn-south-1";
//    final private static String SERVICE_NAME = "dtis-20.10.05";
//    final private static String XAPI_KEY = "SARLUv9uuzoHdCJNRN1dXT-quzeLMmvMrsaTioPk";
//
//    //list function
//    final private static String FUNCTION_TOKEN = "/v2/e-identity/general/token/get";
//    final private static String FUNCTION_DN = "/v2/e-identity/utility/info/document/get";
//    final private static String FUNCTION_REFRESH_DN = "/v2/e-identity/utility/info/document/get";
//
//    final private static String HTTP_METHOD_POST = "POST";
//    final private static String HTTP_METHOD_GET = "GET";
//    final private static int TIMEOUT = 60000;
//    final private static String CONTENT_TYPE_APP_JSON = "application/json";
////    final private static String CONTENT_TYPE_APP_FORM_DATA = "multipart/form-data; boundary=";

    private static String URL;

    private static String ACCESS_KEY;
    private static String SECRET_KEY;
    private static String REGION;
    private static String SERVICE_NAME;
    private static String XAPI_KEY;

    //list function
    private static String FUNCTION_TOKEN;
    private static String FUNCTION_DN;
    private static String FUNCTION_REFRESH_DN;

    private static String HTTP_METHOD_POST;
    private static String HTTP_METHOD_GET;
    private static int TIMEOUT = -1;
    private static String CONTENT_TYPE_APP_JSON;

    private static String BASIC_TOKEN;

    private static final Logger LOG = Logger.getLogger(Function.class);

    public Function(String path) {
        BASIC_TOKEN = "Basic SVNBUFA6SGEyZFRJQ2h6Vm9xbkU4RWRPYXR1Q2hQTkx5dWsxTUVGQUdxQjFXOA==";

        getAwsConfig(path);
//        BASIC_TOKEN = "Basic " + Base64.getEncoder().encodeToString((RELYING_PARTY + ":" + SECRET_KEY).getBytes());
    }

    private void getAwsConfig(String pathConfig) {
        TreeMap<String, Object> map = Utils.readFromAwsConfig(pathConfig);
        if (map != null) {
            for (String key : map.keySet()) {
                switch (key) {
                    case "URL":
                        URL = String.valueOf(map.get(key));
                        break;

                    case "ACCESS_KEY":
                        ACCESS_KEY = String.valueOf(map.get(key));
                        break;

                    case "SECRET_KEY":
                        SECRET_KEY = String.valueOf(map.get(key));
                        break;

                    case "REGION":
                        REGION = String.valueOf(map.get(key));
                        break;

                    case "SERVICE_NAME":
                        SERVICE_NAME = String.valueOf(map.get(key));
                        break;

                    case "XAPI_KEY":
                        XAPI_KEY = String.valueOf(map.get(key));
                        break;

                    case "FUNCTION_TOKEN":
                        FUNCTION_TOKEN = String.valueOf(map.get(key));
                        break;

                    case "FUNCTION_DN":
                        FUNCTION_DN = String.valueOf(map.get(key));
                        break;

                    case "FUNCTION_REFRESH_DN":
                        FUNCTION_REFRESH_DN = String.valueOf(map.get(key));
                        break;

                    case "HTTP_METHOD_POST":
                        HTTP_METHOD_POST = String.valueOf(map.get(key));
                        break;

                    case "HTTP_METHOD_GET":
                        HTTP_METHOD_GET = String.valueOf(map.get(key));
                        break;

                    case "TIMEOUT":
                        TIMEOUT = Integer.parseInt(String.valueOf(map.get(key)));
                        break;

                    case "CONTENT_TYPE_APP_JSON":
                        CONTENT_TYPE_APP_JSON = String.valueOf(map.get(key));
                        break;

                    case "BASIC_TOKEN":
                        BASIC_TOKEN = String.valueOf(map.get(key));
                        break;

                }
            }

            if (URL == null || URL.isEmpty() || ACCESS_KEY == null || ACCESS_KEY.isEmpty() ||
                    SECRET_KEY == null || SECRET_KEY.isEmpty() || REGION == null || REGION.isEmpty() ||
                    SERVICE_NAME == null || SERVICE_NAME.isEmpty() || XAPI_KEY == null || XAPI_KEY.isEmpty() ||
                    FUNCTION_TOKEN == null || FUNCTION_TOKEN.isEmpty() || FUNCTION_DN == null || FUNCTION_DN.isEmpty() ||
                    FUNCTION_REFRESH_DN == null || FUNCTION_REFRESH_DN.isEmpty() || HTTP_METHOD_POST == null ||
                    HTTP_METHOD_POST.isEmpty() || HTTP_METHOD_GET == null || HTTP_METHOD_GET.isEmpty() ||
                    TIMEOUT == -1 || CONTENT_TYPE_APP_JSON == null || CONTENT_TYPE_APP_JSON.isEmpty() ||
                    BASIC_TOKEN == null || BASIC_TOKEN.isEmpty()) {
                LOG.debug("Invalid configuration parameter");
                System.exit(0);
            } else
                LOG.debug("Configuration parameters loaded successfully");
        }
    }

    public String login() throws Exception {
        String tokenUrl = URL + FUNCTION_TOKEN;
        String payload = null;
        AWSCall awsCall = new AWSCall(
                tokenUrl, //full path url
                HTTP_METHOD_GET,
                ACCESS_KEY,
                SECRET_KEY,
                REGION,
                SERVICE_NAME,
                TIMEOUT,
                XAPI_KEY,
                CONTENT_TYPE_APP_JSON,
                null
        );

        String jsonResp = HttpUtils.invokeHttpRequest(
                new URL(tokenUrl),
                HTTP_METHOD_GET,
                TIMEOUT,
                awsCall.getAWSV4Auth(payload, BASIC_TOKEN),
                payload);
        return jsonResp;
    }

    // 0313994173
    public String getDN(String accessToken, String mst) throws Exception {
        String tokenUrl = URL + FUNCTION_DN;
        String payload = "{\n"
                + "  \"document_type\": \"TAX\",\n"
                + "  \"tax_code\": " + "\"" + mst + "\"\n"
                + "}";

        AWSCall awsCall = new AWSCall(
                tokenUrl, //full path url
                HTTP_METHOD_POST,
                ACCESS_KEY,
                SECRET_KEY,
                REGION,
                SERVICE_NAME,
                TIMEOUT,
                XAPI_KEY,
                CONTENT_TYPE_APP_JSON,
                null
        );

        String jsonResp = HttpUtils.invokeHttpRequest(
                new URL(tokenUrl),
                HTTP_METHOD_POST,
                TIMEOUT,
                awsCall.getAWSV4Auth(payload, "Bearer " + accessToken),
                payload);
        return jsonResp;
    }

    public String refreshDN(String accessToken) throws Exception {
        String tokenUrl = URL + FUNCTION_REFRESH_DN;
        String payload = "{\n"
                + "  \"document_type\": \"TAX\",\n"
                + "  \"tax_code\": \"0313994173\""
                + "}";
        AWSCall awsCall = new AWSCall(
                tokenUrl, //full path url
                HTTP_METHOD_POST,
                ACCESS_KEY,
                SECRET_KEY,
                REGION,
                SERVICE_NAME,
                TIMEOUT,
                XAPI_KEY,
                CONTENT_TYPE_APP_JSON,
                null
        );

        String jsonResp = HttpUtils.invokeHttpRequest(
                new URL(tokenUrl),
                HTTP_METHOD_POST,
                TIMEOUT,
                awsCall.getAWSV4Auth(payload, "Bearer " + accessToken),
                payload);
        return jsonResp;
    }

    public static String getURL() {
        return URL;
    }
}
