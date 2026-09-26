package com.chubb.claims.web;

import com.chubb.claims.domain.ClaimAttachment;
import com.chubb.claims.domain.User;
import com.chubb.claims.dto.Dtos.AttachmentDto;
import com.chubb.claims.service.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * File evidence (photos, documents) attached to a claim. Split from
 * ClaimantController/OfficerController because both a claimant (uploading
 * evidence) and an assigned officer (uploading supporting docs) can hit
 * this, and role/ownership checks already live in ClaimService.
 */
@RestController
@RequestMapping("/api/claims/{claimId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB, mirrors application.yml

    private final ClaimService claimService;

    @PostMapping
    public AttachmentDto upload(@CurrentUser User user, @PathVariable Long claimId,
                                 @RequestParam("file") MultipartFile file) {
        byte[] data = readChecked(file);
        return claimService.addAttachment(user, claimId, fileNameOf(file), contentTypeOf(file), data);
    }

    /** Replace an existing file with a new version (uploader only, claim still open). */
    @PutMapping("/{attachmentId}")
    public AttachmentDto replace(@CurrentUser User user, @PathVariable Long claimId,
                                 @PathVariable Long attachmentId,
                                 @RequestParam("file") MultipartFile file) {
        byte[] data = readChecked(file);
        return claimService.replaceAttachment(user, claimId, attachmentId, fileNameOf(file), contentTypeOf(file), data);
    }

    /** Remove a file (uploader only, claim still open). */
    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser User user, @PathVariable Long claimId, @PathVariable Long attachmentId) {
        claimService.deleteAttachment(user, claimId, attachmentId);
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<byte[]> download(@CurrentUser User user, @PathVariable Long claimId,
                                            @PathVariable Long attachmentId) {
        ClaimAttachment attachment = claimService.getAttachmentForDownload(user, claimId, attachmentId);
        String encodedName = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(attachment.getData());
    }

    private byte[] readChecked(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds 10MB limit");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read uploaded file");
        }
    }

    private static String fileNameOf(MultipartFile file) {
        String name = file.getOriginalFilename();
        return (name == null || name.isBlank()) ? "file" : name;
    }

    private static String contentTypeOf(MultipartFile file) {
        return file.getContentType() == null ? "application/octet-stream" : file.getContentType();
    }
}
