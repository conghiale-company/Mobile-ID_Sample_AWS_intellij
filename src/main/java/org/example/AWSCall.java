/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.example;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import vn.mobileid.aws.client.v2.AWSV4Auth;
import vn.mobileid.aws.client.v2.AWSV4Constants;

/**
 * 2021/10/06
 *
 * @author TuoiCm
 */
public class AWSCall {

    final protected static String FILE_DIRECTORY_PDF = "file/";
    private URL baseUrl;
    private String httpMethod;
    private String accessKey;
    private String secretKey;
    private String regionName;
    private String serviceName;
    private int timeOut;
    private String xApiKey;
    private String contentType;
    public String sessionToken;
    public String bearerToken;
    private TreeMap<String, String> awsHeaders;
    private AWSV4Auth.Builder builder;

    public AWSCall(String baseUrl,
            String httpMethod,
            String accessKey,
            String secretKey,
            String regionName,
            String serviceName,
            int timeOut,
            String xApiKey,
            String contentType,
            TreeMap<String, String> queryParametes) throws MalformedURLException {
        this.httpMethod = httpMethod;
        this.accessKey = accessKey; // Identify user accounts.
        this.secretKey = secretKey; // This is the secret key used in conjunction with the Access Key ID to create a signature for the request.
        this.regionName = regionName;
        this.serviceName = serviceName; // This service name is part of the signature generation process.
        this.timeOut = timeOut;
        this.xApiKey = xApiKey;
        this.contentType = contentType;

        this.baseUrl = new URL(baseUrl);

        this.awsHeaders = new TreeMap<>();
        awsHeaders.put(AWSV4Constants.X_API_KEY, this.xApiKey);
        awsHeaders.put(AWSV4Constants.CONTENT_TYPE, this.contentType);

        this.builder = new AWSV4Auth.Builder(accessKey, secretKey)
                .regionName(regionName)
                .serviceName(serviceName)
                .httpMethodName(httpMethod)// GET, PUT, POST, DELETE
                .queryParametes(queryParametes) // query parameters if any
                .awsHeaders(awsHeaders); // aws header parameters        
    }

    //AWS4Auth
    public Map<String, String> getAWSV4Auth(String payload, String token) throws MalformedURLException {
        this.awsHeaders.put(AWSV4Constants.X_AMZ_SECURITY_TOKEN, token);
        AWSV4Auth aWSV4Auth = this.builder
                .endpointURI(new URL(this.baseUrl.toString())) 
                .payload(payload)
                .build();
        return aWSV4Auth.getHeaders();
    }

    public Map<String, String> getAWSV4AuthForFormData(String payload, String token, File file) throws MalformedURLException {
        this.awsHeaders.put(AWSV4Constants.X_AMZ_SECURITY_TOKEN, token);
        this.awsHeaders.put("User-Agent", "PostmanRuntime/7.26.8");
        AWSV4Auth aWSV4Auth = this.builder
                .endpointURI(new URL(this.baseUrl.toString())) 
                .payload(payload)
                .addFile("document", file)
                .build();
        return aWSV4Auth.getHeaders();
    }

}
