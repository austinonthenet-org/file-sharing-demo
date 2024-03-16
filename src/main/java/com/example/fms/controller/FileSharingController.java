package com.example.fms.controller;

import java.io.IOException;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.MultipartContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
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

    @Value("${google.storage.bucket.name}")
    String bucketName;

    @Value("${google.project.id}")
    String projectId;

    @PutMapping("/{brand}/{relative_path}")
    public ResponseEntity<String> processUpload(HttpServletRequest request, @PathVariable(name = "brand") String brandId,
            @PathVariable(name = "relative_path") String relativePath) {

        byte[] body;

        try {

            body = request.getInputStream().readAllBytes();
            log.info("fileSize = " + body.length + " bytes.");
            Tika tika = new Tika();
            String mimeType = tika.detect(body);

            // TODO: call ClamAV for virus scan
            HttpResponse response = makeScanRequest("https://clam-rest-service-3fxgfyig7a-uc.a.run.app/", "https://clam-rest-service-3fxgfyig7a-uc.a.run.app", mimeType, body);
            if (response == null || !"OK".equals(response.getStatusMessage())) {
                log.info("Response status: "+ new String(response.getStatusMessage()));
                log.info("Response body: "+ new String(response.getContent().readAllBytes()));
                return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body("Failed!!");
            }
            
            log.info("Response status: "+ new String(response.getStatusMessage()));

            // TODO: call Google Vision API for image profanity

            log.info("starting upload to bucket. Mime type = " + mimeType);
            Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, brandId + "/" + relativePath))
                    .setContentType(mimeType).build();

            storage.create(blobInfo, body);
            log.info("done upload to bucket.");
        } catch (IOException ex) {
            body = new byte[0];
            log.error("Body parsing exception occurred", ex);
            // ex.printStackTrace();
        }

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body("Success!!");

    }
    
    public static HttpResponse makeScanRequest(String serviceUrl, String audience, String contentType, byte[] contentBytes) throws IOException {
        try {
            log.info("making inter service call...");
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            if (!(credentials instanceof IdTokenProvider)) {
              throw new IllegalArgumentException("Credentials are not an instance of IdTokenProvider.");
            }
            
            log.info("credentials: "+credentials.toString());
            
            IdTokenCredentials tokenCredential =
                IdTokenCredentials.newBuilder()
                    .setIdTokenProvider((IdTokenProvider) credentials)
                    .setTargetAudience(audience)
                    .build();
    
            log.info("tokenCredential: "+tokenCredential.toString());
            
            GenericUrl genericUrl = new GenericUrl(serviceUrl);
            HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(tokenCredential);
            HttpTransport transport = new NetHttpTransport();
            
            // Add parameters
            MultipartContent content = new MultipartContent().setMediaType(
                    new HttpMediaType("multipart/form-data")
                            .setParameter("boundary", "__END_OF_PART__"));
            // Add file
            ByteArrayContent fileContent = new ByteArrayContent(
                    contentType, contentBytes);
            MultipartContent.Part part = new MultipartContent.Part(fileContent);
            part.setHeaders(new HttpHeaders().set(
                    "Content-Disposition", 
                    String.format("form-data; name=\"content\"; filename=\"test\"")));
            content.addPart(part);
            
            HttpRequest request = transport.createRequestFactory(adapter).buildPostRequest(genericUrl, content);
            return request.execute();
        } catch (Exception e) {
            log.error("Exception calling other cloud-run service.", e);
        }
        return null;
    }
    
    public static HttpResponse makeGetRequest(String serviceUrl, String audience) throws IOException {
        try {
            log.info("making inter service call...");
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            if (!(credentials instanceof IdTokenProvider)) {
              throw new IllegalArgumentException("Credentials are not an instance of IdTokenProvider.");
            }
            
            log.info("credentials: "+credentials.toString());
            
            IdTokenCredentials tokenCredential =
                IdTokenCredentials.newBuilder()
                    .setIdTokenProvider((IdTokenProvider) credentials)
                    .setTargetAudience(audience)
                    .build();
    
            log.info("tokenCredential: "+tokenCredential.toString());
            
            GenericUrl genericUrl = new GenericUrl(serviceUrl);
            HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(tokenCredential);
            HttpTransport transport = new NetHttpTransport();
            HttpRequest request = transport.createRequestFactory(adapter).buildGetRequest(genericUrl);
            return request.execute();
        } catch (Exception e) {
            log.error("Exception calling other cloud-run service.", e);
        }
        return null;
    }

    @GetMapping("/{brand}/{relative_path}")
    public ResponseEntity<byte[]> processDownload(HttpServletRequest request,
            @PathVariable(name = "brand") String brandId, @PathVariable(name = "relative_path") String relativePath,
            HttpServletResponse response) {

        try {

            Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
            BlobId blobId = BlobId.of(bucketName, brandId + "/" + relativePath);

            log.info("starting download.");
            Blob blob = storage.get(blobId);
            if (blob != null) {
                String mimeType = blob.asBlobInfo().getContentType();

                return ResponseEntity.ok().contentType(MediaType.valueOf(mimeType)).body(blob.getContent());
            }
            log.info("done download.");

        } catch (Exception ex) {
            log.error("Body parsing exception occurred", ex);
        }

        return ResponseEntity.notFound().build();

    }

}
