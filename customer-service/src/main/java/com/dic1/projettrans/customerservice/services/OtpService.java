package com.dic1.projettrans.customerservice.services;

public interface OtpService {
    /**
     * Generate a 6-digit OTP for the given telephone and store it with a short expiry (e.g., 5 minutes).
     * Any previously generated OTP for the telephone should be replaced.
     * @param telephone user's phone number
     * @return generated OTP code
     */
    String generate(String telephone);

    /**
     * Verify the provided OTP for the given telephone. Returns true if the OTP matches and is not expired.
     * On successful verification, the OTP is invalidated.
     * @param telephone user's phone number
     * @param code provided otp
     * @return true if valid, false otherwise
     */
    boolean verify(String telephone, String code);
}
