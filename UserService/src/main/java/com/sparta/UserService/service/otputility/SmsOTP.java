package com.sparta.UserService.service.otputility;

import org.springframework.stereotype.Component;

@Component
public class SmsOTP extends OTP {
    
    /**
     * Sends OTP via SMS
     * Currently a placeholder implementation
     * Can be extended with actual SMS service integration (Twilio, AWS SNS, etc.)
     * 
     * @param phoneNumber Recipient phone number
     * @return Generated 6-digit OTP
     */
    public String sendOTP(String phoneNumber) {
        String otp = generateOTP(); // Accessing protected method from parent class
        
        // TODO: Implement actual SMS sending logic
        // Example: Integrate with Twilio, AWS SNS, or other SMS service providers
        System.out.println("SMS OTP sent to " + phoneNumber + ": " + otp);
        
        return otp;
    }
    
    /**
     * Sends OTP via SMS with custom message
     * 
     * @param phoneNumber Recipient phone number
     * @param customMessage Custom message to include with OTP
     * @return Generated 6-digit OTP
     */
    public String sendOTP(String phoneNumber, String customMessage) {
        String otp = generateOTP(); // Accessing protected method from parent class
        
        // TODO: Implement actual SMS sending logic
        System.out.println("SMS OTP sent to " + phoneNumber + ": " + otp);
        System.out.println("Message: " + customMessage);
        
        return otp;
    }
}

