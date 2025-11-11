package com.dic1.projettrans.customerservice.services;

public interface OtpService {
    /**
     * Generate a 6-digit OTP for the given email and store it with a short expiry (e.g., 5 minutes).
     * Any previously generated OTP for the email should be replaced.
     * @param email user's email
     * @return generated OTP code
     */
    String generate(String email);

    /**
     * Verify the provided OTP for the given email. Returns true if the OTP matches and is not expired.
     * On successful verification, the OTP is invalidated.
     * @param email user's email
     * @param code provided otp
     * @return true if valid, false otherwise
     */
    boolean verify(String email, String code);
}
