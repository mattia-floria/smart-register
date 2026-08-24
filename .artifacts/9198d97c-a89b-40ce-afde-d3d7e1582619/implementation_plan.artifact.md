# Implementation Plan - Navigation, UI Improvements, and Dashboard Enhancements

This plan addresses several UI/UX improvements, including back gesture support, layout adjustments in the Circolari screen, app version display in Settings, and significant enhancements to the Dashboard (draggable reordering, new widgets, and translations).

## User Review Required

> [!NOTE]
> Draggable reordering in `LazyVerticalStaggeredGrid` will be implemented using a manual drag-and-drop logic as there is no built-in support in the current Compose version without external libraries.

## Proposed Changes

### Core Data & Models

#### [MODIFY] [AuthStorage.kt](file:///C:/Users/Matti/AndroidStudioProjects/smart-register/app/src/main/java/com/afloria/smartregister/data/local/AuthStorage.kt)
- Add new `WidgetType` entries: `GRADES_SUMMARY`, `ABSENCES_COUNT`, `NOTES_PREVIEW`.

---

### Navigation & Settings

#### [MODIFY] [MainScreen.kt](file:///C:/Users/Matti/AndroidStudioProjects/smart-register/app/src/main/java/com/afloria/smartregister/ui/MainScreen.kt)
- **Back Gesture**: Add `BackHandler` to handle navigating back from sub-sections to the Home tab and from `SettingsSection` sub-menus to the main settings menu.
- **Circolari Screen**: Adjust `SettingsHeader` to reduce padding for the back arrow, moving it closer to the screen corner.
- **Settings Version**: Update `SettingsHeader` to optionally display the app version in the top-right corner.
- **Translations**: Ensure section headers and labels are correctly translated (where applicable).

---

### Dashboard Enhancements

#### [MODIFY] [DashboardScreen.kt](file:///C:/Users/Matti/AndroidStudioProjects/smart-register/app/src/main/java/com/afloria/smartregister/ui/screens/DashboardScreen.kt)
- **Draggable Reordering**:
    - Implement drag detection in `DashboardWidgetContainer` when `isEditMode` is active.
    - Update `LazyVerticalStaggeredGrid` to handle item reordering via the `MainViewModel#moveWidget` method.
- **New Widgets**:
    - Add UI implementation for `GRADES_SUMMARY`, `ABSENCES_COUNT`, and `NOTES_PREVIEW`.
- **Translations**:
    - Translate all widget names in the "Add Widget" sheet and widget headers to Italian.
    - Example: `AI_BRIEF` -> "Smarty Brief", `RECOVERY_STATUS` -> "Media Totale", `COUNTDOWN` -> "Conto alla rovescia", etc.

---

## Verification Plan

### Automated Tests
- N/A (UI focused changes)

### Manual Verification
- **Back Gesture**: Verify that the system back gesture/button correctly navigates back from a sub-section (e.g., Circolari) to the Home tab.
- **Back Gesture**: Verify that in Settings, the back gesture returns to the main settings menu if in a sub-menu.
- **Settings**: Verify the app version is visible in the top-right corner.
- **Circolari**: Verify the back arrow is positioned closer to the corner.
- **Dashboard**: Enter Edit Mode, drag a widget, and verify it moves to the new position.
- **Dashboard**: Add new widgets (Grades Summary, Absences, Notes) and verify they display correctly.
- **Dashboard**: Verify all widget names are in Italian.
