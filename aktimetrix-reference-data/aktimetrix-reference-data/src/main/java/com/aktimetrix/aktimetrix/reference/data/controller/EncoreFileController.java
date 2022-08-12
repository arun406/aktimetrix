package com.aktimetrix.aktimetrix.reference.data.controller;

import com.aktimetrix.aktimetrix.reference.data.config.S3StorageProperties;
import com.aktimetrix.aktimetrix.reference.data.dto.*;
import com.aktimetrix.aktimetrix.reference.data.exception.EncoreFileUploadException;
import com.aktimetrix.aktimetrix.reference.data.service.*;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/encore/")
@RequiredArgsConstructor
public class EncoreFileController {
    private static final Logger log = LoggerFactory.getLogger(EncoreFileController.class);

    private final ProductGroupService productGroupService;
    private final S3StorageService s3StorageService;
    private final FlightGroupService flightGroupService;
    private final RouteService routeService;
    private final LineService lineService;
    private final S3StorageProperties s3StorageProperties;

    /**
     * upload file
     *
     * @param file
     * @return
     */
    @PostMapping("{type}/actions/upload")
    public UploadFileResponse uploadFile(@PathVariable String type, @RequestParam("file") MultipartFile file) throws EncoreFileUploadException {
        CSVReader reader = null;

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        // Check if the file's name contains invalid characters
        if (fileName.contains("..")) {
            throw new EncoreFileUploadException("Sorry! Filename contains invalid path sequence " + fileName);
        }

        try {
            s3StorageService.upload(file.getInputStream(), s3StorageProperties.getBucketName(), fileName);

            switch (EncoreDataType.valueOf(type)) {
                case PRODUCT_GROUP:
                    List<ProductGroupDTO> productGroups = EncoreFileParserService
                            .parseAndReturnProductGroups(file.getInputStream());
                    if (productGroups != null) {
                        this.productGroupService.cleanup();
                        productGroupService.save(productGroups);
                    }
                    break;
                case FLIGHT_GROUP:
                    List<FlightGroupDTO> flightGroups = EncoreFileParserService
                            .parseAndReturnFlightGroups(file.getInputStream());
                    if (flightGroups != null) {
                        this.flightGroupService.cleanup();
                        this.flightGroupService.save(flightGroups);
                    }
                    break;
                case ROUTES:
                    List<RouteDTO> routes = EncoreFileParserService
                            .parseAndReturnRoutes(file.getInputStream());
                    if (routes != null) {
                        this.routeService.cleanup();
                        this.routeService.saveAll(routes);
                    }
                    break;
                case LINES:
                    List<LineDTO> lines = EncoreFileParserService
                            .parseAndReturnLines(file.getInputStream());
                    if (lines != null) {
                        this.lineService.cleanup();
                        this.lineService.saveAll(lines);
                    }
                    break;
            }
        } catch (IOException | CsvException e) {
            log.error(e.getMessage(), e);
            throw new EncoreFileUploadException(e);
        } finally {
            try {
                EncoreFileParserService.closeReader(reader);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new EncoreFileUploadException(e);
            }
        }
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("encore/actions/download/")
                .path(fileName)
                .toUriString();

        return new UploadFileResponse(fileName, fileDownloadUri,
                file.getContentType(), file.getSize());
    }


    /**
     * download file
     *
     * @param fileName
     * @param request
     * @return
     */
    @GetMapping("/actions/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = this.s3StorageService.download(s3StorageProperties.getBucketName(), fileName);
        // Try to determine file's content type
        String contentType = null;

        // Fallback to the default content type if type could not be determined
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
