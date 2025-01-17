/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.telephony.ims.stub;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.os.RemoteException;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.ims.aidl.IImsSmsListener;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Base implementation for SMS over IMS.
 *
 * Any service wishing to provide SMS over IMS should extend this class and implement all methods
 * that the service supports.
 *
 * @hide
 */
@SystemApi
@TestApi
public class ImsSmsImplBase {
    private static final String LOG_TAG = "SmsImplBase";

    /** @hide */
    @IntDef({
            SEND_STATUS_OK,
            SEND_STATUS_ERROR,
            SEND_STATUS_ERROR_RETRY,
            SEND_STATUS_ERROR_FALLBACK
        })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SendStatusResult {}
    /**
     * Message was sent successfully.
     */
    public static final int SEND_STATUS_OK = 1;

    /**
     * IMS provider failed to send the message and platform should not retry falling back to sending
     * the message using the radio.
     */
    public static final int SEND_STATUS_ERROR = 2;

    /**
     * IMS provider failed to send the message and platform should retry again after setting TP-RD
     * bit to high.
     */
    public static final int SEND_STATUS_ERROR_RETRY = 3;

    /**
     * IMS provider failed to send the message and platform should retry falling back to sending
     * the message using the radio.
     */
    public static final int SEND_STATUS_ERROR_FALLBACK = 4;

    /** @hide */
    @IntDef({
            DELIVER_STATUS_OK,
            DELIVER_STATUS_ERROR_GENERIC,
            DELIVER_STATUS_ERROR_NO_MEMORY,
            DELIVER_STATUS_ERROR_REQUEST_NOT_SUPPORTED
        })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DeliverStatusResult {}
    /**
     * Message was delivered successfully.
     */
    public static final int DELIVER_STATUS_OK = 1;

    /**
     * Message was not delivered.
     */
    public static final int DELIVER_STATUS_ERROR_GENERIC = 2;

    /**
     * Message was not delivered due to lack of memory.
     */
    public static final int DELIVER_STATUS_ERROR_NO_MEMORY = 3;

    /**
     * Message was not delivered as the request is not supported.
     */
    public static final int DELIVER_STATUS_ERROR_REQUEST_NOT_SUPPORTED = 4;

    /** @hide */
    @IntDef({
            STATUS_REPORT_STATUS_OK,
            STATUS_REPORT_STATUS_ERROR
        })
    @Retention(RetentionPolicy.SOURCE)
    public @interface StatusReportResult {}

    /**
     * Status Report was set successfully.
     */
    public static final int STATUS_REPORT_STATUS_OK = 1;

    /**
     * Error while setting status report.
     */
    public static final int STATUS_REPORT_STATUS_ERROR = 2;

    /**
     * No network error was generated while processing the SMS message.
     */
    // Should match SmsResponse.NO_ERROR_CODE
    public static final int RESULT_NO_NETWORK_ERROR = -1;

    // Lock for feature synchronization
    private final Object mLock = new Object();
    private IImsSmsListener mListener;

    /**
     * Registers a listener responsible for handling tasks like delivering messages.
     *
     * @param listener listener to register.
     *
     * @hide
     */
    public final void registerSmsListener(IImsSmsListener listener) {
        synchronized (mLock) {
            mListener = listener;
        }
    }

    /**
     * This method will be triggered by the platform when the user attempts to send an SMS. This
     * method should be implemented by the IMS providers to provide implementation of sending an SMS
     * over IMS.
     *
     * @param token unique token generated by the platform that should be used when triggering
     *             callbacks for this specific message.
     * @param messageRef the message reference, which may be 1 byte if it is in
     *     {@link SmsMessage#FORMAT_3GPP} format (see TS.123.040) or 2 bytes if it is in
     *     {@link SmsMessage#FORMAT_3GPP2} format (see 3GPP2 C.S0015-B).
     * @param format the format of the message.
     * @param smsc the Short Message Service Center address.
     * @param isRetry whether it is a retry of an already attempted message or not.
     * @param pdu PDU representing the contents of the message.
     */
    public void sendSms(int token, @IntRange(from = 0, to = 65535) int messageRef,
            @SmsMessage.Format String format, String smsc, boolean isRetry,
            byte[] pdu) {
        // Base implementation returns error. Should be overridden.
        try {
            onSendSmsResult(token, messageRef, SEND_STATUS_ERROR,
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "Can not send sms: " + e.getMessage());
        }
    }

    /**
     * This method will be triggered by the platform after
     * {@link #onSmsReceived(int, String, byte[])} has been called to deliver the result to the IMS
     * provider.
     *
     * @param token token provided in {@link #onSmsReceived(int, String, byte[])}
     * @param messageRef the message reference, which may be 1 byte if it is in
     *     {@link SmsMessage#FORMAT_3GPP} format (see TS.123.040) or 2 bytes if it is in
     *     {@link SmsMessage#FORMAT_3GPP2} format (see 3GPP2 C.S0015-B).
     * @param result result of delivering the message.
     */
    public void acknowledgeSms(int token, @IntRange(from = 0, to = 65535)  int messageRef,
            @DeliverStatusResult int result) {
        Log.e(LOG_TAG, "acknowledgeSms() not implemented.");
    }

    /**
     * This method will be triggered by the platform after
     * {@link #onSmsStatusReportReceived(int, int, String, byte[])} or
     * {@link #onSmsStatusReportReceived(int, String, byte[])} has been called to provide the
     * result to the IMS provider.
     *
     * @param token token provided in {@link #onSmsStatusReportReceived(int, int, String, byte[])}
     *              or {@link #onSmsStatusReportReceived(int, String, byte[])}
     * @param messageRef the message reference, which may be 1 byte if it is in
     *     {@link SmsMessage#FORMAT_3GPP} format (see TS.123.040) or 2 bytes if it is in
     *     {@link SmsMessage#FORMAT_3GPP2} format (see 3GPP2 C.S0015-B).
     * @param result result of delivering the message.
     */
    public void acknowledgeSmsReport(int token, @IntRange(from = 0, to = 65535) int messageRef,
            @StatusReportResult int result) {
        Log.e(LOG_TAG, "acknowledgeSmsReport() not implemented.");
    }

    /**
     * This method should be triggered by the IMS providers when there is an incoming message. The
     * platform will deliver the message to the messages database and notify the IMS provider of the
     * result by calling {@link #acknowledgeSms(int, int, int)}.
     *
     * This method must not be called before {@link #onReady()} is called or the call will fail. If
     * the platform is not available, {@link #acknowledgeSms(int, int, int)} will be called with the
     * {@link #DELIVER_STATUS_ERROR_GENERIC} result code.
     * @param token unique token generated by IMS providers that the platform will use to trigger
     *              callbacks for this message.
     * @param format the format of the message.
     * @param pdu PDU representing the contents of the message.
     * @throws RuntimeException if called before {@link #onReady()} is triggered.
     */
    public final void onSmsReceived(int token, @SmsMessage.Format String format, byte[] pdu)
            throws RuntimeException {
        synchronized (mLock) {
            if (mListener == null) {
                throw new RuntimeException("Feature not ready.");
            }
            try {
                mListener.onSmsReceived(token, format, pdu);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Can not deliver sms: " + e.getMessage());
                SmsMessage message = SmsMessage.createFromPdu(pdu, format);
                if (message != null && message.mWrappedSmsMessage != null) {
                    acknowledgeSms(token, message.mWrappedSmsMessage.mMessageRef,
                            DELIVER_STATUS_ERROR_GENERIC);
                } else {
                    Log.w(LOG_TAG, "onSmsReceived: Invalid pdu entered.");
                    acknowledgeSms(token, 0, DELIVER_STATUS_ERROR_GENERIC);
                }
            }
        }
    }

    /**
     * This method should be triggered by the IMS providers when an outgoing SMS message has been
     * sent successfully.
     *
     * @param token token provided in {@link #sendSms(int, int, String, String, boolean, byte[])}
     * @param messageRef the message reference, which may be 1 byte if it is in
     *     {@link SmsMessage#FORMAT_3GPP} format (see TS.123.040) or 2 bytes if it is in
     *     {@link SmsMessage#FORMAT_3GPP2} format (see 3GPP2 C.S0015-B).
     *
     * @throws RuntimeException if called before {@link #onReady()} is triggered or if the
     * connection to the framework is not available. If this happens attempting to send the SMS
     * should be aborted.
     */
    public final void onSendSmsResultSuccess(int token,
            @IntRange(from = 0, to = 65535) int messageRef) throws RuntimeException {
        synchronized (mLock) {
            if (mListener == null) {
                throw new RuntimeException("Feature not ready.");
            }
            try {
                mListener.onSendSmsResult(token, messageRef, SEND_STATUS_OK,
                        SmsManager.RESULT_ERROR_NONE, RESULT_NO_NETWORK_ERROR);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * This method should be triggered by the IMS providers to pass the result of the sent message
     * to the platform.
     *
     * @param token token provided in {@link #sendSms(int, int, String, String, boolean, byte[])}
     * @param messageRef the message reference, which may be 1 byte if it is in
     *     {@link SmsMessage#FORMAT_3GPP} format (see TS.123.040) or 2 bytes if it is in
     *     {@link SmsMessage#FORMAT_3GPP2} format (see 3GPP2 C.S0015-B).
     * @param status result of sending the SMS.
     * @param reason reason in case status is failure.
     *
     * @throws RuntimeException if called before {@link #onReady()} is triggered or if the
     * connection to the framework is not available. If this happens attempting to send the SMS
     * should be aborted.
     * @deprecated Use {@link #onSendSmsResultSuccess(int, int)} or
     * {@link #onSendSmsResultError(int, int, int, int, int)} to notify the framework of the SMS
     * send result.
     */
    @Deprecated
    public final void onSendSmsResult(int token, @IntRange(from = 0, to = 65535) int messageRef,
            @SendStatusResult int status, @SmsManager.Result int reason) throws RuntimeException {
        synchronized (mLock) {
            if (mListener == null) {
                throw new RuntimeException("Feature not ready.");
            }
            try {
                mListener.onSendSmsResult(token, messageRef, status, reason,
                        RESULT_NO_NETWORK_ERROR);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * This method should be triggered by the IMS providers when an outgoing message fails to be
     * sent due to an error generated while processing the message or after being sent to the
     * network.
     *
     * @param token token provided in {@link #sendSms(int, int, String, String, boolean, byte[])}
     * @param messageRef the message reference, which may be 1 byte if it is in
     *     {@link SmsMessage#FORMAT_3GPP} format (see TS.123.040) or 2 bytes if it is in
     *     {@link SmsMessage#FORMAT_3GPP2} format (see 3GPP2 C.S0015-B).
     * @param status result of sending the SMS.
     * @param networkErrorCode the error code reported by the carrier network if sending this SMS
     *  has resulted in an error or {@link #RESULT_NO_NETWORK_ERROR} if no network error was
     *  generated. See 3GPP TS 24.011 Section 7.3.4 for valid error codes and more information.
     *
     * @throws RuntimeException if called before {@link #onReady()} is triggered or if the
     * connection to the framework is not available. If this happens attempting to send the SMS
     * should be aborted.
     */
    public final void onSendSmsResultError(int token,
            @IntRange(from = 0, to = 65535) int messageRef, @SendStatusResult int status,
            @SmsManager.Result int reason, int networkErrorCode) throws RuntimeException {
        synchronized (mLock) {
            if (mListener == null) {
                throw new RuntimeException("Feature not ready.");
            }
            try {
                mListener.onSendSmsResult(token, messageRef, status, reason, networkErrorCode);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }
    }

    /**
     * This method should be triggered by the IMS providers when the status report of the sent
     * message is received. The platform will handle the report and notify the IMS provider of the
     * result by calling {@link #acknowledgeSmsReport(int, int, int)}.
     *
     * This method must not be called before {@link #onReady()} is called or the call will fail. If
     * the platform is not available, {@link #acknowledgeSmsReport(int, int, int)} will be called
     * with the {@link #STATUS_REPORT_STATUS_ERROR} result code.
     * @param token token provided in {@link #sendSms(int, int, String, String, boolean, byte[])}
     * @param messageRef the message reference, which may be 1 byte if it is in
     *     {@link SmsMessage#FORMAT_3GPP} format or 2 bytes if it is in
     *     {@link SmsMessage#FORMAT_3GPP2} format (see 3GPP2 C.S0015-B).
     * @param format the format of the message.
     * @param pdu PDU representing the content of the status report.
     * @throws RuntimeException if called before {@link #onReady()} is triggered
     *
     * @deprecated Use {@link #onSmsStatusReportReceived(int, String, byte[])} instead without the
     * message reference.
     */
    @Deprecated
    public final void onSmsStatusReportReceived(int token,
            @IntRange(from = 0, to = 65535) int messageRef, @SmsMessage.Format String format,
            byte[] pdu) throws RuntimeException {
        synchronized (mLock) {
            if (mListener == null) {
                throw new RuntimeException("Feature not ready.");
            }
            try {
                mListener.onSmsStatusReportReceived(token, format, pdu);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Can not process sms status report: " + e.getMessage());
                acknowledgeSmsReport(token, messageRef, STATUS_REPORT_STATUS_ERROR);
            }
        }
    }

    /**
     * This method should be triggered by the IMS providers when the status report of the sent
     * message is received. The platform will handle the report and notify the IMS provider of the
     * result by calling {@link #acknowledgeSmsReport(int, int, int)}.
     *
     * This method must not be called before {@link #onReady()} is called or the call will fail. If
     * the platform is not available, {@link #acknowledgeSmsReport(int, int, int)} will be called
     * with the {@link #STATUS_REPORT_STATUS_ERROR} result code.
     * @param token unique token generated by IMS providers that the platform will use to trigger
     *              callbacks for this message.
     * @param format the format of the message.
     * @param pdu PDU representing the content of the status report.
     * @throws RuntimeException if called before {@link #onReady()} is triggered
     */
    public final void onSmsStatusReportReceived(int token, @SmsMessage.Format String format,
            byte[] pdu) throws RuntimeException {
        synchronized (mLock) {
            if (mListener == null) {
                throw new RuntimeException("Feature not ready.");
            }
            try {
                mListener.onSmsStatusReportReceived(token, format, pdu);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Can not process sms status report: " + e.getMessage());
                SmsMessage message = SmsMessage.createFromPdu(pdu, format);
                if (message != null && message.mWrappedSmsMessage != null) {
                    acknowledgeSmsReport(
                            token,
                            message.mWrappedSmsMessage.mMessageRef,
                            STATUS_REPORT_STATUS_ERROR);
                } else {
                    Log.w(LOG_TAG, "onSmsStatusReportReceived: Invalid pdu entered.");
                    acknowledgeSmsReport(token, 0, STATUS_REPORT_STATUS_ERROR);
                }
            }
        }
    }

    /**
     * Returns the SMS format that the ImsService expects.
     *
     * @return  The expected format of the SMS messages.
     */
    public @SmsMessage.Format String getSmsFormat() {
      return SmsMessage.FORMAT_3GPP;
    }

    /**
     * Called when ImsSmsImpl has been initialized and communication with the framework is set up.
     * Any attempt by this class to access the framework before this method is called will return
     * with a {@link RuntimeException}.
     */
    public void onReady() {
        // Base Implementation - Should be overridden
    }
}
