# Walkthrough - Automated Release Descriptions

I have updated the GitHub CI workflows to automatically extract the release description from your commit messages.

## Key Changes

### 1. Updated Workflow Logic
Modified both [.github/workflows/android.yml](file:///.github/workflows/android.yml) and [.github/workflows/android_build.yml](file:///.github/workflows/android_build.yml) to:
- Identify the version tag (e.g., `v1.2.3`).
- Detect the `prerelease` keyword.
- **Clean the Message**: It removes the version tag and the word "prerelease" from the commit message to produce a clean description.
- **Support Multiline**: It uses GitHub Actions' multiline output syntax (`<<EOF`) to ensure your entire commit description is captured correctly.

### 2. Custom Release Body
The `Create Release` step now uses the extracted description as the `body` of the GitHub Release, replacing the old generic message.

## Example Usage
If you commit with this message:
```text
v1.3.0: Refined the login screen and fixed a bug in the dashboard.
- Added new icons
- Improved spacing
prerelease
```

The resulting Release on GitHub will be:
- **Tag**: `v1.3.0`
- **Name**: `Smart-register v1.3.0`
- **Description**:
  ```text
  Refined the login screen and fixed a bug in the dashboard.
  - Added new icons
  - Improved spacing
  ```
- **Type**: Marked as **Prerelease**.

## Verification
To verify, simply push a commit with a version tag and a description!
