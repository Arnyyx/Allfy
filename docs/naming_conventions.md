# Allfy Project

## Naming Conventions

- **State Variables**
    - Loading: `is[Action][Object]` (e.g., `isUploadingPost`)
    - Error: `[action][Object]Error` (e.g., `uploadPostError`)
    - Success: `[action][Object]Success` (e.g., `uploadPostSuccess`)
    - Data: `[object]s` or `[object]` (e.g., `feedPosts`, `post`)

- **Flow/StateFlow**
    - Private: `_name` (e.g., `_postState`)
    - Public: `name` (e.g., `postState`)

- **Functions**
    - Action: `[action][Object]` (e.g., `uploadPost`)
    - Clear: `clear[Action][Object]State` (e.g., `clearUploadPostState`)
    - Reset: `reset[Object]State` (e.g., `resetPostState`)
