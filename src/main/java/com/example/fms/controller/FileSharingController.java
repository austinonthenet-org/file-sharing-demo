package com.example.fms.controller;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;


import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class FileSharingController {
    
    @Value("${NAME:World}")
    String name;
    
    @PutMapping("/{relative_path}")
    String processUpload(HttpServletRequest request, @PathVariable(name = "relative_path") String relativePath) {
        byte[] body;
        try {
            body = request.getInputStream().readAllBytes();
            log.info("fileSize = "+body.length+" bytes.");
            Storage storage = StorageOptions.newBuilder().setProjectId("graphite-nectar-415602").build().getService();
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of("fs-landing-bkt", 
                    relativePath))
                    .build();
            log.info("starting upload to bucket.");
            storage.create(blobInfo, body);
            log.info("done upload to bucket.");
        } catch (IOException ex) {
            body = new byte[0];
            log.error("Body parsing exception occurred", ex);
            //ex.printStackTrace();
        }
      
        return "Hello " + name + "!";
      
    }
    
    @PostMapping(value = "/file-upload-path", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity uploadPath(HttpServletRequest request) {

      log.info("Upload temp request arrived ");
      Storage storage = StorageOptions.newBuilder().setProjectId("graphite-nectar-415602").build().getService();
      // Define resource
      BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of("fs-landing-bkt", 
              "51574755/14b4bda018bbd15030f498a8d5b1296cf54d33f58c8942b15c99ae444e2fcf77_uuid_3f8dd0ca-d54f-480e-976a-93d9f5f3b341_27-02-2024_20-45-38-574.PNG"))
              .build();

      // Generate Signed URL
      Map<String, String> extensionHeaders = new HashMap<>();
      extensionHeaders.put("Content-Type", "image/png");
      extensionHeaders.put("x-goog-meta-conversation_id", "368a8b9b-a508-4d60-85a8-b6def69e1eb7");
      extensionHeaders.put("x-goog-meta-dialog_id", "368a8b9b-a508-4d60-85a8-b6def69e1eb7");
      extensionHeaders.put("x-goog-meta-originator_id", "14b4bda018bbd15030f498a8d5b1296cf54d33f58c8942b15c99ae444e2fcf77");
      
      URL url =
              storage.signUrl(
                  blobInfo,
                  15,
                  TimeUnit.MINUTES,
                  Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                  Storage.SignUrlOption.withExtHeaders(extensionHeaders),
                  Storage.SignUrlOption.withV4Signature());

          System.out.println("Generated PUT signed URL:");
          System.out.println(url);
          System.out.println("You can use this URL with any user agent, for example:");
          System.out.println(
              "curl -X PUT -H 'Content-Type: application/octet-stream' --upload-file my-file '"
                  + url
                  + "'");

      return  null;
    }

}
