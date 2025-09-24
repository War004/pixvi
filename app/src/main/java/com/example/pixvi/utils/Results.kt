package com.example.pixvi.utils

/**
 * Provides a standard, project-wide wrapper for the result of any operation that can fail,
 * such as network requests or database I/O.
 *
 * --- DEVELOPER GUIDANCE ---
 *
 * PURPOSE:
 * To create a predictable and type-safe contract between the data layer (Repositories)
 * and the logic layer (ViewModels). This pattern is the mandated alternative to letting
 * exceptions propagate up to the ViewModel.
 *
 * USAGE CONVENTION:
 * 1.  **In the Repository Layer:**
 *     Any public function that performs a failable operation (e.g., an API call) MUST
 *     return `Result<YourDataType>`. The implementation of this function must wrap its
 *     core logic in a `try-catch` block.
 *     - On a successful execution: `return Result.Success(data)`
 *     - On any caught exception: `catch (e: Exception) { return Result.Error("User-friendly message") }`
 *
 * 2.  **In the ViewModel Layer:**
 *     The function that calls the repository will receive the `Result` object. Use an
 *     exhaustive `when` statement to process the outcome and update the UI state. This
 *     removes the need for `try-catch` blocks within the ViewModel, keeping it clean
 *     and focused on state management.
 *
 * This approach ensures that all error paths are explicitly handled at compile time,
 * significantly improving the robustness of the application.
 */
sealed class Result<T> {
    /**
     * Represents a successful outcome, containing the expected data payload.
     * @property data The data returned by the operation.
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Represents a failed outcome, containing a message suitable for UI display.
     * @property message A user-friendly string describing the error.
     */
    data class Error<T>(val errorCode: Int, val message: String) : Result<T>()
}

sealed class UiEvent {
    data class ShowToast(val message: String?) : UiEvent()
    data class isBookmarked (val success: Boolean?) : UiEvent()
}