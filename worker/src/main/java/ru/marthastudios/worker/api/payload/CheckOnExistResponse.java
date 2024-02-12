package ru.marthastudios.worker.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckOnExistResponse {
    @JsonProperty("exist")
    private Boolean isExist;
}
