package ru.marthastudios.combine.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CheckUnloadOnExistResponse {
    @JsonProperty("exist")
    private boolean isExist;
}
