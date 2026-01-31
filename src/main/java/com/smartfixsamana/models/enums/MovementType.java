package com.smartfixsamana.models.enums;

public enum MovementType {
    PURCHASE,       // stock in - purchases
    SALE,           // stock out - direct sale
    REPAIR_USE,     // stock out - used in repair
    REPAIR_RETURN,  // stock in - removed from repair
    ADJUSTMENT,     // manual correction +/-
    DAMAGE          // stock out - damaged/lost
}
