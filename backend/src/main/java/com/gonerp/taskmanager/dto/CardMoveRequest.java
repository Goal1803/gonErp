package com.gonerp.taskmanager.dto;

import lombok.Data;

@Data
public class CardMoveRequest {
    private Long targetColumnId;

    // Legacy absolute placement (still used by some callers): 1-based slot.
    private int position;

    // Relative placement for drag reorder under lazy loading. When
    // referenceCardId is set, the moved card is inserted directly before
    // (placeBefore = true) or after (placeBefore = false) that card, and the
    // server shifts neighbouring positions — so the client need not hold the
    // full card list. When referenceCardId is null, the card is appended to the
    // end of the target column.
    private Long referenceCardId;
    private Boolean placeBefore;
}
