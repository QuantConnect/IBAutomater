/*
 * QUANTCONNECT.COM - Democratizing Finance, Empowering Individuals.
 * IBAutomater v1.0. Copyright 2019 QuantConnect Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

using System.Collections.Generic;

namespace QuantConnect.IBAutomater
{
    /// <summary>
    /// The IBAutomater error codes
    /// </summary>
    public enum ErrorCode
    {
        /// <summary>
        /// No error
        /// </summary>
        None,

        /// <summary>
        /// The process failed to start
        /// </summary>
        ProcessStartFailed,

        /// <summary>
        /// A Java exception was thrown
        /// </summary>
        JavaException,

        /// <summary>
        /// The login failed
        /// </summary>
        LoginFailed,

        /// <summary>
        /// An existing session was detected
        /// </summary>
        ExistingSessionDetected,

        /// <summary>
        /// A security dialog (code card) was detected
        /// </summary>
        SecurityDialogDetected,

        /// <summary>
        /// Two factor authentication request was not confirmed within 3 minutes
        /// </summary>
        TwoFactorConfirmationTimeout,

        /// <summary>
        /// The IBGateway version is no longer supported
        /// </summary>
        UnsupportedVersion
    }

    /// <summary>
    /// Represents the result of an IBAutomater start (or restart) operation
    /// </summary>
    public class StartResult
    {
        private static readonly Dictionary<ErrorCode, string> ErrorMessages =
            new Dictionary<ErrorCode, string>
            {
                {
                    ErrorCode.ProcessStartFailed,
                    "The IBAutomater was unable to start the IBGateway process."
                },
                {
                    ErrorCode.JavaException,
                    "A Java exception was thrown."
                },
                {
                    ErrorCode.LoginFailed,
                    "Login failed. Please check the validity of your login credentials."
                },
                {
                    ErrorCode.ExistingSessionDetected,
                    "An existing session was detected and will not be automatically disconnected. " +
                    "Please close the existing session manually."
                },
                {
                    ErrorCode.SecurityDialogDetected,
                    "A security dialog was detected for Code Card Authentication. " +
                    "Only 'Seamless Authentication' via IBKR mobile app is supported."
                },
                {
                    ErrorCode.TwoFactorConfirmationTimeout,
                    "The two factor authentication request timed out. " +
                    "The request must be confirmed within 3 minutes."
                },
                {
                    ErrorCode.UnsupportedVersion,
                    "The IBGateway version is no longer supported."
                }
            };

        /// <summary>
        /// Returns a <see cref="StartResult"/> object indicating that the start operation was successful
        /// </summary>
        public static readonly StartResult Success = new StartResult(ErrorCode.None);

        /// <summary>
        /// The IBAutomater error code
        /// </summary>
        public ErrorCode ErrorCode { get; }

        /// <summary>
        /// The IBAutomater error message
        /// </summary>
        public string ErrorMessage { get; }

        /// <summary>
        /// Returns true if the start operation was successful
        /// </summary>
        public bool HasError => ErrorCode != ErrorCode.None;

        /// <summary>
        /// Creates a new instance of the <see cref="StartResult"/> class
        /// </summary>
        /// <param name="errorCode"></param>
        /// <param name="additionalMessage"></param>
        public StartResult(ErrorCode errorCode, string additionalMessage = "")
        {
            ErrorCode = errorCode;
            ErrorMessage = GetErrorMessage(errorCode);

            if (!string.IsNullOrWhiteSpace(additionalMessage))
            {
                ErrorMessage += $" - {additionalMessage}";
            }
        }

        /// <summary>
        /// Returns an error message for the given IBAutomater error
        /// </summary>
        /// <returns>The error message</returns>
        private static string GetErrorMessage(ErrorCode errorCode)
        {
            string errorMessage;
            return ErrorMessages.TryGetValue(errorCode, out errorMessage) ? errorMessage : string.Empty;
        }
    }
}
