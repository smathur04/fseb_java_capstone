package assembly.general.api.dto;

import assembly.general.api.entity.Condition;
import jakarta.validation.constraints.NotNull;

public record ReturnRequest(
        @NotNull Condition condition,
        String notes
) {
}