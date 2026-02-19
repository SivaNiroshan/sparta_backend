package com.sparta.UserService.service;

import com.sparta.UserService.service.otputility.EmailOTP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OTPService {
    
    @Autowired
    private EmailOTP emailOTP;
    
    // SmsOTP is available but not currently used
    // @Autowired
    // private SmsOTP smsOTP;
    
    /**
     * Sends OTP via email
     * Currently using EmailOTP implementation
     * 
     * @param email Recipient email address
     * @return Generated 6-digit OTP
     */
    public String sendOTPByEmail(String email) {
        return emailOTP.sendOTP(email);
    }
    
    /**
     * Sends OTP via email with custom subject and message
     * 
     * @param email Recipient email address
     * @param subject Email subject
     * @param customMessage Custom message to include with OTP
     * @return Generated 6-digit OTP
     */
    public String sendOTPByEmail(String email, String subject, String customMessage) {
        return emailOTP.sendOTP(email, subject, customMessage);
    }
    
    /**
     * Verifies if the provided OTP matches the expected OTP
     * 
     * @param providedOTP OTP provided by user
     * @param expectedOTP Expected OTP to match against
     * @return true if OTPs match, false otherwise
     */
    public boolean verifyOTP(String providedOTP, String expectedOTP) {
        return providedOTP != null && expectedOTP != null && providedOTP.equals(expectedOTP);
    }
}

