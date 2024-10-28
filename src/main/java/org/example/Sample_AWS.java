/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 *
 * @author Tan_Hung
 */
public class Sample_AWS {

    static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        boolean loop = true;
        int resultCode;
        String jsonResp;
        JsonNode jsonNode;
        String accessToken = null;

        Function func = new Function("");
        do {
            printUsage();
            System.out.print("Choice: ");
            String choice = reader.readLine();
            if (Utils.isNumeric(choice)) {
                resultCode = Integer.parseInt(choice);
            } else {
                break;
            }
            switch (resultCode) {
                case 1:
                    jsonResp = func.login();
                    System.out.println("JSON Response: " + jsonResp);
                    jsonNode = objectMapper.readTree(jsonResp);
                    accessToken = jsonNode.path("access_token").asText();
                    System.out.println("AccessToken: " + accessToken);
                    break;
                case 2:
                    jsonResp = func.getDN(accessToken, "0313994173");
                    System.out.println("JSON Response: " + jsonResp);
                    jsonNode = objectMapper.readTree(jsonResp);

                    String status = jsonNode.path("status").asText();
                    System.out.println("status: " + status);
                    String mess = jsonNode.path("message").asText();
                    System.out.println("message: " + mess);
                    String dtis_id = jsonNode.path("dtis_id").asText();
                    System.out.println("dtis_id: " + dtis_id);

                    System.out.println("document_data: {");
                    System.out.println("    tax_informations: [");
                    System.out.println("        owner_information: {");
                    JsonNode taxInformations = jsonNode.path("document_data").path("tax_informations");
                    for (JsonNode taxInfo : taxInformations) {
                        System.out.println("full_name: " + taxInfo.path("owner_information").path("full_name").asText());
                        System.out.println("id_card_number: " + taxInfo.path("owner_information").path("id_card_number").asText());
                        System.out.println("address: " + taxInfo.path("owner_information").path("address").asText());
                    }
                    break;
                    
                    case 3:
                    jsonResp = func.refreshDN(accessToken);
                    System.out.println("JSON Response: " + jsonResp);
                    jsonNode = objectMapper.readTree(jsonResp);

                    status = jsonNode.path("status").asText();
                    System.out.println("status: " + status);
                    mess = jsonNode.path("message").asText();
                    System.out.println("message: " + mess);
                    dtis_id = jsonNode.path("dtis_id").asText();
                    System.out.println("dtis_id: " + dtis_id);

                    System.out.println("document_data: {");
                    System.out.println("    tax_informations: [");
                    System.out.println("        owner_information: {");
                    taxInformations = jsonNode.path("document_data").path("tax_informations");
                    for (JsonNode taxInfo : taxInformations) {
                        System.out.println("full_name: " + taxInfo.path("owner_information").path("full_name").asText());
                        System.out.println("id_card_number: " + taxInfo.path("owner_information").path("id_card_number").asText());
                        System.out.println("address: " + taxInfo.path("owner_information").path("address").asText());
                    }
                    break;
                default:
                    loop = false;
            }
        } while (loop);
        System.out.println("Exit!");
    }

    private static void printUsage() {
        System.out.println("Functions: ");
        System.out.println("\t 1. /oidc/token");
        System.out.println("\t 2. info/document/get");
        System.out.println("\t 2. info/document/refresh");
        System.out.println("\t (any).quit");

    }
}
