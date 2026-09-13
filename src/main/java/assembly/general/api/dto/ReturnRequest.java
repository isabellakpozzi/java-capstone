package assembly.general.api.dto;

import assembly.general.api.entity.BookCondition;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnRequest {

    @NotNull
    private BookCondition condition;

    private String notes;
}