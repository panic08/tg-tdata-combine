package ru.marthastudios.worker.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.marthastudios.worker.api.payload.CheckOnExistResponse;
import ru.marthastudios.worker.property.WorkerProperty;

import java.io.File;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnloadApi {
    private final WorkerProperty workerProperty;
    private final RestTemplate restTemplate;

    public CheckOnExistResponse checkOnExist(String workerId) {
        return restTemplate.getForObject(workerProperty.getCombineIp() + "/api/v1/unload/" + workerId + "/exist",
                CheckOnExistResponse.class);
    }

    public void create(String workerId, File unloadFile) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("workerId", workerId);

        try {
            body.add("file", new FileSystemResource(unloadFile.getPath()));
        } catch (Exception e){
            log.error(e.getMessage());
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity
                = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(workerProperty.getCombineIp() + "/api/v1/unload", requestEntity, Void.class);
    }
}
