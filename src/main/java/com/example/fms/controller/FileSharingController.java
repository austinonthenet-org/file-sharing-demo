package com.example.fms.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/v1")
public class FileSharingController {
    
    @Value("${NAME:World}")
    String name;
    
    @Value("${google.storage.bucket.name}")
    String bucketName;
    
    @Value("${google.project.id}")
    String projectId;
    
    @PutMapping("/{brand}/{relative_path}")
    public String processUpload(HttpServletRequest request,
            @PathVariable(name = "brand") String brandId, 
            @PathVariable(name = "relative_path") String relativePath) {
        byte[] body;
        try {
            body = request.getInputStream().readAllBytes();
            log.info("fileSize = "+body.length+" bytes.");
            Tika tika = new Tika();
            String mimeType = tika.detect(body);
            log.info("starting upload to bucket. Mime type = "+mimeType);
            Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, 
                    brandId +"/"+ relativePath))
                    .setContentType(mimeType)
                    .build();
            
            storage.create(blobInfo, body);
            log.info("done upload to bucket.");
        } catch (IOException ex) {
            body = new byte[0];
            log.error("Body parsing exception occurred", ex);
            //ex.printStackTrace();
        }
      
        return "Hello " + name + "!";
      
    }
    
    @GetMapping("/{brand}/{relative_path}")
    public ResponseEntity<InputStream> processDownload(HttpServletRequest request,
            @PathVariable(name = "brand") String brandId, 
            @PathVariable(name = "relative_path") String relativePath,
            HttpServletResponse response) {
        
        try {
            
            Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
            BlobId blobId = BlobId.of(bucketName, 
                    brandId +"/"+ relativePath);
            
            log.info("starting download.");
            Blob blob = storage.get(blobId);
            if (blob != null) {
                String mimeType = blob.asBlobInfo().getContentType();
    
                //storage.downloadTo(blobId, os, null);
                ReadChannel reader = blob.reader();
                return ResponseEntity.ok().contentType(MediaType.valueOf(mimeType)).body(Channels.newInputStream(reader));
            }
            log.info("done download.");
            
        } catch (Exception ex) {
            log.error("Body parsing exception occurred", ex);
            //ex.printStackTrace();
        }
      
        return ResponseEntity.notFound().build();
      
    }
    
//    @PostMapping(value = "/file-upload-path", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity uploadPath(HttpServletRequest request) {
//
//      log.info("Upload temp request arrived ");
//      Storage storage = StorageOptions.newBuilder().setProjectId("graphite-nectar-415602").build().getService();
//      // Define resource
//      BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of("fs-landing-bkt", 
//              "51574755/14b4bda018bbd15030f498a8d5b1296cf54d33f58c8942b15c99ae444e2fcf77_uuid_3f8dd0ca-d54f-480e-976a-93d9f5f3b341_27-02-2024_20-45-38-574.PNG"))
//              .build();
//
//      // Generate Signed URL
//      Map<String, String> extensionHeaders = new HashMap<>();
//      extensionHeaders.put("Content-Type", "image/png");
//      extensionHeaders.put("x-goog-meta-conversation_id", "368a8b9b-a508-4d60-85a8-b6def69e1eb7");
//      extensionHeaders.put("x-goog-meta-dialog_id", "368a8b9b-a508-4d60-85a8-b6def69e1eb7");
//      extensionHeaders.put("x-goog-meta-originator_id", "14b4bda018bbd15030f498a8d5b1296cf54d33f58c8942b15c99ae444e2fcf77");
//      
//      URL url =
//              storage.signUrl(
//                  blobInfo,
//                  15,
//                  TimeUnit.MINUTES,
//                  Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
//                  Storage.SignUrlOption.withExtHeaders(extensionHeaders),
//                  Storage.SignUrlOption.withV4Signature());
//
//          System.out.println("Generated PUT signed URL:");
//          System.out.println(url);
//          System.out.println("You can use this URL with any user agent, for example:");
//          System.out.println(
//              "curl -X PUT -H 'Content-Type: application/octet-stream' --upload-file my-file '"
//                  + url
//                  + "'");
//
//      return  null;
//    }

}
