/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.example;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 2021/10/06
 *
 * @author TuoiCM
 */
public class HttpUtils {

    final private static int RETRY = 5;
    
    static {
        disableSslVerification();
    }

    public static String invokeHttpRequest(
            URL endpointUrl,
            String httpMethod,
            int timeout,
            Map<String, String> headers,
            String requestBody) {

        if (httpMethod.equals("GET")
                && !Utils.isNullOrEmpty(requestBody)) {
            String url = endpointUrl.toString();
            try {
                endpointUrl = new URL(url.concat("?request_data_base64=").concat(Base64.getUrlEncoder().encodeToString(requestBody.getBytes("UTF-8"))));
                requestBody = null;
            } catch (Exception ex) {
                throw new RuntimeException("Request failed. " + ex.getMessage(), ex);
            }
        }
//        int retry = RETRY;
//        while (retry > 0) {
//            try {
//                HttpURLConnection connection = createHttpConnection(endpointUrl, httpMethod, timeout, headers);
//                if (requestBody != null) {
//                    DataOutputStream wr = new DataOutputStream(
//                            connection.getOutputStream());
//                    wr.writeBytes(requestBody);
//                    wr.flush();
//                    wr.close();
//                }
//                return executeHttpRequest(connection);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            Utils.wait(1000);
//        }

//        Bỏ while
        try {
            HttpURLConnection connection = createHttpConnection(endpointUrl, httpMethod, timeout, headers);
            if (requestBody != null) {
                DataOutputStream wr = new DataOutputStream(
                        connection.getOutputStream());
                wr.writeBytes(requestBody);
                wr.flush();
                wr.close();
            }
            return executeHttpRequest(connection);
        } catch (Exception e) {
            e.printStackTrace();
        }

        throw new RuntimeException("Failed to call to identity");
    }

    private static String executeHttpRequest(HttpURLConnection connection) {
        try {
            InputStream is;
            try {
                is = connection.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                is = connection.getErrorStream();
            }

            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            throw new RuntimeException("Request failed. " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection createHttpConnection(URL endpointUrl,
            String httpMethod,
            int timeout,
            Map<String, String> headers) {
        try {
            HttpURLConnection connection = (HttpURLConnection) endpointUrl.openConnection();
            connection.setRequestMethod(httpMethod);
            connection.setRequestProperty("User-Agent", "PostmanRuntime/7.32.2");
            if (headers != null) {
                for (String headerKey : headers.keySet()) {
                    connection.setRequestProperty(headerKey, headers.get(headerKey));
                }
            }
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            return connection;
        } catch (Exception e) {
            throw new RuntimeException("Cannot create connection. " + e.getMessage(), e);
        }
    }

    public static String invokeHttpMutltiPartRequest(
            URL endpointUrl,
            String httpMethod,
            int timeout,
            Map<String, String> headers,
            Map<String, File> filesForPayload,
            Map<String, String> textsForPayload,
            String boundary) throws Exception {
        String responseString = "";
        MultipartUtility multipart = new MultipartUtility(endpointUrl, headers, "UTF-8", boundary, httpMethod);

        if (filesForPayload != null) {
            for (Entry<String, File> entry : filesForPayload.entrySet()) {
                multipart.addFilePart(entry.getKey(), entry.getValue());
            }
        }

        if (textsForPayload != null) {
            for (Entry<String, String> entry : textsForPayload.entrySet()) {
                multipart.addFormField(entry.getKey(), entry.getValue());
            }
        }

        List<String> response = multipart.finish();
        for (String line : response) {
            responseString += line;
        }
        return responseString;
    }

    private static void disableSslVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }
}
