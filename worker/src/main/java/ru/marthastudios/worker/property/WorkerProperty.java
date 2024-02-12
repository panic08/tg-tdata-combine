package ru.marthastudios.worker.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class WorkerProperty {
    @Value("${worker.id}")
    private String id;

    @Value("${worker.combineIp}")
    private String combineIp;

    @Value("${worker.tDataPath}")
    private String tDataPath;
}
