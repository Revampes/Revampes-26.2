package xyz.revampes.Revamp.utility.dungeon.terminal;

/**
 * Every visual state a terminal slot can be in. Straight port of yqloss'
 * {@code yqloss.yqlossclientmixinkt.module.betterterminal.SlotType}.
 */
public enum SlotType {
    EMPTY,
    ORDER_1,
    ORDER_2,
    ORDER_3,
    ORDER_CLICKED,
    ORDER_OTHER,
    PANES_ON,
    PANES_OFF,
    START_CORRECT,
    START_CLICKED,
    START_WRONG,
    COLOR_CORRECT,
    COLOR_CLICKED,
    COLOR_WRONG,
    RUBIX_RIGHT_2,
    RUBIX_RIGHT_1,
    RUBIX_CORRECT,
    RUBIX_LEFT_1,
    RUBIX_LEFT_2,
    ALIGN_TARGET,
    ALIGN_ACTIVE_CURRENT,
    ALIGN_ACTIVE_OTHER,
    ALIGN_INACTIVE,
    ALIGN_ACTIVE_BUTTON,
    ALIGN_INACTIVE_BUTTON
}
